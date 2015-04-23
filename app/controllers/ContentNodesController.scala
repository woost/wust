package controllers

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import model.WustSchema._
import model.authorizations._
import model.users.User
import modules.cake.HeaderEnvironmentModule
import modules.json.GraphFormat._
import modules.requests.NodeAddRequest
import play.api.libs.json._
import play.api.mvc.Action
import play.api.mvc.Controller
import renesca._
import renesca.schema._
import renesca.graph.RelationType
import renesca.parameter.implicits._
import modules.json.GraphFormat._
import model.WustSchema._
import model._

trait ContentNodesController[NodeType <: ContentNode] extends ResourceRouter[String] with Silhouette[User, JWTAuthenticator] with HeaderEnvironmentModule with DatabaseController with Controller {
  //TODO: shared code: label <-> api mapping
  //TODO: use transactions instead of db
  def factory: ContentNodeFactory[NodeType]
  def label = factory.label
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
  def destroy(uuid: String) = SecuredAction(WithRole(God)) {
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

  def update(uuid: String) = Action(parse.json) { request =>
    val nodeAdd = decodeRequest(request.body)
    val discourse = nodeDiscourseGraph(uuid)
    discourse.contentNodes.headOption match {
      case Some(node) => {
        node.title = nodeAdd.title
        db.persistChanges(discourse.graph)
        Ok(Json.toJson(node))
      }
      case None       => BadRequest(s"Node with uuid $uuid not found.")
    }
  }

  def connectNodes[START <: UuidNode, RELATION <: SchemaAbstractRelation[START, END] with SchemaItem, END <: UuidNode](startUuid: String, factory: SchemaAbstractRelationFactory[START, RELATION, END], endUuid: String) = {
    val (discourse, (start, end)) = discourseNodes[START, END](startUuid, endUuid)
    discourse.add(factory.local(start, end))
    db.persistChanges(discourse.graph)
    (start, end)
  }

  def disconnectNodes[START <: UuidNode, RELATION <: SchemaRelation[START,END], END <: UuidNode](startUuid: String, factory: SchemaRelationFactory[START,RELATION,END], endUuid: String) {
      disconnectNodes(startUuid, List(factory.relationType), endUuid)
  }

  def disconnectNodes[START <: UuidNode, RELATION <: SchemaHyperRelation[START,_,RELATION,_,END], END <: UuidNode](startUuid: String, factory: SchemaHyperRelationFactory[START,_,RELATION,_,END], endUuid: String) {
      disconnectNodes(startUuid, List(factory.startRelationType, factory.endRelationType), endUuid)
  }

  private def disconnectNodes(startUuid: String, relationTypes: Seq[RelationType], endUuid: String) {
    val discourse = relationDiscourseGraph(startUuid, relationTypes, endUuid)

    // all nodes which lie on a path between fromNode and toNode
    val connectorNodes = discourse.contentNodes.filter(node => startUuid != node.uuid && endUuid != node.uuid)

    discourse.graph.nodes --= connectorNodes.map(_.node) //TODO: wrap boilerplate
    discourse.graph.relations.clear()
    db.persistChanges(discourse.graph)
  }
}
