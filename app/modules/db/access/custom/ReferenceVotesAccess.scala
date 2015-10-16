package modules.db.access.custom

import controllers.api.nodes.{HyperConnectParameter, ConnectParameter, RequestContext}
import model.WustSchema.{Created => SchemaCreated, _}
import modules.db.Database._
import modules.db._
import modules.karma._
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
  def nodeDefinition(startUuid: String, endUuid: String)(implicit ctx: QueryContext): HyperNodeDefBase[T]
  def postDefinition(nodeDefinition: HyperNodeDefBase[T]): NodeDef[Post]
  def selectPost(reference: T): Post
  def updateKarma(reference: T, karmaDefinition: KarmaDefinition): Unit

  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, Votable with AbstractRelation[S,E], E]) = context.withUser { user =>
    db.transaction { tx =>

      val weight = sign * Moderation.postVoteKarma
      implicit val ctx = new QueryContext
      val referenceDef = nodeDefinition(param.startUuid, param.endUuid)
      val userDef = ConcreteNodeDef(user)
      val votesDef = RelationDef(userDef, Votes, referenceDef)
      val createdDef = RelationDef(userDef, SchemaCreated, postDefinition(referenceDef))

      val query = s"""
      match ${referenceDef.toPattern}
      set ${referenceDef.name}._locked = true
      with ${referenceDef.startName},${referenceDef.startRelationName},${referenceDef.endName},${referenceDef.endRelationName},${referenceDef.name}
      match ${userDef.toPattern}
      optional match ${votesDef.toPattern(false, false)}
      optional match ${createdDef.toPattern(false, false)}
      return *
      """

      val discourse = Discourse(tx.queryGraph(query, ctx.params))

      // we only match createds to the post from the current user, so if there
      // are created relations, he is the author
      if (discourse.createds.isEmpty) {
        val reference = selectNode(discourse, param.startUuid, param.endUuid)
        val votes = discourse.votes.headOption
        val karmaDefinition = if (weight == 0) {
          // if there are any existing votes, disconnect them
          votes.map { vote =>
            reference.voteCount -= vote.weight
            discourse.remove(vote)
            KarmaDefinition(-vote.weight, "Unvoted post")
          }
        } else if (votes.isEmpty) {
          // we want to vote on the change request with our weight if there is
          // no vote from user. we merge the vote to guarantee uniqueness.
          reference.voteCount += weight
          val newVotes = Votes.merge(user, reference, weight = weight, onMatch = Set("weight"))
          discourse.add(newVotes)
          Some(KarmaDefinition(weight, "Upvoted post"))
        } else None

        val post = selectPost(reference)
        val quality = reference.quality(post.viewCount)

        reference._locked = false
        val failure = tx.persistChanges(discourse)
        if (failure.isEmpty) {
          karmaDefinition.foreach(updateKarma(reference, _))
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

  override def nodeDefinition(startUuid: String, endUuid: String)(implicit ctx: QueryContext): HyperNodeDefBase[Tags] = {
    HyperNodeDef(FactoryUuidNodeDef(Scope, startUuid), Tags, FactoryUuidNodeDef(Post, endUuid))
  }

  override def postDefinition(nodeDefinition: HyperNodeDefBase[Tags]) = nodeDefinition.endDefinition.asInstanceOf[NodeDef[Post]]
  override def selectPost(reference: Tags) = reference.endNodeOpt.get

  override def updateKarma(reference: Tags, karmaDefinition: KarmaDefinition) {
    implicit val ctx = new QueryContext
    val tagDef = ConcreteNodeDef(reference.startNodeOpt.get)
    val postDef = ConcreteNodeDef(reference.endNodeOpt.get)
    val createdDef = RelationDef(FactoryNodeDef(User), SchemaCreated, postDef)

    KarmaUpdate.persistWithTags(karmaDefinition, KarmaQueryCreated(createdDef), tagDef)
  }
}

case class VotesConnectsAccess(sign: Long) extends VotesReferenceAccess[Connects] {

  override def selectNode(discourse: Discourse, startUuid: String, endUuid: String) = discourse.connects.find(c => c.startNodeOpt.map(_.uuid == startUuid).getOrElse(false) && c.endNodeOpt.map(_.uuid == endUuid).getOrElse(false)).get

  override def nodeDefinition(startUuid: String, endUuid: String)(implicit ctx: QueryContext): HyperNodeDefBase[Connects] = {
    HyperNodeDef(FactoryUuidNodeDef(Post, startUuid), Connects, FactoryUuidNodeDef(Connectable, endUuid))
  }

  override def postDefinition(nodeDefinition: HyperNodeDefBase[Connects]) = nodeDefinition.startDefinition.asInstanceOf[NodeDef[Post]]
  override def selectPost(reference: Connects) = reference.startNodeOpt.get

  override def updateKarma(reference: Connects, karmaDefinition: KarmaDefinition) {
    implicit val ctx = new QueryContext
    val postDef = ConcreteNodeDef(reference.startNodeOpt.get)
    val parentDef = ConcreteNodeDef(reference)
    val createdDef = RelationDef(FactoryNodeDef(User), SchemaCreated, postDef)

    KarmaUpdate.persistWithConnectedTagsThroughParent(karmaDefinition, KarmaQueryCreated(createdDef), parentDef)
  }
}
