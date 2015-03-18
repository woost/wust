package controllers

import modules.requests.{GoalAddRequest, IdeaAddRequest, NodeAddRequest, ProblemAddRequest}
import play.api.libs.json._
import play.api.mvc.{AnyContent, Action, Controller}
import org.atmosphere.play.AtmosphereCoordinator.{instance => atmosphere}

import renesca._
import renesca.graph.RelationType
import renesca.parameter.implicits._
import modules.json.GraphFormat._
import model._

trait ContentNodesController[NodeType <: ContentNode] extends Controller with DatabaseController {
  def factory: ContentNodeFactory[NodeType]
  def label = factory.label
  def decodeRequest(jsValue: JsValue): NodeAddRequest

  def create = Action { request =>
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

  def disconnect(uuidFrom: String, relationType: RelationType, uuidTo: String): Action[AnyContent] = {
    disconnect(uuidFrom, List(relationType), uuidTo)
  }

  def disconnect(uuidFrom: String, relationTypes: Seq[RelationType], uuidTo: String) = Action {
    val discourse = relationDiscourseGraph(uuidFrom, relationTypes, uuidTo)
    val connectorNodes = discourse.nodes.filter(node => !List(uuidFrom, uuidTo).contains(node.uuid))
    discourse.graph.nodes --= connectorNodes.map(_.node)
    discourse.graph.relations.clear()
    db.persistChanges(discourse.graph)
    Ok(JsObject(Seq()))
  }
}
