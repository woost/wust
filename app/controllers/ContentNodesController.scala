package controllers

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import model.WustSchema._
import model.authorizations._
import model.users.User
import modules.cake.HeaderEnvironmentModule
import modules.json.GraphFormat._
import modules.requests.NodeAddRequest
import org.atmosphere.play.AtmosphereCoordinator.{instance => atmosphere}
import play.api.libs.json._
import play.api.mvc.Action
import renesca._
import renesca.graph.RelationType
import renesca.parameter.implicits._
import modules.json.GraphFormat._
import model.WustSchema._
import model._

trait ContentNodesController[NodeType <: ContentNode] extends Silhouette[User, JWTAuthenticator] with HeaderEnvironmentModule with DatabaseController {
  //TODO: shared code: label <-> api mapping
  //TODO: use transactions instead of db
  def factory: ContentNodeFactory[NodeType]
  def label = factory.label
  def apiname: String
  def decodeRequest(jsValue: JsValue): NodeAddRequest

  def create = UserAwareAction { request =>
    request.identity match {
      case Some(user) =>
        val json = request.body.asJson.get
        val nodeAdd = decodeRequest(json)

        val discourse = Discourse.empty
        val contentNode = factory.local(nodeAdd.title)
        discourse.add(contentNode)
        db.persistChanges(discourse.graph)

        // TODO: HTTP status Created
        Ok(Json.toJson(contentNode))

      case None => Unauthorized("Only users who are logged in can create Nodes")
    }
  }

  // TODO: leaks hyperedges
  def remove(uuid: String) = SecuredAction(WithRole(God)) {
    implicit request =>
      val discourse = nodeDiscourseGraph(uuid)
      discourse.graph.nodes.clear
      db.persistChanges(discourse.graph)
      Ok(JsObject(Seq()))
  }

  def show(uuid: String) = Action {
    val query = Query(s"match (n :$label {uuid: {uuid}}) return n limit 1", Map("uuid" -> uuid))
    db.queryGraph(query).nodes.headOption match {
      case Some(node) => Ok(Json.toJson(factory.create(node)))
      case None       => BadRequest(s"Node with label $label and uuid $uuid not found.")
    }
  }

  def update(uuid: String) = Action { request =>
    val json = request.body.asJson.get
    val nodeAdd = decodeRequest(json)
    val discourse = nodeDiscourseGraph(uuid)
    discourse.nodes.headOption match {
      case Some(node) => {
        node.title = nodeAdd.title
        db.persistChanges(discourse.graph)
        Ok(Json.toJson(node))
      }
      case None       => BadRequest(s"Node with uuid $uuid not found.")
    }
  }

  def disconnect(uuidFrom: String, relationType: RelationType, uuidTo: String) {
    disconnect(uuidFrom, List(relationType), uuidTo)
  }

  def disconnect(uuidFrom: String, relationTypes: Seq[RelationType], uuidTo: String) {
    val discourse = relationDiscourseGraph(uuidFrom, relationTypes, uuidTo)

    // all nodes which lie on a path between fromNode and toNode
    val connectorNodes = discourse.nodes.filter(node => uuidFrom != node.uuid && uuidTo != node.uuid)

    discourse.graph.nodes --= connectorNodes.map(_.node) //TODO: wrap boilerplate
    discourse.graph.relations.clear()
    db.persistChanges(discourse.graph)

  }

  def jsonChange(changeType: String, data: JsValue) = JsObject(Seq(
    ("type", JsString(changeType)),
    ("data", data)
  ))


  private def broadcast(uuid: String, data: JsValue): Unit = {
    val broadcaster = atmosphere.framework.metaBroadcaster
    broadcaster.broadcastTo(s"/live/v1/$apiname/$uuid", data)
  }

  def broadcastConnect(uuid: String, node: ContentNode): Unit = {
    broadcast(uuid, jsonChange("connect", Json.toJson(node)))
  }

  def broadcastDisconnect(uuid: String, otherUuid: String, label: String): Unit = {
    broadcast(uuid,
      jsonChange("disconnect", JsObject(Seq(("id", JsString(otherUuid)), ("label", JsString(label))))))
  }
}
