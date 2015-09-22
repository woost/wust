package modules.db.access.custom

import controllers.api.nodes.{HyperConnectParameter, ConnectParameter, RequestContext}
import model.WustSchema.{Created => SchemaCreated, _}
import modules.db.Database._
import modules.db._
import modules.db.access.{EndRelationAccessDefault, EndRelationAccess}
import play.api.libs.json._
import renesca.parameter.implicits._
import renesca.schema._
import renesca._
import play.api.mvc.Results._
import moderation.Moderation

trait VotesReferenceAccess[T <: Reference] extends EndRelationAccessDefault[User, Votes, Votable] {
  val sign: Long
  val nodeFactory = User

  def selectNode(discourse: Discourse, startUuid: String, endUuid: String): T
  def nodeDefinition(startUuid: String, endUuid: String): HyperNodeDefinitionBase[T]
  def postDefinition(nodeDefinition: HyperNodeDefinitionBase[T]): NodeDefinition[Post]
  def selectPost(reference: T): Post
  def updateKarma(tx: QueryHandler, reference: T, karma: Long): Unit

  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, Votable with AbstractRelation[S,E], E]) = context.withUser { user =>
    db.transaction { tx =>

      val weight = sign * Moderation.postVoteKarma
      val referenceDef = nodeDefinition(param.startUuid, param.endUuid)
      val userDef = ConcreteNodeDefinition(user)
      val votesDef = RelationDefinition(userDef, Votes, referenceDef)
      val createdDef = RelationDefinition(userDef, SchemaCreated, postDefinition(referenceDef))

      val query = s"""
      match ${referenceDef.toQuery}
      set ${referenceDef.name}._locked = true
      with ${referenceDef.startName},${referenceDef.startRelationName},${referenceDef.endName},${referenceDef.endRelationName},${referenceDef.name}
      match ${userDef.toQuery}
      optional match ${votesDef.toQuery(false, false)}
      optional match ${createdDef.toQuery(false, false)}
      return *
      """

      val params = votesDef.parameterMap ++ createdDef.parameterMap

      val discourse = Discourse(tx.queryGraph(query, params))

      // we only match createds to the post from the current user, so if there
      // are created relations, he is the author
      if (discourse.createds.isEmpty) {
        val reference = selectNode(discourse, param.startUuid, param.endUuid)
        val votes = discourse.votes.headOption
        votes.foreach(reference.voteCount -= _.weight)

        val success = if (weight == 0) {
          // if there are any existing votes, disconnect them
          votes.foreach { vote =>
            updateKarma(tx, reference, -vote.weight)
            discourse.remove(vote)
          }
          true
        } else {
          // we want to vote on the change request with our weight. we merge the
          // votes relation as we want to override any previous vote. merging is
          // better than just updating the weight on an existing relation, as it
          // guarantees uniqueness
          reference.voteCount += weight
          val newVotes = Votes.merge(user, reference, weight = weight, onMatch = Set("weight"))
          discourse.add(newVotes)
          updateKarma(tx, reference, weight)
        }

        val post = selectPost(reference)
        val quality = reference.quality(post.viewCount)

        reference._locked = false
        val failure = tx.persistChanges(discourse)
        if (failure.isEmpty) {
          Ok(JsObject(Seq(
            ("quality", JsNumber(quality)),
            ("vote", JsObject(Seq(
              ("weight", JsNumber(weight))
            )))
          )))
        } else {
          BadRequest("No vote :/")
        }
      } else {
        Forbidden("Voting on own post not allowed")
      }
    }
  }
}

case class VotesTagsAccess(sign: Long) extends VotesReferenceAccess[Tags] {

  override def selectNode(discourse: Discourse, startUuid: String, endUuid: String) = discourse.tags.find(t => t.startNodeOpt.map(_.uuid == startUuid).getOrElse(false) && t.endNodeOpt.map(_.uuid == endUuid).getOrElse(false)).get

  override def nodeDefinition(startUuid: String, endUuid: String): HyperNodeDefinitionBase[Tags] = {
    HyperNodeDefinition(FactoryUuidNodeDefinition(Scope, startUuid), Tags, FactoryUuidNodeDefinition(Post, endUuid))
  }

  override def postDefinition(nodeDefinition: HyperNodeDefinitionBase[Tags]) = nodeDefinition.endDefinition.asInstanceOf[NodeDefinition[Post]]
  override def selectPost(reference: Tags) = reference.endNodeOpt.get

  override def updateKarma(tx: QueryHandler, reference: Tags, karma: Long) {
    val tagDef = ConcreteNodeDefinition(reference.startNodeOpt.get)
    val postDef = ConcreteNodeDefinition(reference.endNodeOpt.get)
    val userDef = ConcreteFactoryNodeDefinition(User)
    val createdDef = RelationDefinition(userDef, SchemaCreated, postDef)

    val query = s"""
    match ${createdDef.toQuery}, ${tagDef.toQuery}
    merge (${userDef.name})-[r:`${HasKarma.relationType}`]->(${tagDef.name})
    on create set r.karma = {karma}
    on match set r.karma = r.karma + {karma}
    """

    val params = tagDef.parameterMap ++ createdDef.parameterMap ++ Map("karma" -> karma)

    tx.query(query, params)
  }
}

case class VotesConnectsAccess(sign: Long) extends VotesReferenceAccess[Connects] {

  override def selectNode(discourse: Discourse, startUuid: String, endUuid: String) = discourse.connects.find(c => c.startNodeOpt.map(_.uuid == startUuid).getOrElse(false) && c.endNodeOpt.map(_.uuid == endUuid).getOrElse(false)).get

  override def nodeDefinition(startUuid: String, endUuid: String): HyperNodeDefinitionBase[Connects] = {
    HyperNodeDefinition(FactoryUuidNodeDefinition(Post, startUuid), Connects, FactoryUuidNodeDefinition(Connectable, endUuid))
  }

  override def postDefinition(nodeDefinition: HyperNodeDefinitionBase[Connects]) = nodeDefinition.endDefinition.asInstanceOf[NodeDefinition[Post]]
  override def selectPost(reference: Connects) = reference.startNodeOpt.get

  override def updateKarma(tx: QueryHandler, reference: Connects, karma: Long) {
    val connDef = ConcreteNodeDefinition(reference.endNodeOpt.get)
    val postDef = ConcreteNodeDefinition(reference.startNodeOpt.get)
    val userDef = ConcreteFactoryNodeDefinition(User)
    val createdDef = RelationDefinition(userDef, SchemaCreated, postDef)

    val query = s"""
    match ${createdDef.toQuery},
    ${connDef.toQuery}-[:`${Connects.startRelationType}`|`${Connects.endRelationType}` *0..20]->(connectable: `${Connectable.label}`),
    (tag: `${Scope.label}`)-[:`${Tags.startRelationType}`]->(:`${Tags.label}`)-[:`${Tags.endRelationType}`]->(connectable: `${Post.label}`)
    with distinct tag, ${userDef.name}
    merge (${userDef.name})-[r:`${HasKarma.relationType}`]->(tag)
    on create set r.karma = {karma}
    on match set r.karma = r.karma + {karma}
    """

    val params = connDef.parameterMap ++ createdDef.parameterMap ++ Map("karma" -> karma)

    tx.query(query, params)
  }
}
