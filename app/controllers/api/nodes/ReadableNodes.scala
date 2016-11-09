package controllers.api.nodes

import model.WustSchema._
import modules.requests._

trait ReadableNodes[NODE <: UuidNode] extends NodesBase {
  def nodeSchema: NodeSchema[NODE]

  override def show(uuid: String) = UserAwareAction { request =>
    nodeSchema.op.read(context(request), uuid)
  }

  override def index = UserAwareAction { request =>
    nodeSchema.op.read(context(request))
  }

  override def showMembers(path: String, uuid: String) = UserAwareAction { request =>
    getSchema(nodeSchema.connectSchemas, path)(connectSchema => {
      connectSchema.op.read(context(request), ConnectParameter(nodeSchema.op.factory, uuid))
    })
  }

  override def showNestedMembers(path: String, nestedPath: String, uuid: String, otherUuid: String) = UserAwareAction { request =>
    getHyperSchema(nodeSchema.connectSchemas, path)({
      case c @ StartHyperConnectSchema(factory, op, connectSchemas) =>
        getSchema(connectSchemas, nestedPath)(schema =>
          schema.op.read(context(request), StartHyperConnectParameter(nodeSchema.op.factory, uuid, c.factory, c.op.nodeFactory, otherUuid)))
      case c @ EndHyperConnectSchema(factory, op, connectSchemas) =>
        getSchema(connectSchemas, nestedPath)(schema =>
          schema.op.read(context(request), EndHyperConnectParameter(nodeSchema.op.factory, uuid, c.factory, c.op.nodeFactory, otherUuid)))
    })
  }
}
