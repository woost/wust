package controllers

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import model.WustSchema._
import model.authorizations._
import model.users.User
import modules.cake.HeaderEnvironmentModule
import formatters.json.GraphFormat._
import modules.db.Database._
import modules.requests._
import play.api.libs.json._
import play.api.mvc.Action
import play.api.mvc.Controller
import renesca._
import renesca.schema._
import renesca.graph.RelationType
import renesca.parameter.implicits._
import model.WustSchema._
import model._
import modules.live.Broadcaster

trait NestedNodes[NodeType <: ContentNode] extends NestedResourceRouter with ContentNodes[NodeType] {
  def nodeSchema: NodeSchema[NodeType]

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
        case StartConnectSchema(factory) => {
          val (start, end) = startHyperConnectNodes(uuid, outerFactory, otherUuid, factory, connect.uuid)
          Ok(Json.toJson(end))
        }
        case EndConnectSchema(factory) => {
          val (start, end) = endHyperConnectNodes(connect.uuid, factory, uuid, outerFactory, otherUuid)
          Ok(Json.toJson(start))
        }
      }
      case EndHyperConnectSchema(outerFactory,connectSchemas) => connectSchemas(nestedPath) match {
        case StartConnectSchema(factory) => {
          val (start, end) = startHyperConnectNodes(otherUuid, outerFactory, uuid, factory, connect.uuid)
          Ok(Json.toJson(end))
        }
        case EndConnectSchema(factory) => {
          val (start, end) = endHyperConnectNodes(connect.uuid, factory, otherUuid, outerFactory, uuid)
          Ok(Json.toJson(start))
        }
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
    nodeSchema.connectSchemas(path) match {
      case StartHyperConnectSchema(outerFactory,connectSchemas) => connectSchemas(nestedPath) match {
        case StartConnectSchema(factory) => {
          startHyperDisconnectNodes(uuid, outerFactory, otherUuid, factory, nestedUuid)
          Ok(JsObject(Seq()))
        }
        case EndConnectSchema(factory) => {
          endHyperDisconnectNodes(nestedUuid, factory, uuid, outerFactory, otherUuid)
          Ok(JsObject(Seq()))
        }
      }
      case EndHyperConnectSchema(outerFactory,connectSchemas) => connectSchemas(nestedPath) match {
        case StartConnectSchema(factory) => {
          startHyperDisconnectNodes(otherUuid, outerFactory, uuid, factory, nestedUuid)
          Ok(JsObject(Seq()))
        }
        case EndConnectSchema(factory) => {
          endHyperDisconnectNodes(nestedUuid, factory, otherUuid, outerFactory, uuid)
          Ok(JsObject(Seq()))
        }
      }
      case _ => BadRequest(s"Not a nested path '$path'")
    }
  }
}
