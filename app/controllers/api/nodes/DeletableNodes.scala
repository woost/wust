package controllers.api.nodes

import model.WustSchema._
import model.auth.{God, WithRole}
import modules.requests._
import play.api.libs.json._
import play.api.mvc.Action

trait DeletableNodes[NODE <: UuidNode] extends NodesBase {
  protected val nodeSchema: NodeSchema[NODE]

  private def deleteResult(deleted: Boolean) = if (deleted)
      Ok(JsObject(Seq()))
    else
      BadRequest("Cannot delete Node")

  override def destroy(uuid: String) = SecuredAction(WithRole(God)) { request =>
      getResult(nodeSchema.op.delete(uuid))(deleteResult)
  }

  override def disconnectMember(path: String, uuid: String, otherUuid: String) = Action {
    val baseNode = nodeSchema.op.toNodeDefinition(uuid)
    getSchema(nodeSchema.connectSchemas, path)(connectSchema => {
      getResult(connectSchema.op.delete(baseNode, otherUuid))(deleteResult)
    })
  }

  override def disconnectNestedMember(path: String, nestedPath: String, uuid: String, otherUuid: String, nestedUuid: String) = Action {
    val baseNode = nodeSchema.op.toNodeDefinition(uuid)
    getHyperSchema(nodeSchema.connectSchemas, path)({
      case c@StartHyperConnectSchema(factory,op,connectSchemas) =>
        val hyperRel = c.toNodeDefinition(baseNode, otherUuid)
        getSchema(connectSchemas, nestedPath)(schema =>
          getResult(schema.op.deleteHyper(hyperRel, nestedUuid))(deleteResult)
        )
      case c@EndHyperConnectSchema(factory,op,connectSchemas) =>
        val hyperRel = c.toNodeDefinition(baseNode, otherUuid)
        getSchema(connectSchemas, nestedPath)(schema =>
          getResult(schema.op.deleteHyper(hyperRel, nestedUuid))(deleteResult)
        )
    })
  }
}
