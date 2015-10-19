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
    val limit = context.sizeWithDefault
    val skip = context.skip.getOrElse(0)

    implicit val ctx = new QueryContext
    val crDef = LabelNodeDef[TagChangeRequest](ChangeRequest.labels)
    val crTagsDef = RelationDef(crDef, ProposesTag, FactoryNodeDef(Scope))
    val crClassifiesDef = RelationDef(crDef, ProposesClassify, FactoryNodeDef(Classification))
    val postDef = LabelNodeDef[Post](Set.empty) //matching real posts and hidden posts without any label just by their relations
    val tagsDef = HyperNodeDef(FactoryNodeDef(Scope), Tags, postDef)
    val connectsDef = FactoryNodeDef(Connects)
    val connDef = RelationDef(postDef, ConnectsStart, connectsDef)
    val tagClassifiesDef = RelationDef(FactoryNodeDef(Classification), Classifies, tagsDef)
    val classifiesDef = RelationDef(FactoryNodeDef(Classification), Classifies, connectsDef)
    val userDef = ConcreteNodeDef(user)

    //TODO: we need to match the deleted relation separately, as hidden posts
    //are only allowed for instant deleted requests - actually we should know
    //which request is responsible for the current deletion.
    //we currently would show edit and tag requests for already deleted posts.
    val query = s"""
    match ${ crDef.toPattern }-[relation:`${ Updated.endRelationType }`|`${ Deleted.endRelationType }`|`${ AddTags.endRelationType }`|`${ RemoveTags.endRelationType }`]->${ postDef.toPattern }, ${ userDef.toPattern }
    where (${ crDef.name }.status = ${ INSTANT } OR ${ crDef.name }.status = ${ PENDING })
    and not((${userDef.name})-[:`${Votes.relationType}`]->(${crDef.name}))
    and not((${userDef.name})-[:`${Skipped.relationType}`]->(${crDef.name}))
    and not((${userDef.name})-[:`${UpdatedStart.relationType}`|`${DeletedStart.relationType}`|`${AddTagsStart.relationType}`|`${RemoveTagsStart.relationType}`]->(${crDef.name}))
    with ${ postDef.name }, relation, ${ crDef.name } order by ${ crDef.name }.timestamp skip ${ skip } limit ${ limit }
    optional match ${ crTagsDef.toPattern(false, true) }
    optional match ${ crClassifiesDef.toPattern(false, true) }
    optional match ${ tagsDef.toPattern(true, false) }
    optional match ${ tagClassifiesDef.toPattern(true, false) }
    optional match ${ connDef.toPattern(false, true) }, ${ classifiesDef.toPattern(true, false) }
    return *
    """

    val discourse = Discourse(db.queryGraph(query, ctx.params))

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
      val updatedDef = LabelNodeDef[TagChangeRequest](nodeFactory.labels)
      val postDef = FactoryUuidNodeDef(Post, param.baseUuid)
      val userDef = FactoryNodeDef(User)
      val votesDef = RelationDef(ConcreteNodeDef(user), Votes, updatedDef)
      val proposesTagDef = RelationDef(updatedDef, ProposesTag, FactoryNodeDef(Scope))
      val proposesClassifyDef = RelationDef(updatedDef, ProposesClassify, FactoryNodeDef(Classification))

      val query = s"""
      match ${userDef.toPattern}-[updated1:`${Updated.startRelationType}`|`${AddTags.startRelationType}`|`${Deleted.startRelationType}`|`${RemoveTags.startRelationType}`]->${updatedDef.toPattern}-[updated2:`${Updated.endRelationType}`|`${Deleted.endRelationType}`|`${AddTags.endRelationType}`|`${RemoveTags.endRelationType}`]->${postDef.toPattern}
      where ${ updatedDef.name }.status = ${ PENDING }
      optional match ${ votesDef.toPattern(true, false) }
      optional match ${ proposesTagDef.toPattern(false, true) }
      optional match ${ proposesClassifyDef.toPattern(false, true) }
      return *
      """

      val discourse = Discourse(db.queryGraph(query, ctx.params))
      discourse.changeRequests
    }.getOrElse(Seq.empty)))
  }

}

case class PostHasHistoryAccess() extends RelationAccessDefault[Post, ChangeRequest] {
  import formatters.json.FinishedChangeRequestFormat.CRFormat

  val nodeFactory = ChangeRequest

  override def read(context: RequestContext, param: ConnectParameter[Post]) = {
      implicit val ctx = new QueryContext
      val requestDef = LabelNodeDef[TagChangeRequest](ChangeRequest.labels)
      val postDef = FactoryUuidNodeDef(Post, param.baseUuid)
      val userDef = FactoryNodeDef(User)
      val proposesTagDef = RelationDef(requestDef, ProposesTag, FactoryNodeDef(Scope))
      val proposesClassifyDef = RelationDef(requestDef, ProposesClassify, FactoryNodeDef(Classification))

      //TODO: separate queries for subclasses
      //TODO: simpler? locking really needed?
      val query = s"""
      match ${userDef.toPattern}-[updated1:`${Updated.startRelationType}`|`${AddTags.startRelationType}`|`${RemoveTags.startRelationType}`]->${requestDef.toPattern}-[updated2:`${Updated.endRelationType}`|`${AddTags.endRelationType}`|`${RemoveTags.endRelationType}`]->${postDef.toPattern}
      where ${requestDef.name}.status = $INSTANT or ${requestDef.name}.status = $APPROVED
      optional match ${ proposesTagDef.toPattern(false, true) }
      optional match ${ proposesClassifyDef.toPattern(false, true) }
      return * order by ${requestDef.name}.timestamp DESC
      """

      val discourse = Discourse(db.queryGraph(query, ctx.params))
      // Ok(Json.toJson(discourse.changeRequests))
      //TODO: why need to sort?
      Ok(Json.toJson(discourse.changeRequests.sortBy(- _.timestamp)))
  }
}
