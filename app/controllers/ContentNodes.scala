package controllers

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import model.WustSchema._
import model.authorizations._
import model.users.User
import modules.cake.HeaderEnvironmentModule
import modules.json.GraphFormat._
import modules.requests._
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
import live.Broadcaster

trait ContentNodes[NodeType <: ContentNode] extends ResourceRouter[String] with Silhouette[User, JWTAuthenticator] with HeaderEnvironmentModule with DatabaseController with Controller {
  //TODO: use transactions instead of db
  def nodeSchema: NodeSchema[NodeType]

  def create = UserAwareAction { request =>
    request.identity match {
      case Some(user) =>
        val nodeAdd = request.body.asJson.get.as[NodeAddRequest]

        val discourse = Discourse.empty
        val contentNode = nodeSchema.factory.local(nodeAdd.title)
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
      case Some(node) => Ok(Json.toJson(nodeSchema.factory.create(node)))
      case None       => BadRequest(s"Node with label ${nodeSchema.factory.label} and uuid $uuid not found.")
    }
  }

  def update(uuid: String) = Action(parse.json) { request =>
    val nodeAdd = request.body.as[NodeAddRequest]
    val discourse = nodeDiscourseGraph(nodeSchema.factory, uuid)
    discourse.contentNodes.headOption match {
      case Some(node) => {
        node.title = nodeAdd.title
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

  // TODO: proper response on wrong path
  def showMembers(path: String, uuid: String) = Action {
    nodeSchema.connectSchemas(path) match {
      case StartConnection(factory) => Ok(Json.toJson(connectedNodesDiscourseGraph(uuid, factory).contentNodes))
      case EndConnection(factory) => Ok(Json.toJson(connectedNodesDiscourseGraph(factory, uuid).contentNodes))
    }
  }

  def showNestedMembers(path: String, nestedPath: String, uuid: String, otherUuid: String) = Action {
    nodeSchema.connectSchemas(path) match {
      case StartHyperConnectSchema(outerFactory,connectSchemas) => connectSchemas(nestedPath) match {
        case StartConnectSchema(factory) => Ok(Json.toJson(hyperConnectedNodesDiscourseGraph(uuid, outerFactory, otherUuid, factory).contentNodes))
        case EndConnectSchema(factory) => Ok(Json.toJson(hyperConnectedNodesDiscourseGraph(factory, uuid, outerFactory, otherUuid).contentNodes))
      }
      case EndHyperConnectSchema(outerFactory,connectSchemas) => connectSchemas(nestedPath) match {
        case StartConnectSchema(factory) => Ok(Json.toJson(hyperConnectedNodesDiscourseGraph(otherUuid, outerFactory, uuid, factory).contentNodes))
        case EndConnectSchema(factory) => Ok(Json.toJson(hyperConnectedNodesDiscourseGraph(factory, otherUuid, outerFactory, uuid).contentNodes))
      }
      case _ => BadRequest(s"Not a nested path '$path'")
    }
  }

  def connectMember(path: String, uuid: String) = Action(parse.json) { request =>
    val connect = request.body.as[ConnectRequest]

    nodeSchema.connectSchemas(path) match {
      case StartConnection(factory) => {
        val (start,end) = connectNodes(uuid, factory, connect.uuid)
        Broadcaster.broadcastConnect(start, factory, end)
        Ok(Json.toJson(end))
      }
      case EndConnection(factory) => {
        val (start,end) = connectNodes(connect.uuid, factory, uuid)
        Broadcaster.broadcastConnect(start, factory, end)
        Ok(Json.toJson(start))
      }
    }
  }

  def connectNestedMember(path: String, nestedPath: String, uuid: String, otherUuid: String) = Action(parse.json) { request =>
    val connect = request.body.as[ConnectRequest]

    nodeSchema.connectSchemas(path) match {
      case StartHyperConnectSchema(outerFactory,connectSchemas) => connectSchemas(nestedPath) match {
        case StartConnectSchema(factory) => Ok(Json.toJson(startHyperConnectNodes(uuid, outerFactory, otherUuid, factory, connect.uuid)))
        case EndConnectSchema(factory) => Ok(Json.toJson(endHyperConnectNodes(connect.uuid, factory, uuid, outerFactory, otherUuid)))
      }
      case EndHyperConnectSchema(outerFactory,connectSchemas) => connectSchemas(nestedPath) match {
        case StartConnectSchema(factory) => Ok(Json.toJson(startHyperConnectNodes(otherUuid, outerFactory, uuid, factory, connect.uuid)))
        case EndConnectSchema(factory) => Ok(Json.toJson(endHyperConnectNodes(connect.uuid, factory, otherUuid, outerFactory, uuid)))
      }
      case _ => BadRequest(s"Not a nested path '$path'")
    }
  }

  def disconnectMember(path: String, uuid: String, otherUuid: String) = Action {
    nodeSchema.connectSchemas(path) match {
      case StartConnection(factory) => {
        disconnectNodes(uuid, factory, otherUuid)
        Broadcaster.broadcastDisconnect(uuid, factory, otherUuid)
      }
      case EndConnection(factory) => {
        disconnectNodes(otherUuid, factory, uuid)
        Broadcaster.broadcastDisconnect(otherUuid, factory, uuid)
      }
    }

    Ok(JsObject(Seq()))
  }

  def disconnectNestedMember(path: String, nestedPath: String, uuid: String, otherUuid: String, nestedUuid: String) = Action {
    BadRequest("not implemented")
  }
}
