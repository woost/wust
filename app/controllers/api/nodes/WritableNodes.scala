package controllers.api.nodes

import model.WustSchema._
import modules.requests._

trait WritableNodes[NODE <: UuidNode] extends NodesBase {
  def nodeSchema: NodeSchema[NODE]

  override def create = UserAwareAction { request =>
    nodeSchema.op.create(context(request))
  }

  override def update(uuid: String) = UserAwareAction { request =>
    nodeSchema.op.update(context(request), uuid)
  }

  override def connectMember(path: String, uuid: String) = UserAwareAction { request =>
    getSchema(nodeSchema.connectSchemas, path)(connectSchema => {
      connectSchema.op.create(context(request), ConnectParameter(nodeSchema.op.factory, uuid))
    })
  }

  override def connectMember(path: String, uuid: String, otherUuid: String) = UserAwareAction { request =>
    validateConnect(uuid, otherUuid)(() => {
      getSchema(nodeSchema.connectSchemas, path)(connectSchema => {
        connectSchema.op.create(context(request), ConnectParameter(nodeSchema.op.factory, uuid), otherUuid)
      })
    })
  }

  override def connectNestedMember(path: String, nestedPath: String, uuid: String, otherUuid: String) = UserAwareAction { request =>
    getHyperSchema(nodeSchema.connectSchemas, path)({
      case c@StartHyperConnectSchema(factory, op, connectSchemas) =>
        getSchema(connectSchemas, nestedPath)(schema =>
          schema.op.create(context(request), HyperConnectParameter(nodeSchema.op.factory, uuid, c.factory, c.op.nodeFactory, otherUuid))
        )
      case c@EndHyperConnectSchema(factory, op, connectSchemas)   =>
        getSchema(connectSchemas, nestedPath)(schema =>
          schema.op.create(context(request), HyperConnectParameter(c.op.nodeFactory, otherUuid, c.factory, nodeSchema.op.factory, uuid))
        )
    })
  }

  override def connectNestedMember(path: String, nestedPath: String, uuid: String, otherUuid: String, nestedUuid: String) = UserAwareAction { request =>
    validateHyperConnect(uuid, otherUuid, nestedUuid)(() => {
      getHyperSchema(nodeSchema.connectSchemas, path)({
        case c@StartHyperConnectSchema(factory, op, connectSchemas) =>
          getSchema(connectSchemas, nestedPath)(schema =>
            schema.op.create(context(request), HyperConnectParameter(nodeSchema.op.factory, uuid, c.factory, c.op.nodeFactory, otherUuid), nestedUuid)
          )
        case c@EndHyperConnectSchema(factory, op, connectSchemas)   =>
          getSchema(connectSchemas, nestedPath)(schema =>
            schema.op.create(context(request), HyperConnectParameter(c.op.nodeFactory, otherUuid, c.factory, nodeSchema.op.factory, uuid), nestedUuid)
          )
      })
    })
  }
}
