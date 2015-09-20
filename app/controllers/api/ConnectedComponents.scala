package controllers.api

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import formatters.json.GraphFormat._
import model.WustSchema.{Post, _}
import modules.auth.HeaderEnvironmentModule
import modules.db.Database._
import modules.db.FactoryUuidNodeDefinition
import play.api.libs.json.Json
import play.api.mvc.Controller

object ConnectedComponents extends Controller with Silhouette[User, JWTAuthenticator] with HeaderEnvironmentModule {
  def show(uuid: String) = UserAwareAction { request =>
    val discourse = connectedComponent(FactoryUuidNodeDefinition(Post, uuid), request.identity)
    if(discourse.nodes.isEmpty)
      NotFound(s"Cannot find node with uuid '$uuid'")
    else
      Ok(Json.toJson(discourse))
  }
}
