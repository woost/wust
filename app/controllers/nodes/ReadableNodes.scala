package controllers.nodes

import formatters.json.GraphFormat._
import model.WustSchema._
import modules.requests._
import modules.requests.types.AccessibleConnectSchema
import play.api.libs.json.Json
import play.api.mvc.Action

trait ReadableNodes[NODE <: UuidNode] extends NodesBase {
  //TODO: use transactions instead of db
  protected val nodeSchema: NodeSchema[NODE]

  private def jsonNode(node: UuidNode) = Ok(Json.toJson(node))
  private def jsonNodes(nodes: Iterable[_ <: UuidNode]) = Ok(Json.toJson(nodes))

  override def show(uuid: String) = Action {
    getResult(nodeSchema.op.read(uuid))(jsonNode)
  }

  override def index() = Action {
    getResult(nodeSchema.op.read)(jsonNodes)
  }

  // TODO: proper response on wrong path
  override def showMembers(path: String, uuid: String) = Action {
    val baseNode = nodeSchema.op.toNodeDefinition(uuid)
    getSchema(nodeSchema.connectSchemas, path)(connectSchema => {
      getResult(connectSchema.op.read(baseNode))(jsonNodes)
    })
  }

  override def showNestedMembers(path: String, nestedPath: String, uuid: String, otherUuid: String) = Action {
    val baseNode = nodeSchema.op.toNodeDefinition(uuid)
    getHyperSchema(nodeSchema.connectSchemas, path)({
      case c@StartHyperConnectSchema(_,_,connectSchemas) =>
        val hyperRel = c.toNodeDefinition(baseNode, otherUuid)
        getSchema(connectSchemas, nestedPath)(schema =>
          getResult(schema.op.read(hyperRel))(jsonNodes)
        )
      case c@EndHyperConnectSchema(_,_,connectSchemas) =>
        val hyperRel = c.toNodeDefinition(baseNode, otherUuid)
        getSchema(connectSchemas, nestedPath)(schema =>
          getResult(schema.op.read(hyperRel))(jsonNodes)
        )
    })
  }
}
