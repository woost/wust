package controllers.api.nodes

import model.WustSchema._
import model.auth.{God, WithRole}
import modules.requests._
import play.api.libs.json._
import play.api.mvc.Action

trait DeletableNodes[NODE <: UuidNode] extends NodesBase {
  def nodeSchema: NodeSchema[NODE]

  private def deleteResult(deleted: Boolean) = if (deleted)
      Ok(JsObject(Seq()))
    else
      BadRequest("Cannot delete Node")

  override def destroy(uuid: String) = UserAwareAction { request =>
      getResult(nodeSchema.op.delete(context(request), uuid))(deleteResult)
  }

  override def disconnectMember(path: String, uuid: String, otherUuid: String) = UserAwareAction { request =>
    getSchema(nodeSchema.connectSchemas, path)(connectSchema => {
      getResult(connectSchema.inv.op.delete(context(request), ConnectParameter(nodeSchema.op.factory, uuid), otherUuid))(deleteResult)
    })
  }

  override def disconnectNestedMember(path: String, nestedPath: String, uuid: String, otherUuid: String, nestedUuid: String) = UserAwareAction { request =>
    getHyperSchema(nodeSchema.connectSchemas, path)({
      case c@StartHyperConnectSchema(factory,op,connectSchemas) =>
        getSchema(connectSchemas, nestedPath)(schema =>
          getResult(schema.inv.op.delete(context(request), HyperConnectParameter(nodeSchema.op.factory, uuid, c.factory, c.op.nodeFactory, otherUuid), nestedUuid))(deleteResult)
        )
      case c@EndHyperConnectSchema(factory,op,connectSchemas) =>
        getSchema(connectSchemas, nestedPath)(schema =>
          getResult(schema.inv.op.delete(context(request), HyperConnectParameter(c.op.nodeFactory, otherUuid, c.factory, nodeSchema.op.factory, uuid), nestedUuid))(deleteResult)
        )
    })
  }
}
