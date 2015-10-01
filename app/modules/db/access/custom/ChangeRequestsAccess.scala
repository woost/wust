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

  override def read(context: RequestContext) = context.withUser { user =>
    val limit = context.limit
    val skip = context.skip

    implicit val ctx = new QueryContext
    val crDef = LabelNodeDefinition[TagChangeRequest](ChangeRequest.labels)
    val crTagsDef = RelationDefinition(crDef, ProposesTag, FactoryNodeDefinition(Scope))
    val crClassifiesDef = RelationDefinition(crDef, ProposesClassify, FactoryNodeDefinition(Classification))
    val postDef = LabelNodeDefinition[Post](Set.empty) //matching real posts and hidden posts without any label just by their relations
    val tagsDef = HyperNodeDefinition(FactoryNodeDefinition(Scope), Tags, postDef)
    val connectsDef = FactoryNodeDefinition(Connects)
    val connDef = RelationDefinition(postDef, ConnectsStart, connectsDef)
    val tagClassifiesDef = RelationDefinition(FactoryNodeDefinition(Classification), Classifies, tagsDef)
    val classifiesDef = RelationDefinition(FactoryNodeDefinition(Classification), Classifies, connectsDef)
    val userDef = ConcreteNodeDefinition(user)

    //TODO: we need to match the deleted relation separately, as hidden posts
    //are only allowed for instant deleted requests - actually we should know
    //which request is responsible for the current deletion.
    //we currently would show edit and tag requests for already deleted posts.
    val query = s"""
    match ${ crDef.toQuery }-[relation:`${ Updated.endRelationType }`|`${ Deleted.endRelationType }`|`${ AddTags.endRelationType }`|`${ RemoveTags.endRelationType }`]->${ postDef.toQuery }, ${ userDef.toQuery }
    where (${ crDef.name }.status = ${ INSTANT } OR ${ crDef.name }.status = ${ PENDING })
    and not((${userDef.name})-[:`${Votes.relationType}`]->(${crDef.name}))
    and not((${userDef.name})-[:`${Skipped.relationType}`]->(${crDef.name}))
    and not((${userDef.name})-[:`${UpdatedStart.relationType}`|`${DeletedStart.relationType}`|`${AddTagsStart.relationType}`|`${RemoveTagsStart.relationType}`]->(${crDef.name}))
    with ${ postDef.name }, relation, ${ crDef.name } order by ${ crDef.name }.timestamp skip ${ skip } limit ${ limit }
    optional match ${ crTagsDef.toQuery(false, true) }
    optional match ${ crClassifiesDef.toQuery(false, true) }
    optional match ${ tagsDef.toQuery(true, false) }
    optional match ${ tagClassifiesDef.toQuery(true, false) }
    optional match ${ connDef.toQuery(false, true) }, ${ classifiesDef.toQuery(true, false) }
    return *
    """

    val params = userDef.parameterMap ++ crDef.parameterMap ++ crTagsDef.parameterMap ++ crClassifiesDef.parameterMap ++ tagsDef.parameterMap ++ connDef.parameterMap ++ classifiesDef.parameterMap

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
      val proposesTagDef = RelationDefinition(updatedDef, ProposesTag, FactoryNodeDefinition(Scope))
      val proposesClassifyDef = RelationDefinition(updatedDef, ProposesClassify, FactoryNodeDefinition(Classification))

      val query = s"""
      match ${ updatedDef.toQuery }-[:`${ Updated.endRelationType }`|`${ AddTags.endRelationType }`|`${ RemoveTags.endRelationType }`|`${ Deleted.endRelationType }`]->${ postDef.toQuery }
      where ${ updatedDef.name }.status = ${ PENDING }
      optional match ${ votesDef.toQuery(true, false) }
      optional match ${ proposesTagDef.toQuery(false, true) }
      optional match ${ proposesClassifyDef.toQuery(false, true) }
      return *
      """

      val params = postDef.parameterMap ++ votesDef.parameterMap ++ proposesTagDef.parameterMap ++ proposesClassifyDef.parameterMap
      val discourse = Discourse(db.queryGraph(query, params))
      discourse.changeRequests
    }.getOrElse(Seq.empty)))
  }

}
