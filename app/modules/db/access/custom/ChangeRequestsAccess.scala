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
    val tagsDef = HyperNodeDefinition(ConcreteFactoryNodeDefinition(Scope), Tags, postDef)
    val connectsDef = ConcreteFactoryNodeDefinition(Connects)
    val connDef = RelationDefinition(postDef, PostToConnects, connectsDef)
    val tagClassifiesDef = RelationDefinition(ConcreteFactoryNodeDefinition(Classification), Classifies, tagsDef)
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

    //TODO: we need to match the deleted relation separately, as hidden posts
    //are only allowed for instant deleted requests - actually we should know
    //which request is responsible for the current deletion.
    //we currently would show edit and tag requests for already deleted posts.
    val query = s"""
    match ${ crDef.toQuery }-[relation:`${ Updated.endRelationType }`|`${ Deleted.endRelationType }`|`${ AddTags.endRelationType }`|`${ RemoveTags.endRelationType }`]->${ postDef.toQuery } $userMatcher
    where (${ crDef.name }.status = ${ INSTANT } OR ${ crDef.name }.status = ${ PENDING })
    $userCondition
    with ${ postDef.name }, relation, ${ crDef.name } order by ${ crDef.name }.timestamp skip ${ skip } limit ${ limit }
    optional match ${ crTagsDef.toQuery(false, true) }
    optional match ${ tagsDef.toQuery(true, false) }
    optional match ${ tagClassifiesDef.toQuery(true, false) }
    optional match ${ connDef.toQuery(false, true) }, ${ classifiesDef.toQuery(true, false) }
    return *
    """

    val params = userParams ++ crDef.parameterMap ++ tagsDef.parameterMap ++ connDef.parameterMap ++ classifiesDef.parameterMap

    val discourse = Discourse(db.queryGraph(query, params))

    //HACK: we set the labels of all hidden posts to Post (e.g. an already
    //deleted post of an instant request) in order to have working
    //neighbour/relation accessors, as they care about correct labels. this is
    //important for the formatter to work.
    //BE CAREFUL: DO NOT PERSIST AFTER THIS POINT
    discourse.hiddens.foreach { hidden =>
      hidden.rawItem.labels.clear()
      hidden.rawItem.labels ++= Post.labels
    }

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
      match ${ updatedDef.toQuery }-[:`${ Updated.endRelationType }`|`${ AddTags.endRelationType }`|`${ RemoveTags.endRelationType }`|`${ Deleted.endRelationType }`]->${ postDef.toQuery }
      where ${ updatedDef.name }.status = ${ PENDING }
      optional match ${ votesDef.toQuery(true, false) }
      optional match ${ proposes.toQuery(false, true) }
      return *
      """

      val discourse = Discourse(db.queryGraph(query, postDef.parameterMap ++ votesDef.parameterMap))
      discourse.changeRequests
    }.getOrElse(Seq.empty)))
  }

}
