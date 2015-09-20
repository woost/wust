package modules.db.access.custom

import controllers.api.nodes.{ConnectParameter, RequestContext}
import formatters.json.ChangeRequestFormat._
import model.WustSchema.{Created => SchemaCreated, _}
import modules.db.Database.db
import modules.db._
import modules.db.access._
import play.api.libs.json._
import play.api.mvc.Results._

case class InstantChangeRequestAccess() extends NodeAccessDefault[ChangeRequest] {
  val factory = ChangeRequest

  override def read(context: RequestContext) = {
    val page = context.page.getOrElse(0)
    val limit = context.limit
    val skip = page * limit

    val crDef = LabelNodeDefinition[TagChangeRequest](ChangeRequest.labels)
    val crTagsDef = RelationDefinition(crDef, ProposesTag, ConcreteFactoryNodeDefinition(Scope))
    val postDef = ConcreteFactoryNodeDefinition(Post)
    val tagsDef = RelationDefinition(ConcreteFactoryNodeDefinition(Scope), Tags, postDef)
    val connectsDef = ConcreteFactoryNodeDefinition(Connects)
    val connDef = RelationDefinition(postDef, PostToConnects, connectsDef)
    val classifiesDef = RelationDefinition(ConcreteFactoryNodeDefinition(Classification), Classifies, connectsDef)

    val query = s"""
    match ${ crDef.toQuery }-[relation:`${ UpdatedToPost.relationType }`|`${ AddTagsToPost.relationType }`|`${ RemoveTagsToPost.relationType }`]->${ postDef.toQuery } where ${ crDef.name }.applied = ${ INSTANT }
    with ${ postDef.name }, relation, ${ crDef.name } order by ${ crDef.name }.timestamp skip ${ skip } limit ${ limit }
    optional match ${ crTagsDef.toQuery(false, true) }
    optional match ${ tagsDef.toQuery(true, false) }
    optional match ${ connDef.toQuery(false, true) }, ${ classifiesDef.toQuery(true, false) }
    return *
    """

    val params = crDef.parameterMap ++ tagsDef.parameterMap ++ connDef.parameterMap ++ classifiesDef.parameterMap

    val discourse = Discourse(db.queryGraph(query, params))

    Ok(Json.toJson(discourse.changeRequests))
  }
}

//TODO: one api to get them all?
case class PostUpdatedAccess() extends EndRelationAccessDefault[Updated, UpdatedToPost, Post] {
  val nodeFactory = Updated
  override def read(context: RequestContext, param: ConnectParameter[Post]) = {
    Ok(Json.toJson(context.user.map { user =>
      val userDef = ConcreteNodeDefinition(user)
      val updatedDef = ConcreteFactoryNodeDefinition(Updated)
      val postDef = FactoryUuidNodeDefinition(Post, param.baseUuid)
      val relDef = RelationDefinition(updatedDef, UpdatedToPost, postDef)
      val votesDef = RelationDefinition(userDef, Votes, updatedDef)

      val query = s"""
      match ${ relDef.toQuery }
      where ${ updatedDef.name }.applied = ${ PENDING }
      optional match ${ votesDef.toQuery(true, false) }
      return ${ votesDef.name }, ${ updatedDef.name }
      """

      val discourse = Discourse(db.queryGraph(query, relDef.parameterMap ++ votesDef.parameterMap))
      discourse.updateds
    }.getOrElse(Seq.empty)))
  }
}

case class PostTagChangeRequestAccess() extends RelationAccessDefault[Post, TagChangeRequest] {
  val nodeFactory = TagChangeRequestMatches

  override def read(context: RequestContext, param: ConnectParameter[Post]) = {
    Ok(Json.toJson(context.user.map { user =>
      val userDef = ConcreteNodeDefinition(user)
      val updatedDef = ConcreteFactoryNodeDefinition(nodeFactory)
      val postDef = FactoryUuidNodeDefinition(Post, param.baseUuid)
      val votesDef = RelationDefinition(userDef, Votes, updatedDef)
      val scopeDef = ConcreteFactoryNodeDefinition(Scope)
      val proposes = RelationDefinition(updatedDef, ProposesTag, scopeDef)

      val query = s"""
      match ${ updatedDef.toQuery }-[:`${ AddTags.endRelationType }`|`${ RemoveTags.endRelationType }`]->${ postDef.toQuery }
      where ${ updatedDef.name }.applied = ${ PENDING }
      optional match ${ votesDef.toQuery(true, false) }
      optional match ${ proposes.toQuery(false, true) }
      return ${ votesDef.name }, ${ proposes.name }, ${ updatedDef.name }
      """

      val discourse = Discourse(db.queryGraph(query, postDef.parameterMap ++ votesDef.parameterMap))
      discourse.tagChangeRequests
    }.getOrElse(Seq.empty)))
  }

}
