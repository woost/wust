package controllers.api

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import model.WustSchema._
import formatters.json.PostFormat._
import modules.auth.HeaderEnvironmentModule
import modules.db.Database.db
import modules.db._
import play.api.libs.json._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import renesca.Query
import modules.db.access.custom.TaggedTaggable

object Session extends Controller with Silhouette[User, JWTAuthenticator] with HeaderEnvironmentModule {
  def history() = UserAwareAction { request =>
    request.identity.map { user =>
      val userDef = ConcreteNodeDefinition(user)
      val postDef = ConcreteFactoryNodeDefinition(Post)
      val viewedDef = RelationDefinition(userDef, Viewed, postDef)

      val query = s"match ${ viewedDef.toQuery } return ${postDef.name} order by ${viewedDef.name}.timestamp desc limit 8"
      val params = viewedDef.parameterMap
      val discourse = Discourse(db.queryGraph(Query(query, params)))

      Ok(Json.toJson(TaggedTaggable.shapeResponse(discourse.posts)))
    }.getOrElse(Unauthorized("Only users can have a session history"))
  }
}
