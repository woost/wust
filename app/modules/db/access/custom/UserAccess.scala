package modules.db.access.custom

import controllers.api.nodes._
import formatters.json.RequestFormat._
import formatters.json.TagFormat.karmaTagWriter
import model.WustSchema.{Created => SchemaCreated, _}
import modules.db.Database.db
import modules.db._
import modules.db.access._
import modules.requests._
import play.api.libs.json._
import play.api.mvc.Results._

class UserAccess extends NodeReadBase[User] {
  val factory = User

  import formatters.json.UserFormat._

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

object UserAccess {
  def apply = new UserAccess
}

case class UserContributions() extends RelationAccessDefault[User, Post] {

  import formatters.json.PostFormat._

  val nodeFactory = Post

  override def read(context: RequestContext, param: ConnectParameter[User]) = {
    val page = context.page.getOrElse(0)
    val limit = context.limit
    val skip = page * limit

    val userDef = FactoryUuidNodeDefinition(User, param.baseUuid)
    val postDef = ConcreteFactoryNodeDefinition(Post)
    val tagsDef = HyperNodeDefinition(ConcreteFactoryNodeDefinition(Scope), Tags, postDef)
    val connectsDef = ConcreteFactoryNodeDefinition(Connects)
    val connDef = RelationDefinition(postDef, PostToConnects, connectsDef)
    val tagClassifiesDef = RelationDefinition(ConcreteFactoryNodeDefinition(Classification), Classifies, tagsDef)
    val classifiesDef = RelationDefinition(ConcreteFactoryNodeDefinition(Classification), Classifies, connectsDef)

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
    val userDef = FactoryUuidNodeDefinition(User, param.baseUuid)
    val contextDef = ConcreteFactoryNodeDefinition(Scope)
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
    val userDef = FactoryUuidNodeDefinition(User, param.baseUuid)
    val nodeDef = ConcreteFactoryNodeDefinition(Post)
    val karmaLogDef = HyperNodeDefinition(userDef, KarmaLog, nodeDef)
    val logOnScopeDef = RelationDefinition(karmaLogDef, LogOnScope, ConcreteFactoryNodeDefinition(Scope))

    val tagDef = ConcreteFactoryNodeDefinition(Scope)
    val connectsDef = ConcreteFactoryNodeDefinition(Connects)
    val tagsDef = HyperNodeDefinition(tagDef, Tags, nodeDef)
    val tagClassifiesDef = RelationDefinition(ConcreteFactoryNodeDefinition(Classification), Classifies, tagsDef)
    val connDef = RelationDefinition(nodeDef, PostToConnects, connectsDef)
    val classifiesDef = RelationDefinition(ConcreteFactoryNodeDefinition(Classification), Classifies, connectsDef)

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
