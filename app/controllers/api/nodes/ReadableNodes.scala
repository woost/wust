package controllers.api.nodes

import formatters.json.ApiNodeFormat._
import model.WustSchema._
import modules.requests._
import play.api.libs.json._
import play.api.mvc.{AnyContent, Request, Action}
import renesca.schema.Node

trait ReadableNodes[NODE <: UuidNode] extends NodesBase {
  def nodeSchema: NodeSchema[NODE]

  private def jsonNode(node: Node) = Ok(Json.toJson(node))
  private def jsonNodes(nodes: Iterable[Node]) = Ok(Json.toJson(nodes))

  override def show(uuid: String) = UserAwareAction { request =>
    getResult(nodeSchema.op.read(context(request), uuid))(jsonNode)
  }

  override def index = UserAwareAction { request =>
    getResult(nodeSchema.op.read(context(request)))(jsonNodes)
  }

  override def showMembers(path: String, uuid: String) = UserAwareAction { request =>
    val baseNode = nodeSchema.op.toNodeDefinition(uuid)
    getSchema(nodeSchema.connectSchemas, path)(connectSchema => {
      getResult(connectSchema.op.read(context(request), baseNode))(jsonNodes)
    })
  }

  override def showNestedMembers(path: String, nestedPath: String, uuid: String, otherUuid: String) = UserAwareAction { request =>
    val baseNode = nodeSchema.op.toNodeDefinition(uuid)
    getHyperSchema(nodeSchema.connectSchemas, path)({
      case c@StartHyperConnectSchema(_, _, connectSchemas) =>
        val hyperRel = c.toNodeDefinition(baseNode, otherUuid)
        getSchema(connectSchemas, nestedPath)(schema =>
          getResult(schema.op.read(context(request), hyperRel))(jsonNodes)
        )
      case c@EndHyperConnectSchema(_, _, connectSchemas)   =>
        val hyperRel = c.toNodeDefinition(baseNode, otherUuid)
        getSchema(connectSchemas, nestedPath)(schema =>
          getResult(schema.op.read(context(request), hyperRel))(jsonNodes)
        )
    })
  }
}
