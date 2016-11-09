package controllers.api.nodes

import model.WustSchema._
import modules.requests._
import controllers.api.auth.PublicReadingControl

trait ReadableNodes[NODE <: UuidNode] extends NodesBase with PublicReadingControl {
  def nodeSchema: NodeSchema[NODE]

  override def show(uuid: String) = PublicReadingControlledUserAwareAction { request =>
    nodeSchema.op.read(context(request), uuid)
  }

  override def index = PublicReadingControlledUserAwareAction { request =>
    nodeSchema.op.read(context(request))
  }

  override def showMembers(path: String, uuid: String) = PublicReadingControlledUserAwareAction { request =>
    getSchema(nodeSchema.connectSchemas, path)(connectSchema => {
      connectSchema.op.read(context(request), ConnectParameter(nodeSchema.op.factory, uuid))
    })
  }

  override def showNestedMembers(path: String, nestedPath: String, uuid: String, otherUuid: String) = PublicReadingControlledUserAwareAction { request =>
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
