package controllers

import modules.requests.{GoalAddRequest, IdeaAddRequest, NodeAddRequest, ProblemAddRequest}
import play.api.libs.json._
import play.api.mvc.{AnyContent, Action, Controller}
import org.atmosphere.play.AtmosphereCoordinator.{instance => atmosphere}

import renesca._
import renesca.graph.RelationType
import renesca.parameter.implicits._
import modules.json.GraphFormat._
import model.WustSchema._
import model._
import play.api.mvc.{Action, RequestHeader}
import securesocial.core._
import model.DemoUser

trait ContentNodesController[NodeType <: ContentNode] extends Controller with securesocial.core.SecureSocial[DemoUser] with DatabaseController {
  override implicit val env: RuntimeEnvironment[DemoUser]

  //TODO: shared code: label <-> api mapping
  //TODO: use transactions instead of db
  def factory: ContentNodeFactory[NodeType]
  def label = factory.label
  def apiname: String
  def decodeRequest(jsValue: JsValue): NodeAddRequest

  def create = SecuredAction { request =>
    val json = request.body.asJson.get
    val nodeAdd = decodeRequest(json)

    val discourse = Discourse.empty
    val contentNode = factory.local(nodeAdd.title)
    discourse.add(contentNode)
    db.persistChanges(discourse.graph)

    Ok(Json.toJson(contentNode))
  }

  // TODO: leaks hyperedges
  def remove(uuid: String) = Action {
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


  def broadcast(uuid: String, data: JsValue): Unit = {
    val broadcaster = atmosphere.framework.metaBroadcaster
    broadcaster.broadcastTo(s"/live/v1/$apiname/$uuid", data)
  }

  def broadcastDisconnect(uuid: String, otherUuid: String, label: String): Unit = {
    broadcast(uuid,
      jsonChange("disconnect", JsObject(Seq(("id", JsString(otherUuid)), ("label", JsString(label))))))
  }
}
