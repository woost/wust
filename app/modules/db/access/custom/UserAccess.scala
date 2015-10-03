package modules.db.access.custom

import controllers.api.nodes._
import formatters.json.RequestFormat._
import formatters.json.TagFormat.karmaTagWriter
import formatters.json.UserFormat
import model.WustSchema.{Created => SchemaCreated, _}
import modules.db.Database.db
import modules.db._
import modules.db.helpers.TaggedTaggable
import modules.db.access._
import modules.requests._
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._
import renesca.parameter.implicits._

case class UserAccess() extends NodeReadBase[User] {
  val factory = User

  implicit val format = UserFormat.UserFormat

  override def read(context: RequestContext) = {
    val page = context.page.getOrElse(0)
    val limit = context.sizeWithDefault
    val skip = page * limit

    implicit val ctx = new QueryContext
    val userDef = FactoryNodeDef(User)
    val scopeDef = FactoryNodeDef(Scope)
    val hasKarmaDef = RelationDef(userDef, HasKarma, scopeDef)

    val (aggregateOp, scopeCondition, scopeParams) = if (context.scopes.isEmpty)
      ("sum", "", Map.empty)
    else
      ("sum", s"where ${scopeDef.name}.uuid in {scopeUuids}", Map("scopeUuids" -> context.scopes))

    val query = s"""
    match ${userDef.toQuery}
    optional match ${ hasKarmaDef.toQuery(false, true) }
    $scopeCondition
    with ${userDef.name}, ${hasKarmaDef.name}, ${aggregateOp}(${hasKarmaDef.name}.karma) as karmaAggregate order by karmaAggregate DESC
    return ${userDef.name}, ${hasKarmaDef.name}
    """

    val params = ctx.params ++ scopeParams
    val discourse = Discourse(db.queryGraph(query, params))
    Ok(Json.toJson(discourse.users))
  }

  override def update(context: RequestContext, uuid: String) = {
    context.withUser { user =>
      context.withJson { (request: UserUpdateRequest) =>
        //TODO: sanity check + welcome mail
        if(request.email.isDefined)
          user.email = request.email

        db.transaction(_.persistChanges(user)) match {
          case Some(err) => BadRequest(s"Cannot update User: $err'")
          case _         => Ok(Json.toJson(user))
        }
      }
    }
  }
}

case class UserContributions() extends RelationAccessDefault[User, Post] {
  import formatters.json.PostFormat._

  val nodeFactory = Post

  override def read(context: RequestContext, param: ConnectParameter[User]) = {
    val page = context.page.getOrElse(0)
    val limit = context.sizeWithDefault
    val skip = page * limit

    implicit val ctx = new QueryContext
    val userDef = FactoryUuidNodeDef(User, param.baseUuid)
    val postDef = FactoryNodeDef(Post)
    val tagsDef = HyperNodeDef(FactoryNodeDef(Scope), Tags, postDef)
    val connectsDef = FactoryNodeDef(Connects)
    val connDef = RelationDef(postDef, ConnectsStart, connectsDef)
    val tagClassifiesDef = RelationDef(FactoryNodeDef(Classification), Classifies, tagsDef)
    val classifiesDef = RelationDef(FactoryNodeDef(Classification), Classifies, connectsDef)

    val query = s"""
    match ${ userDef.toQuery }-[r1]->(hyper:`${ SchemaCreated.label }`)-[r2]->${ postDef.toQuery }
    with distinct ${ postDef.name } order by ${ postDef.name }.timestamp skip ${ skip } limit ${ limit }
    optional match ${ tagsDef.toQuery(true, false) }
    optional match ${ tagClassifiesDef.toQuery(true, false) }
    optional match ${ connDef.toQuery(false, true) }, ${ classifiesDef.toQuery(true, false) }
    return *
    """

    val discourse = Discourse(db.queryGraph(query, ctx.params))
    Ok(Json.toJson(discourse.posts))
  }

}

//TODO: include in read users
case class UserHasKarmaScopes() extends StartRelationAccessDefault[User, HasKarma, Scope] {
  val nodeFactory = Scope

  override def read(context: RequestContext, param: ConnectParameter[User]) = {
    implicit val ctx = new QueryContext
    val userDef = FactoryUuidNodeDef(User, param.baseUuid)
    val karmaDef = RelationDef(userDef, HasKarma, FactoryNodeDef(Scope))

    val query = s"match ${ karmaDef.toQuery } where ${karmaDef.name}.karma <> 0 return *"
    val discourse = Discourse(db.queryGraph(query, ctx.params))

    Ok(JsArray(discourse.scopes.map(karmaTagWriter)))
  }
}

case class UserHasKarmaLog() extends StartRelationAccessDefault[User, KarmaLog, Post] {
  import formatters.json.UserFormat._

  val nodeFactory = Post

  override def read(context: RequestContext, param: ConnectParameter[User]) = {
    implicit val ctx = new QueryContext
    val userDef = FactoryUuidNodeDef(User, param.baseUuid)
    val nodeDef = LabelNodeDef[Post](Set.empty)
    val karmaLogDef = HyperNodeDef(userDef, KarmaLog, nodeDef)
    val logOnScopeDef = RelationDef(karmaLogDef, LogOnScope, FactoryNodeDef(Scope))

    val tagDef = FactoryNodeDef(Scope)
    val connectsDef = FactoryNodeDef(Connects)
    val tagsDef = HyperNodeDef(tagDef, Tags, nodeDef)
    val tagClassifiesDef = RelationDef(FactoryNodeDef(Classification), Classifies, tagsDef)
    val connDef = RelationDef(nodeDef, ConnectsStart, connectsDef)
    val classifiesDef = RelationDef(FactoryNodeDef(Classification), Classifies, connectsDef)

    val query = s"""
    match ${ karmaLogDef.toQuery }, ${ logOnScopeDef.toQuery(false, true) }
    optional match ${ tagsDef.toQuery(true, false) }
    optional match ${ tagClassifiesDef.toQuery(true, false) }
    optional match ${ connDef.toQuery(false, true) }, ${ classifiesDef.toQuery(true, false) }
    return * order by ${karmaLogDef.name}.timestamp
    """

    val discourse = Discourse(db.queryGraph(query, ctx.params))

    Ok(Json.toJson(discourse.karmaLogs))
  }
}

case class UserMarks() extends StartRelationWriteBase[User, Marks, Post] with StartRelationDeleteBase[User, Marks, Post] {
  implicit val format = formatters.json.PostFormat.PostFormat

  val nodeFactory = Post
  val factory = Marks

  override def read(context: RequestContext, param: ConnectParameter[User]) = {
    implicit val ctx = new QueryContext
    val userDef = FactoryUuidNodeDef(User, param.baseUuid)
    val nodeDef = FactoryNodeDef(Post)
    val marksDef = RelationDef(userDef, Marks, nodeDef)

    val tagDef = FactoryNodeDef(Scope)
    val connectsDef = FactoryNodeDef(Connects)
    val tagsDef = HyperNodeDef(tagDef, Tags, nodeDef)
    val tagClassifiesDef = RelationDef(FactoryNodeDef(Classification), Classifies, tagsDef)
    val connDef = RelationDef(nodeDef, ConnectsStart, connectsDef)
    val classifiesDef = RelationDef(FactoryNodeDef(Classification), Classifies, connectsDef)

    val query = s"""
    match ${ marksDef.toQuery }
    optional match ${ tagsDef.toQuery(true, false) }
    optional match ${ tagClassifiesDef.toQuery(true, false) }
    optional match ${ connDef.toQuery(false, true) }, ${ classifiesDef.toQuery(true, false) }
    return *
    """

    val discourse = Discourse(db.queryGraph(query, ctx.params))

    Ok(Json.toJson(discourse.posts))
  }
}

case class UserHasHistory() extends StartRelationAccessDefault[User, Viewed, Post] {
  import formatters.json.PostFormat.PostFormat

  val nodeFactory = Post

  override def read(context: RequestContext, param: ConnectParameter[User]) = {
      implicit val ctx = new QueryContext
      val userDef = FactoryUuidNodeDef(User, param.baseUuid)
      val postDef = FactoryNodeDef(Post)
      val viewedDef = RelationDef(userDef, Viewed, postDef)

      val query = s"match ${ viewedDef.toQuery } return ${ postDef.name } order by ${ viewedDef.name }.timestamp desc limit 8"
      val discourse = Discourse(db.queryGraph(query, ctx.params))

      Ok(Json.toJson(TaggedTaggable.shapeResponse(discourse.posts)))
  }
}
