package modules.db.access.custom

import controllers.api.nodes.{ConnectParameter, RequestContext}
import model.WustSchema.{Created => SchemaCreated, _}
import modules.db.Database.db
import modules.db._
import modules.db.access._
import play.api.libs.json._
import play.api.mvc.Results._

case class InstantChangeRequestAccess() extends NodeAccessDefault[ChangeRequest] {
  import formatters.json.ChangeRequestFormat._

  val factory = ChangeRequest

  override def read(context: RequestContext) = {
    val limit = context.limit
    val skip = context.skip

    implicit val ctx = new QueryContext
    val crDef = LabelNodeDefinition[TagChangeRequest](ChangeRequest.labels)
    val crTagsDef = RelationDefinition(crDef, ProposesTag, ConcreteFactoryNodeDefinition(Scope))
    val postDef = LabelNodeDefinition[Post](Set.empty) //matching real posts and hidden posts without any label just by their relations
    val tagsDef = RelationDefinition(ConcreteFactoryNodeDefinition(Scope), Tags, postDef)
    val connectsDef = ConcreteFactoryNodeDefinition(Connects)
    val connDef = RelationDefinition(postDef, PostToConnects, connectsDef)
    val classifiesDef = RelationDefinition(ConcreteFactoryNodeDefinition(Classification), Classifies, connectsDef)

    val (userMatcher, userCondition, userParams) = context.user.map { user =>
      val userDef = ConcreteNodeDefinition(user)
      val matcher = s", ${userDef.toQuery}"
      val condition = s"""
      and not((${userDef.name})-[:`${Votes.relationType}`]->(${crDef.name}))
      and not((${userDef.name})-[:`${Skipped.relationType}`]->(${crDef.name}))
      and not((${userDef.name})-[:`${UserToUpdated.relationType}`|`${UserToDeleted.relationType}`|`${UserToAddTags.relationType}`|`${UserToRemoveTags.relationType}`]->(${crDef.name}))
      """
      (matcher, condition, userDef.parameterMap)
    }.getOrElse(("", "", Map.empty))

    val query = s"""
    match ${ crDef.toQuery }-[relation:`${ UpdatedToPost.relationType }`|`${ DeletedToHidden.relationType }`|`${ AddTagsToPost.relationType }`|`${ RemoveTagsToPost.relationType }`]->${ postDef.toQuery } $userMatcher
    where (${ crDef.name }.applied = ${ INSTANT } OR ${ crDef.name }.applied = ${ PENDING })
    $userCondition
    with ${ postDef.name }, relation, ${ crDef.name } order by ${ crDef.name }.timestamp skip ${ skip } limit ${ limit }
    optional match ${ crTagsDef.toQuery(false, true) }
    optional match ${ tagsDef.toQuery(true, false) }
    optional match ${ connDef.toQuery(false, true) }, ${ classifiesDef.toQuery(true, false) }
    return *
    """

    val params = userParams ++ crDef.parameterMap ++ tagsDef.parameterMap ++ connDef.parameterMap ++ classifiesDef.parameterMap

    val discourse = Discourse(db.queryGraph(query, params))

    Ok(Json.toJson(discourse.changeRequests))
  }
}

case class ChangeRequestsSkippedAccess() extends EndRelationAccessDefault[User, Skipped, ChangeRequest] {
  val nodeFactory = User

  override def create(context: RequestContext, param: ConnectParameter[ChangeRequest]) = context.withUser { user =>
    val cr = ChangeRequest.matchesOnUuid(param.baseUuid)
    val skipped = Skipped.merge(user, cr)
    val failure = db.transaction(_.persistChanges(skipped))
    if (failure.isDefined)
      BadRequest("Cannot create skipped relation")
    else
      NoContent
  }
}

case class PostChangeRequestAccess() extends RelationAccessDefault[Post, ChangeRequest] {
  import formatters.json.EditNodeFormat.CRFormat

  val nodeFactory = ChangeRequestMatches

  override def read(context: RequestContext, param: ConnectParameter[Post]) = {
    Ok(Json.toJson(context.user.map { user =>
      implicit val ctx = new QueryContext
      val userDef = ConcreteNodeDefinition(user)
      val updatedDef = LabelNodeDefinition[TagChangeRequest](nodeFactory.labels)
      val postDef = FactoryUuidNodeDefinition(Post, param.baseUuid)
      val votesDef = RelationDefinition(userDef, Votes, updatedDef)
      val scopeDef = ConcreteFactoryNodeDefinition(Scope)
      val proposes = RelationDefinition(updatedDef, ProposesTag, scopeDef)

      val query = s"""
      match ${ updatedDef.toQuery }-[:`${ Updated.endRelationType }`|`${ AddTags.endRelationType }`|`${ RemoveTags.endRelationType }`]->${ postDef.toQuery }
      where ${ updatedDef.name }.applied = ${ PENDING }
      optional match ${ votesDef.toQuery(true, false) }
      optional match ${ proposes.toQuery(false, true) }
      return *
      """

      val discourse = Discourse(db.queryGraph(query, postDef.parameterMap ++ votesDef.parameterMap))
      discourse.changeRequests
    }.getOrElse(Seq.empty)))
  }

}
