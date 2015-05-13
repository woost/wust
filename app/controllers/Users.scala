package controllers

import controllers.nodes.{ReadableNodes, Nodes}
import controllers.router.{NestedResourceRouter, ResourceRouter, DefaultNestedResourceController, DefaultResourceController}
import model.WustSchema._
import modules.db.{StartAnyRelation, StartRelationRead, NodeRead}
import modules.requests.{StartConnectSchema, NodeSchema}
import play.api.libs.json._
import play.api.mvc.Action

import scala.concurrent.Await
import scala.concurrent.duration._

object Users extends ReadableNodes[User] {
  implicit val restFormat = formatters.json.UserFormats.RestFormat

  lazy val nodeSchema = NodeSchema(routePath, new NodeRead(User), Map(
    "goals"  -> StartConnectSchema(new StartRelationRead(Contributes, Goal)),
    "problems"  -> StartConnectSchema(new StartRelationRead(Contributes, Problem)),
    "ideas"  -> StartConnectSchema(new StartRelationRead(Contributes, Idea)),
    "all"  -> StartConnectSchema(new StartAnyRelation(Contributes))
  ))

  override def show(id: String) = Action {
    Await.result(userService.retrieve(id), Duration(5, SECONDS)) match {
      case Some(user) => Ok(Json.toJson(user))
      case None       => BadRequest(s"User with id '$id' not found.")
    }
  }

  override def index() = Action {
    Ok(Json.toJson(Await.result(userService.retrieve, Duration(5, SECONDS))))
  }
}
