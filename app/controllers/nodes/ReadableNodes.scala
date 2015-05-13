package controllers.nodes

import model.WustSchema._
import modules.db.{HyperNodeDefinition, UuidNodeDefinition}
import modules.requests._
import play.api.mvc.Action

trait ReadableNodes[NODE <: UuidNode] extends NodesBase {
  //TODO: use transactions instead of db
  def nodeSchema: NodeSchema[NODE]

  override def show(uuid: String) = Action {
    val nodeOpt = nodeSchema.op.read(uuid)
    jsonNode(nodeOpt)
  }

  override def index() = Action {
    val nodeOpt = nodeSchema.op.read
    jsonNodes(nodeOpt)
  }

  // TODO: proper response on wrong path
  override def showMembers(path: String, uuid: String) = Action {
    val baseNode = nodeSchema.op.toNodeDefinition(uuid)
    val connectSchema = nodeSchema.connectSchemas(path)
    val nodeOpt = connectSchema.op.read(baseNode)
    jsonNodes(nodeOpt)
  }

  override def showNestedMembers(path: String, nestedPath: String, uuid: String, otherUuid: String) = Action {
    val connectSchema = nodeSchema.connectSchemas(path)
    val baseNode = nodeSchema.op.toNodeDefinition(uuid)
    val nodeOpt = connectSchema match {
      case c@StartHyperConnectSchema(outerFactory,op,connectSchemas) =>
        val hyperRel = c.toNodeDefinition(baseNode, otherUuid)
        val nestedConnectSchema = connectSchemas(nestedPath)
        nestedConnectSchema.op.read(hyperRel)
      case c@EndHyperConnectSchema(outerFactory,op,connectSchemas) =>
        val hyperRel = c.toNodeDefinition(baseNode, otherUuid)
        val nestedConnectSchema = connectSchemas(nestedPath)
        nestedConnectSchema.op.read(hyperRel)
      case _ => None
    }
    jsonNodes(nodeOpt)
  }
}
