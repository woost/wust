package controllers.nodes

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import controllers.router.{NestedResourceController, DefaultNestedResourceController, NestedResourceRouter}
import formatters.json.GraphFormat._
import model.WustSchema._
import modules.auth.HeaderEnvironmentModule
import play.api.libs.json._

trait NodesBase extends NestedResourceRouter with DefaultNestedResourceController with Silhouette[User, JWTAuthenticator] with HeaderEnvironmentModule {

  def jsonNodes(nodeOpt: Option[Iterable[_ <: UuidNode]]) = {
    if(nodeOpt.isDefined)
      Ok(Json.toJson(nodeOpt.get))
    else
      BadRequest("Nothing found")
  }

  def jsonNode(nodeOpt: Option[_ <: UuidNode]) = {
    if(nodeOpt.isDefined)
      Ok(Json.toJson(nodeOpt.get))
    else
      BadRequest("Nothing found")
  }
}
