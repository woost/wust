package controllers.nodes

import formatters.json.GraphFormat._
import model.WustSchema._
import modules.db.{HyperNodeDefinition, UuidNodeDefinition}
import modules.requests._
import play.api.mvc.Action

trait WritableNodes[NODE <: UuidNode] extends NodesBase {
  //TODO: use transactions instead of db
  def nodeSchema: NodeSchema[NODE]

  override def create = UserAwareAction { request =>
    request.identity match {
      case Some(user) =>
        val nodeAdd = request.body.asJson.get
        val nodeOpt = nodeSchema.op.create(user, nodeAdd)
        // TODO: HTTP status Created
        jsonNode(nodeOpt)

      case None => Unauthorized("Only users who are logged in can create Nodes")
    }
  }

  override def update(uuid: String) = UserAwareAction { request =>
    request.identity match {
      case Some(user) =>
        val nodeAdd = request.body.asJson.get
        val nodeOpt = nodeSchema.op.update(uuid, user, nodeAdd)
        jsonNode(nodeOpt)
      case None => Unauthorized("Only users who are logged in can create Nodes")
    }
  }

  override def connectMember(path: String, uuid: String) = Action(parse.json) { request =>
    val connect = request.body.as[ConnectRequest]

    val baseNode = nodeSchema.op.toNodeDefinition(uuid)
    val connectSchema = nodeSchema.connectSchemas(path)
    val nodeOpt = connectSchema.op.create(baseNode, connect.uuid)
    jsonNode(nodeOpt)
  }

  override def connectNestedMember(path: String, nestedPath: String, uuid: String, otherUuid: String) = Action(parse.json) { request =>
    val connect = request.body.as[ConnectRequest]

    val connectSchema = nodeSchema.connectSchemas(path)
    val baseNode = nodeSchema.op.toNodeDefinition(uuid)
    val nodeOpt = connectSchema match {
      case c@StartHyperConnectSchema(outerFactory,op,connectSchemas) =>
        val hyperRel = c.toNodeDefinition(baseNode, otherUuid)
        val nestedConnectSchema = connectSchemas(nestedPath)
        nestedConnectSchema.op.createHyper(hyperRel, connect.uuid)
      case c@EndHyperConnectSchema(outerFactory,op,connectSchemas) =>
        val hyperRel = c.toNodeDefinition(baseNode, otherUuid)
        val nestedConnectSchema = connectSchemas(nestedPath)
        nestedConnectSchema.op.createHyper(hyperRel, connect.uuid)
      case _ => None
    }

    jsonNode(nodeOpt)
  }
}
