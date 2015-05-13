package controllers.nodes

import model.WustSchema._
import model.auth.{God, WithRole}
import modules.db._
import modules.requests._
import play.api.libs.json._
import play.api.mvc.Action

trait DeletableNodes[NODE <: UuidNode] extends NodesBase {
  // TODO: use transactions instead of db
  def nodeSchema: NodeSchema[NODE]

  // TODO: leaks hyperedges
  // TODO: broadcast
  override def destroy(uuid: String) = SecuredAction(WithRole(God)) {
    implicit request =>
      nodeSchema.op.delete(uuid)
      Ok(JsObject(Seq()))
  }

  override def disconnectMember(path: String, uuid: String, otherUuid: String) = Action {
    val baseNode = nodeSchema.op.toNodeDefinition(uuid)
    val connectSchema = nodeSchema.connectSchemas(path)
    connectSchema.op.delete(baseNode, otherUuid)
    Ok(JsObject(Seq()))
  }

  override def disconnectNestedMember(path: String, nestedPath: String, uuid: String, otherUuid: String, nestedUuid: String) = Action {
    val connectSchema = nodeSchema.connectSchemas(path)
    val baseNode = nodeSchema.op.toNodeDefinition(uuid)
    connectSchema match {
      case c@StartHyperConnectSchema(outerFactory,op,connectSchemas) =>
        val hyperRel = c.toNodeDefinition(baseNode, otherUuid)
        val nestedConnectSchema = connectSchemas(nestedPath)
        nestedConnectSchema.op.delete(hyperRel, nestedUuid)
        Ok(JsObject(Seq()))
      case c@EndHyperConnectSchema(outerFactory,op,connectSchemas) =>
        val hyperRel = c.toNodeDefinition(baseNode, otherUuid)
        val nestedConnectSchema = connectSchemas(nestedPath)
        nestedConnectSchema.op.delete(hyperRel, nestedUuid)
        Ok(JsObject(Seq()))
      case _ => BadRequest(s"Nothing found")
    }
  }
}
