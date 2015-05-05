package controllers

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import model.WustSchema._
import model.authorizations._
import model.users.User
import modules.cake.HeaderEnvironmentModule
import formatters.json.GraphFormat._
import modules.requests._
import play.api.libs.json._
import play.api.mvc.Action
import play.api.mvc.Controller
import renesca._
import renesca.schema._
import renesca.graph.RelationType
import renesca.parameter.implicits._
import modules.db.Database._
import model.WustSchema._
import model._
import modules.live.Broadcaster

trait ContentNodes[NodeType <: ContentNode] extends ResourceRouter with Silhouette[User, JWTAuthenticator] with HeaderEnvironmentModule {
  //TODO: use transactions instead of db
  def nodeSchema: NodeSchema[NodeType]

  def create = UserAwareAction { request =>
    request.identity match {
      case Some(user) =>
        val nodeAdd = request.body.asJson.get.as[NodeAddRequest]

        val discourse = Discourse.empty
        val contentNode = nodeSchema.factory.local(nodeAdd.title, description = nodeAdd.description)
        discourse.add(contentNode)
        db.persistChanges(discourse.graph)

        // TODO: HTTP status Created
        Ok(Json.toJson(contentNode))

      case None => Unauthorized("Only users who are logged in can create Nodes")
    }
  }

  // TODO: leaks hyperedges
  // TODO: broadcast
  def destroy(uuid: String) = SecuredAction(WithRole(God)) {
    implicit request =>
      val discourse = nodeDiscourseGraph(nodeSchema.factory, uuid)
      discourse.graph.nodes.clear
      db.persistChanges(discourse.graph)
      Ok(JsObject(Seq()))
  }

  def show(uuid: String) = Action {
    val discourse = nodeDiscourseGraph(nodeSchema.factory, uuid)
    discourse.graph.nodes.headOption match {
      case Some(node) => Ok(Json.toJson(nodeSchema.factory.wrap(node)))
      case None       => BadRequest(s"Node with label ${ nodeSchema.factory.label } and uuid $uuid not found.")
    }
  }

  def update(uuid: String) = Action(parse.json) { request =>
    val nodeAdd = request.body.as[NodeAddRequest]
    val discourse = nodeDiscourseGraph(nodeSchema.factory, uuid)
    discourse.contentNodes.headOption match {
      case Some(node) => {
        node.title = nodeAdd.title
        node.description = nodeAdd.description
        db.persistChanges(discourse.graph)
        Broadcaster.broadcastEdit(nodeSchema.path, node)
        Ok(Json.toJson(node))
      }
      case None       => BadRequest(s"Node with uuid $uuid not found.")
    }
  }

  def index() = Action {
    val (_, nodes) = discourseNodes(nodeSchema.factory)
    Ok(Json.toJson(nodes))
  }
}
