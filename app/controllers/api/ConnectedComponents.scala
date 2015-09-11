package controllers.api

import modules.db.{FactoryUuidNodeDefinition, AnyUuidNodeDefinition}
import modules.db.Database._
import formatters.json.GraphFormat._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import model.WustSchema.Post
import com.mohiva.play.silhouette.api.Silhouette
import modules.auth.HeaderEnvironmentModule
import model.WustSchema._
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator

object ConnectedComponents extends Controller with Silhouette[User, JWTAuthenticator] with HeaderEnvironmentModule  {
  def show(uuid: String) = UserAwareAction { request =>
    val discourse = connectedComponent(FactoryUuidNodeDefinition(Post, uuid), request.identity)
    if (discourse.nodes.isEmpty)
      NotFound(s"Cannot find node with uuid '$uuid'")
    else
      Ok(Json.toJson(discourse))
  }
}
