package modules.db.access.custom

import controllers.api.nodes._
import formatters.json.RequestFormat._
import formatters.json.TagFormat.karmaTagWriter
import formatters.json.UserFormat
import model.WustSchema.{Created => SchemaCreated, _}
import modules.db.Database.db
import modules.db._
import modules.db.access._
import modules.requests._
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._

case class UserAccess() extends NodeReadBase[User] {
  val factory = User

  implicit val format = UserFormat.UserFormat

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
    val limit = context.limit
    val skip = page * limit

    implicit val ctx = new QueryContext
    val userDef = FactoryUuidNodeDefinition(User, param.baseUuid)
    val postDef = FactoryNodeDefinition(Post)
    val tagsDef = HyperNodeDefinition(FactoryNodeDefinition(Scope), Tags, postDef)
    val connectsDef = FactoryNodeDefinition(Connects)
    val connDef = RelationDefinition(postDef, ConnectsStart, connectsDef)
    val tagClassifiesDef = RelationDefinition(FactoryNodeDefinition(Classification), Classifies, tagsDef)
    val classifiesDef = RelationDefinition(FactoryNodeDefinition(Classification), Classifies, connectsDef)

    val query = s"""
    match ${ userDef.toQuery }-[r1]->(hyper:`${ SchemaCreated.label }`)-[r2]->${ postDef.toQuery }
    with distinct ${ postDef.name } order by ${ postDef.name }.timestamp skip ${ skip } limit ${ limit }
    optional match ${ tagsDef.toQuery(true, false) }
    optional match ${ tagClassifiesDef.toQuery(true, false) }
    optional match ${ connDef.toQuery(false, true) }, ${ classifiesDef.toQuery(true, false) }
    return *
    """

    val params = userDef.parameterMap ++ tagsDef.parameterMap ++ connDef.parameterMap ++ tagClassifiesDef.parameterMap ++ classifiesDef.parameterMap

    val discourse = Discourse(db.queryGraph(query, params))
    Ok(Json.toJson(discourse.posts))
  }

}

case class UserHasKarmaScopes() extends StartRelationAccessDefault[User, HasKarma, Scope] {
  val nodeFactory = Scope

  override def read(context: RequestContext, param: ConnectParameter[User]) = {
    implicit val ctx = new QueryContext
    val userDef = FactoryUuidNodeDefinition(User, param.baseUuid)
    val contextDef = FactoryNodeDefinition(Scope)
    val karmaDef = RelationDefinition(userDef, HasKarma, contextDef)

    val query = s"match ${ karmaDef.toQuery } where ${karmaDef.name}.karma <> 0 return *"
    val params = karmaDef.parameterMap
    val discourse = Discourse(db.queryGraph(query, params))

    Ok(JsArray(discourse.scopes.map(karmaTagWriter)))
  }
}

case class UserHasKarmaLog() extends StartRelationAccessDefault[User, KarmaLog, Post] {
  import formatters.json.UserFormat._

  val nodeFactory = Post

  override def read(context: RequestContext, param: ConnectParameter[User]) = {
    implicit val ctx = new QueryContext
    val userDef = FactoryUuidNodeDefinition(User, param.baseUuid)
    val nodeDef = LabelNodeDefinition[Post](Set.empty)
    val karmaLogDef = HyperNodeDefinition(userDef, KarmaLog, nodeDef)
    val logOnScopeDef = RelationDefinition(karmaLogDef, LogOnScope, FactoryNodeDefinition(Scope))

    val tagDef = FactoryNodeDefinition(Scope)
    val connectsDef = FactoryNodeDefinition(Connects)
    val tagsDef = HyperNodeDefinition(tagDef, Tags, nodeDef)
    val tagClassifiesDef = RelationDefinition(FactoryNodeDefinition(Classification), Classifies, tagsDef)
    val connDef = RelationDefinition(nodeDef, ConnectsStart, connectsDef)
    val classifiesDef = RelationDefinition(FactoryNodeDefinition(Classification), Classifies, connectsDef)

    val query = s"""
    match ${ karmaLogDef.toQuery }, ${ logOnScopeDef.toQuery(false, true) }
    optional match ${ tagsDef.toQuery(true, false) }
    optional match ${ tagClassifiesDef.toQuery(true, false) }
    optional match ${ connDef.toQuery(false, true) }, ${ classifiesDef.toQuery(true, false) }
    return * order by ${karmaLogDef.name}.timestamp
    """

    val params = karmaLogDef.parameterMap ++ logOnScopeDef.parameterMap ++ tagsDef.parameterMap ++ tagClassifiesDef.parameterMap ++ classifiesDef.parameterMap
    val discourse = Discourse(db.queryGraph(query, params))

    Ok(Json.toJson(discourse.karmaLogs))
  }
}

case class UserMarks() extends StartRelationAccessDefault[User, Marks, Post] {
  import formatters.json.PostFormat._

  val nodeFactory = Post

  //TODO: decorator with baseuuid in callback
  def allowed(userOpt: Option[User], uuid: String)(handler: => Result) = userOpt.filter(_.uuid == uuid).map(_ => handler).getOrElse(Forbidden("Marks are private"))

  object CreateDelete extends StartConRelationAccessBase[User, Marks, Post] with StartRelationDeleteBase[User, Marks, Post] {
    implicit val format = PostFormat
    val nodeFactory = Post
    val factory = Marks
  }

  override def delete(context: RequestContext, param: ConnectParameter[User], uuid: String) = allowed(context.user, param.baseUuid) {
    CreateDelete.delete(context, param, uuid)
  }

  override def create(context: RequestContext, param: ConnectParameter[User]) = allowed(context.user, param.baseUuid) {
    CreateDelete.create(context, param)
  }

  override def create(context: RequestContext, param: ConnectParameter[User], uuid: String) = allowed(context.user, param.baseUuid) {
    CreateDelete.create(context, param, uuid)
  }

  override def read(context: RequestContext, param: ConnectParameter[User]) = {
    implicit val ctx = new QueryContext
    val userDef = FactoryUuidNodeDefinition(User, param.baseUuid)
    val nodeDef = FactoryNodeDefinition(Post)
    val marksDef = RelationDefinition(userDef, Marks, nodeDef)

    val tagDef = FactoryNodeDefinition(Scope)
    val connectsDef = FactoryNodeDefinition(Connects)
    val tagsDef = HyperNodeDefinition(tagDef, Tags, nodeDef)
    val tagClassifiesDef = RelationDefinition(FactoryNodeDefinition(Classification), Classifies, tagsDef)
    val connDef = RelationDefinition(nodeDef, ConnectsStart, connectsDef)
    val classifiesDef = RelationDefinition(FactoryNodeDefinition(Classification), Classifies, connectsDef)

    val query = s"""
    match ${ marksDef.toQuery }
    optional match ${ tagsDef.toQuery(true, false) }
    optional match ${ tagClassifiesDef.toQuery(true, false) }
    optional match ${ connDef.toQuery(false, true) }, ${ classifiesDef.toQuery(true, false) }
    return *
    """

    val params = marksDef.parameterMap ++ tagsDef.parameterMap ++ tagClassifiesDef.parameterMap ++ classifiesDef.parameterMap
    val discourse = Discourse(db.queryGraph(query, params))

    Ok(Json.toJson(discourse.posts))
  }
}
