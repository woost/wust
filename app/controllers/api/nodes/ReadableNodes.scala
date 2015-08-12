package controllers.api.nodes

import formatters.json.ApiNodeFormat._
import model.WustSchema._
import modules.requests._
import play.api.libs.json._
import play.api.mvc.{AnyContent, Request, Action}
import renesca.schema.Node

trait ReadableNodes[NODE <: UuidNode] extends NodesBase {
  def nodeSchema: NodeSchema[NODE]

  //TODO: we need to rewrap the node in order to instantiate with the correct type
  // the factory could be a matches-factory (for traits). Then this won't have the actual type of the node
  private def jsonNode(node: Node) = Ok(Json.toJson(SchemaWrapper.wrapNode(node)))
  private def jsonNodes(nodes: Iterable[Node]) = Ok(Json.toJson(nodes.map(n => SchemaWrapper.wrapNode(n))))

  override def show(uuid: String) = UserAwareAction { request =>
    getResult(nodeSchema.op.read(context(request), uuid))(jsonNode)
  }

  override def index = UserAwareAction { request =>
    getResult(nodeSchema.op.read(context(request)))(jsonNodes)
  }

  override def showMembers(path: String, uuid: String) = UserAwareAction { request =>
    getSchema(nodeSchema.connectSchemas, path)(connectSchema => {
      getResult(connectSchema.inv.op.read(context(request), ConnectParameter(nodeSchema.op.factory, uuid)))(jsonNodes)
    })
  }

  override def showNestedMembers(path: String, nestedPath: String, uuid: String, otherUuid: String) = UserAwareAction { request =>
    getHyperSchema(nodeSchema.connectSchemas, path)({
      case c@StartHyperConnectSchema(factory,op,connectSchemas) =>
        getSchema(connectSchemas, nestedPath)(schema =>
          getResult(schema.inv.op.read(context(request), HyperConnectParameter(nodeSchema.op.factory, uuid, c.factory, c.op.nodeFactory, otherUuid)))(jsonNodes)
        )
      case c@EndHyperConnectSchema(factory,op,connectSchemas) =>
        getSchema(connectSchemas, nestedPath)(schema =>
          getResult(schema.inv.op.read(context(request), HyperConnectParameter(c.op.nodeFactory, otherUuid, c.factory, nodeSchema.op.factory, uuid)))(jsonNodes)
        )
    })
  }
}
