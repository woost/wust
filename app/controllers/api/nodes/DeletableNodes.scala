package controllers.api.nodes

import model.WustSchema._
import modules.requests._
import controllers.api.auth.PublicReadingControl

trait DeletableNodes[NODE <: UuidNode] extends NodesBase with PublicReadingControl {
  def nodeSchema: NodeSchema[NODE]

  override def destroy(uuid: String) = PublicReadingControlledUserAwareAction { request =>
    nodeSchema.op.delete(context(request), uuid)
  }

  override def disconnectMember(path: String, uuid: String, otherUuid: String) = PublicReadingControlledUserAwareAction { request =>
    getSchema(nodeSchema.connectSchemas, path)(connectSchema => {
      connectSchema.op.delete(context(request), ConnectParameter(nodeSchema.op.factory, uuid), otherUuid)
    })
  }

  override def disconnectNestedMember(path: String, nestedPath: String, uuid: String, otherUuid: String, nestedUuid: String) = PublicReadingControlledUserAwareAction { request =>
    getHyperSchema(nodeSchema.connectSchemas, path)({
      case c@StartHyperConnectSchema(factory, op, connectSchemas) =>
        getSchema(connectSchemas, nestedPath)(schema =>
          schema.op.delete(context(request), StartHyperConnectParameter(nodeSchema.op.factory, uuid, c.factory, c.op.nodeFactory, otherUuid), nestedUuid)
        )
      case c@EndHyperConnectSchema(factory, op, connectSchemas)   =>
        getSchema(connectSchemas, nestedPath)(schema =>
          schema.op.delete(context(request), EndHyperConnectParameter(nodeSchema.op.factory, uuid, c.factory, c.op.nodeFactory, otherUuid), nestedUuid)
        )
    })
  }
}
