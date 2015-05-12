package controllers

import controllers.router.NestedResourceRouter
import formatters.json.GraphFormat._
import model.WustSchema._
import modules.db.Database._
import modules.db._
import modules.live.Broadcaster
import modules.requests._
import play.api.libs.json._
import play.api.mvc.Action

trait NestedNodes[NodeType <: ContentNode] extends NestedResourceRouter with ContentNodes[NodeType] {
  def nodeSchema: NodeSchema[NodeType]

  // TODO: proper response on wrong path
  def showMembers(path: String, uuid: String) = Action {
    val baseNode = UuidNodeDefinition(nodeSchema.factory, uuid)
    nodeSchema.connectSchemas(path) match {
      //TODO: code dup
      case StartConnectSchema(factory,nodeFactory) => Ok(Json.toJson(startConnectedNodesDiscourseGraph(RelationDefinition(baseNode, factory, LabelNodeDefinition(nodeFactory))).contentNodes))
      case StartHyperConnectSchema(factory,nodeFactory,_) => Ok(Json.toJson(startConnectedNodesDiscourseGraph(RelationDefinition(baseNode, factory, LabelNodeDefinition(nodeFactory))).contentNodes))
      case EndConnectSchema(factory,nodeFactory) => Ok(Json.toJson(endConnectedNodesDiscourseGraph(RelationDefinition(LabelNodeDefinition(nodeFactory), factory, baseNode)).contentNodes))
      case EndHyperConnectSchema(factory,nodeFactory,_) => Ok(Json.toJson(endConnectedNodesDiscourseGraph(RelationDefinition(LabelNodeDefinition(nodeFactory), factory, baseNode)).contentNodes))
    }
  }

  def showNestedMembers(path: String, nestedPath: String, uuid: String, otherUuid: String) = Action {
    val connectSchema = nodeSchema.connectSchemas(path)
    val baseNode = UuidNodeDefinition(nodeSchema.factory, uuid)
    connectSchema match {
      case StartHyperConnectSchema(outerFactory,nodeFactory,connectSchemas) =>
        val otherNode = UuidNodeDefinition(nodeFactory, otherUuid)
        val hyperRel = HyperNodeDefinition(baseNode, outerFactory, otherNode)
        connectSchemas(nestedPath) match {
          case StartConnectSchema(factory, nodeFactory) => Ok(Json.toJson(startConnectedNodesDiscourseGraph(RelationDefinition(hyperRel, factory, LabelNodeDefinition(nodeFactory))).contentNodes))
          case EndConnectSchema(factory, nodeFactory) => Ok(Json.toJson(endConnectedNodesDiscourseGraph(RelationDefinition(LabelNodeDefinition(nodeFactory), factory, hyperRel)).contentNodes))
        }
      case EndHyperConnectSchema(outerFactory,nodeFactory,connectSchemas) =>
        val otherNode = UuidNodeDefinition(nodeFactory, otherUuid)
        val hyperRel = HyperNodeDefinition(otherNode, outerFactory, baseNode)
        connectSchemas(nestedPath) match {
          case StartConnectSchema(factory, nodeFactory) => Ok(Json.toJson(startConnectedNodesDiscourseGraph(RelationDefinition(hyperRel, factory, LabelNodeDefinition(nodeFactory))).contentNodes))
          case EndConnectSchema(factory, nodeFactory) => Ok(Json.toJson(endConnectedNodesDiscourseGraph(RelationDefinition(LabelNodeDefinition(nodeFactory), factory, hyperRel)).contentNodes))
        }
      case _ => BadRequest(s"Not a nested path '$path'")
    }
  }

  def connectMember(path: String, uuid: String) = Action(parse.json) { request =>
    val connect = request.body.as[ConnectRequest]

    val connectSchema = nodeSchema.connectSchemas(path)
    val baseNode = UuidNodeDefinition(nodeSchema.factory, uuid)
    connectSchema match {
        //TODO: code dup
      case StartConnectSchema(factory,nodeFactory) => {
        val otherNode = UuidNodeDefinition(nodeFactory, connect.uuid)
        val (start,end) = connectNodes(RelationDefinition(baseNode, factory, otherNode))
        Broadcaster.broadcastConnect(start, factory, end)
        Ok(Json.toJson(end))
      }
      case StartHyperConnectSchema(factory,nodeFactory,_) => {
        val otherNode = UuidNodeDefinition(nodeFactory, connect.uuid)
        val (start,end) = connectNodes(RelationDefinition(baseNode, factory, otherNode))
        Broadcaster.broadcastConnect(start, factory, end)
        Ok(Json.toJson(end))
      }
      case EndConnectSchema(factory,nodeFactory) => {
        val otherNode = UuidNodeDefinition(nodeFactory, connect.uuid)
        val (start,end) = connectNodes(RelationDefinition(otherNode, factory, baseNode))
        Broadcaster.broadcastConnect(start, factory, end)
        Ok(Json.toJson(start))
      }
      case EndHyperConnectSchema(factory,nodeFactory,_) => {
        val otherNode = UuidNodeDefinition(nodeFactory, connect.uuid)
        val (start,end) = connectNodes(RelationDefinition(otherNode, factory, baseNode))
        Broadcaster.broadcastConnect(start, factory, end)
        Ok(Json.toJson(start))
      }
    }
  }

  def connectNestedMember(path: String, nestedPath: String, uuid: String, otherUuid: String) = Action(parse.json) { request =>
    val connect = request.body.as[ConnectRequest]

    val connectSchema = nodeSchema.connectSchemas(path)
    val baseNode = UuidNodeDefinition(nodeSchema.factory, uuid)
    connectSchema match {
      case StartHyperConnectSchema(outerFactory,nodeFactory,connectSchemas) =>
        val otherNode = UuidNodeDefinition(nodeFactory, otherUuid)
        val nestedConnectSchema = connectSchemas(nestedPath)
        val hyperRel = HyperNodeDefinition(baseNode, outerFactory, otherNode)
        nestedConnectSchema match {
          case StartConnectSchema(factory,nodeFactory) =>
            val nestedNode = UuidNodeDefinition(nodeFactory, connect.uuid)
            val (start, end) = startHyperConnectNodes(RelationDefinition(hyperRel, factory, nestedNode))
            Broadcaster.broadcastHyperConnect(uuid, outerFactory, otherUuid, factory, end)
            Ok(Json.toJson(end))
          case EndConnectSchema(factory,nodeFactory) =>
            val nestedNode = UuidNodeDefinition(nodeFactory, connect.uuid)
            val (start, end) = endHyperConnectNodes(RelationDefinition(nestedNode, factory, hyperRel))
            Broadcaster.broadcastHyperConnect(uuid, outerFactory, otherUuid, factory, start)
            Ok(Json.toJson(start))
        }
      case EndHyperConnectSchema(outerFactory,nodeFactory,connectSchemas) =>
        val otherNode = UuidNodeDefinition(nodeFactory, otherUuid)
        val nestedConnectSchema = connectSchemas(nestedPath)
        val hyperRel = HyperNodeDefinition(otherNode, outerFactory, baseNode)
        nestedConnectSchema match {
          case StartConnectSchema(factory,nodeFactory) => {
            val nestedNode = UuidNodeDefinition(nodeFactory, connect.uuid)
            val (start, end) = startHyperConnectNodes(RelationDefinition(hyperRel, factory, nestedNode))
            Broadcaster.broadcastHyperConnect(otherUuid, outerFactory, uuid, factory, end)
            Ok(Json.toJson(end))
          }
          case EndConnectSchema(factory,nodeFactory) => {
            val nestedNode = UuidNodeDefinition(nodeFactory, connect.uuid)
            val (start, end) = endHyperConnectNodes(RelationDefinition(nestedNode, factory, hyperRel))
            Broadcaster.broadcastHyperConnect(otherUuid, outerFactory, uuid, factory, start)
            Ok(Json.toJson(start))
          }
        }
      case _ => BadRequest(s"Not a nested path '$path'")
    }
  }

  def disconnectMember(path: String, uuid: String, otherUuid: String) = Action {
    val connectSchema = nodeSchema.connectSchemas(path)
    val baseNode = UuidNodeDefinition(nodeSchema.factory, uuid)
    connectSchema match {
      //TODO: code dup
      case StartConnectSchema(factory,nodeFactory) => {
        val otherNode = UuidNodeDefinition(nodeFactory, otherUuid)
        disconnectNodes(RelationDefinition(baseNode, factory, otherNode))
        Broadcaster.broadcastDisconnect(uuid, factory, otherUuid)
      }
      case StartHyperConnectSchema(factory,nodeFactory,_) => {
        val otherNode = UuidNodeDefinition(nodeFactory, otherUuid)
        disconnectNodes(RelationDefinition(baseNode, factory, otherNode))
        Broadcaster.broadcastDisconnect(uuid, factory, otherUuid)
      }
      case EndConnectSchema(factory,nodeFactory) => {
        val otherNode = UuidNodeDefinition(nodeFactory, otherUuid)
        disconnectNodes(RelationDefinition(otherNode, factory, baseNode))
        Broadcaster.broadcastDisconnect(otherUuid, factory, uuid)
      }
      case EndHyperConnectSchema(factory,nodeFactory,_) => {
        val otherNode = UuidNodeDefinition(nodeFactory, otherUuid)
        disconnectNodes(RelationDefinition(otherNode, factory, baseNode))
        Broadcaster.broadcastDisconnect(otherUuid, factory, uuid)
      }
    }

    Ok(JsObject(Seq()))
  }

  def disconnectNestedMember(path: String, nestedPath: String, uuid: String, otherUuid: String, nestedUuid: String) = Action {
    val connectSchema = nodeSchema.connectSchemas(path)
    val baseNode = UuidNodeDefinition(nodeSchema.factory, uuid)
    connectSchema match {
      case StartHyperConnectSchema(outerFactory,nodeFactory,connectSchemas) =>
        val otherNode = UuidNodeDefinition(nodeFactory, otherUuid)
        val nestedConnectSchema = connectSchemas(nestedPath)
        val hyperRel = HyperNodeDefinition(baseNode, outerFactory, otherNode)
        nestedConnectSchema match {
          case StartConnectSchema(factory,nodeFactory) => {
            val nestedNode = UuidNodeDefinition(nodeFactory, nestedUuid)
            disconnectNodes(RelationDefinition(hyperRel, factory, nestedNode))
            Broadcaster.broadcastHyperDisconnect(uuid, outerFactory, otherUuid, factory, nestedUuid)
            Ok(JsObject(Seq()))
          }
          case EndConnectSchema(factory,nodeFactory) => {
            val nestedNode = UuidNodeDefinition(nodeFactory, nestedUuid)
            disconnectNodes(RelationDefinition(nestedNode, factory, hyperRel))
            Broadcaster.broadcastHyperDisconnect(uuid, outerFactory, otherUuid, factory, nestedUuid)
            Ok(JsObject(Seq()))
          }
        }
      case EndHyperConnectSchema(outerFactory,nodeFactory,connectSchemas) =>
        val otherNode = UuidNodeDefinition(nodeFactory, otherUuid)
        val nestedConnectSchema = connectSchemas(nestedPath)
        val hyperRel = HyperNodeDefinition(otherNode, outerFactory, baseNode)
        nestedConnectSchema match {
          case StartConnectSchema(factory,nodeFactory) => {
            val nestedNode = UuidNodeDefinition(nodeFactory, nestedUuid)
            disconnectNodes(RelationDefinition(hyperRel, factory, nestedNode))
            Broadcaster.broadcastHyperDisconnect(otherUuid, outerFactory, uuid, factory, nestedUuid)
            Ok(JsObject(Seq()))
          }
          case EndConnectSchema(factory,nodeFactory) => {
            val nestedNode = UuidNodeDefinition(nodeFactory, nestedUuid)
            disconnectNodes(RelationDefinition(nestedNode, factory, hyperRel))
            Broadcaster.broadcastHyperDisconnect(otherUuid, outerFactory, uuid, factory, nestedUuid)
            Ok(JsObject(Seq()))
          }
        }
      case _ => BadRequest(s"Not a nested path '$path'")
    }
  }
}
