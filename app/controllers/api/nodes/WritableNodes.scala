package controllers.api.nodes

import model.WustSchema._
import modules.requests._
import controllers.api.auth.PublicReadingControl

trait WritableNodes[NODE <: UuidNode] extends NodesBase with PublicReadingControl {
  def nodeSchema: NodeSchema[NODE]

  override def create = PublicReadingControlledUserAwareAction { request =>
    nodeSchema.op.create(context(request))
  }

  override def update(uuid: String) = PublicReadingControlledUserAwareAction { request =>
    nodeSchema.op.update(context(request), uuid)
  }

  override def connectMember(path: String, uuid: String) = PublicReadingControlledUserAwareAction { request =>
    getSchema(nodeSchema.connectSchemas, path)(connectSchema => {
      connectSchema.op.create(context(request), ConnectParameter(nodeSchema.op.factory, uuid))
    })
  }

  override def connectMember(path: String, uuid: String, otherUuid: String) = PublicReadingControlledUserAwareAction { request =>
    validateConnect(uuid, otherUuid)(() => {
      getSchema(nodeSchema.connectSchemas, path)(connectSchema => {
        connectSchema.op.create(context(request), ConnectParameter(nodeSchema.op.factory, uuid), otherUuid)
      })
    })
  }

  override def connectNestedMember(path: String, nestedPath: String, uuid: String, otherUuid: String) = PublicReadingControlledUserAwareAction { request =>
    getHyperSchema(nodeSchema.connectSchemas, path)({
      case c@StartHyperConnectSchema(factory, op, connectSchemas) =>
        getSchema(connectSchemas, nestedPath)(schema =>
          schema.op.create(context(request), StartHyperConnectParameter(nodeSchema.op.factory, uuid, c.factory, c.op.nodeFactory, otherUuid))
        )
      case c@EndHyperConnectSchema(factory, op, connectSchemas)   =>
        getSchema(connectSchemas, nestedPath)(schema =>
          schema.op.create(context(request), EndHyperConnectParameter(nodeSchema.op.factory, uuid, c.factory, c.op.nodeFactory, otherUuid))
        )
    })
  }

  override def connectNestedMember(path: String, nestedPath: String, uuid: String, otherUuid: String, nestedUuid: String) = PublicReadingControlledUserAwareAction { request =>
    validateHyperConnect(uuid, otherUuid, nestedUuid)(() => {
      getHyperSchema(nodeSchema.connectSchemas, path)({
        case c@StartHyperConnectSchema(factory, op, connectSchemas) =>
          getSchema(connectSchemas, nestedPath)(schema =>
            schema.op.create(context(request), StartHyperConnectParameter(nodeSchema.op.factory, uuid, c.factory, c.op.nodeFactory, otherUuid), nestedUuid)
          )
        case c@EndHyperConnectSchema(factory, op, connectSchemas)   =>
          getSchema(connectSchemas, nestedPath)(schema =>
            schema.op.create(context(request), EndHyperConnectParameter(nodeSchema.op.factory, uuid, c.factory, c.op.nodeFactory, otherUuid), nestedUuid)
          )
      })
    })
  }
}
