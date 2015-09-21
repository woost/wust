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
    val tagsDef = RelationDefinition(ConcreteFactoryNodeDefinition(Scope), Tags, postDef)
    val connectsDef = ConcreteFactoryNodeDefinition(Connects)
    val connDef = RelationDefinition(postDef, PostToConnects, connectsDef)
    val classifiesDef = RelationDefinition(ConcreteFactoryNodeDefinition(Classification), Classifies, connectsDef)

    val query = s"""
    match ${ userDef.toQuery }-[r1]->(hyper:`${ Action.label }`)-[r2]->${ postDef.toQuery }
    with ${ userDef.name }, ${ postDef.name }, r1, r2, hyper order by hyper.timestamp skip ${ skip } limit ${ limit }
    optional match ${ tagsDef.toQuery(true, false) }
    optional match ${ connDef.toQuery(false, true) }, ${ classifiesDef.toQuery(true, false) }
    return *
    """

    val params = userDef.parameterMap ++ tagsDef.parameterMap ++ connDef.parameterMap ++ classifiesDef.parameterMap

    val discourse = Discourse(db.queryGraph(query, params))
    Ok(Json.toJson(discourse.posts))
  }

}

case class UserHasKarma() extends StartRelationAccessDefault[User, HasKarma, Scope] {
  val nodeFactory = Scope

  override def read(context: RequestContext, param: ConnectParameter[User]) = {
    val userDef = FactoryUuidNodeDefinition(User, param.baseUuid)
    val contextDef = ConcreteFactoryNodeDefinition(Scope)
    val karmaDef = RelationDefinition(userDef, HasKarma, contextDef)

    val query = s"match ${ karmaDef.toQuery } return *"
    val params = karmaDef.parameterMap
    val discourse = Discourse(db.queryGraph(query, params))

    Ok(JsArray(discourse.scopes.map(karmaTagWriter)))
  }
}
