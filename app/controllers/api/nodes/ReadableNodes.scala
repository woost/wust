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
  private def getPage(request: Request[AnyContent]): Option[Int] = {
    request.queryString.get("page").flatMap(_.headOption).map(_.toInt)
  }

  override def show(uuid: String) = Action {
    getResult(nodeSchema.op.read(uuid))(jsonNode)
  }

  override def index = Action { request =>
    val page = getPage(request)
    getResult(nodeSchema.op.read(page))(jsonNodes)
  }

  override def showMembers(path: String, uuid: String) = Action { request =>
    val baseNode = nodeSchema.op.toNodeDefinition(uuid)
    val page = getPage(request)
    getSchema(nodeSchema.connectSchemas, path)(connectSchema => {
      getResult(connectSchema.op.read(baseNode, page))(jsonNodes)
    })
  }

  override def showNestedMembers(path: String, nestedPath: String, uuid: String, otherUuid: String) = Action { request =>
    val baseNode = nodeSchema.op.toNodeDefinition(uuid)
    val page = getPage(request)
    getHyperSchema(nodeSchema.connectSchemas, path)({
      case c@StartHyperConnectSchema(_, _, connectSchemas) =>
        val hyperRel = c.toNodeDefinition(baseNode, otherUuid)
        getSchema(connectSchemas, nestedPath)(schema =>
          getResult(schema.op.read(hyperRel, page))(jsonNodes)
        )
      case c@EndHyperConnectSchema(_, _, connectSchemas)   =>
        val hyperRel = c.toNodeDefinition(baseNode, otherUuid)
        getSchema(connectSchemas, nestedPath)(schema =>
          getResult(schema.op.read(hyperRel, page))(jsonNodes)
        )
    })
  }
}
