package controllers.api.nodes

import formatters.json.ApiNodeFormat._
import formatters.json.ResponseFormat.connectWrites
import model.WustSchema._
import modules.requests._
import play.api.libs.json.Json
import play.api.mvc.Action

trait WritableNodes[NODE <: UuidNode] extends NodesBase {
  def nodeSchema: NodeSchema[NODE]

  private def jsonNode(node: UuidNode) = Ok(Json.toJson(node))
  private def createdJsonNode(node: UuidNode) = Ok(Json.toJson(node))
  private def connectResponse(response: ConnectResponse[UuidNode]) = Ok(Json.toJson(response))

  override def create = UserAwareAction { request =>
    getResult(nodeSchema.op.create(context(request)))(createdJsonNode)
  }

  override def update(uuid: String) = UserAwareAction { request =>
    getResult(nodeSchema.op.update(context(request), uuid))(jsonNode)
  }

  override def connectMember(path: String, uuid: String) = UserAwareAction { request =>
    getSchema(nodeSchema.connectSchemas, path)(connectSchema => {
      getResult(connectSchema.inv.op.create(context(request), ConnectParameter(nodeSchema.op.factory, uuid)))(connectResponse)
    })
  }

  override def connectMember(path: String, uuid: String, otherUuid: String) = UserAwareAction { request =>
    validateConnect(uuid, otherUuid)(() => {
      getSchema(nodeSchema.connectSchemas, path)(connectSchema => {
        getResult(connectSchema.inv.op.create(context(request), ConnectParameter(nodeSchema.op.factory, uuid), otherUuid))(connectResponse)
      })
    })
  }

  override def connectNestedMember(path: String, nestedPath: String, uuid: String, otherUuid: String) = UserAwareAction { request =>
    getHyperSchema(nodeSchema.connectSchemas, path)({
      case c@StartHyperConnectSchema(factory, op, connectSchemas) =>
        getSchema(connectSchemas, nestedPath)(schema =>
          getResult(schema.inv.op.create(context(request), HyperConnectParameter(nodeSchema.op.factory, uuid, c.factory, c.op.nodeFactory, otherUuid)))(connectResponse)
        )
      case c@EndHyperConnectSchema(factory, op, connectSchemas)   =>
        getSchema(connectSchemas, nestedPath)(schema =>
          getResult(schema.inv.op.create(context(request), HyperConnectParameter(c.op.nodeFactory, otherUuid, c.factory, nodeSchema.op.factory, uuid)))(connectResponse)
        )
    })
  }

  override def connectNestedMember(path: String, nestedPath: String, uuid: String, otherUuid: String, nestedUuid: String) = UserAwareAction { request =>
    validateHyperConnect(uuid, otherUuid, nestedUuid)(() => {
      getHyperSchema(nodeSchema.connectSchemas, path)({
        case c@StartHyperConnectSchema(factory, op, connectSchemas) =>
          getSchema(connectSchemas, nestedPath)(schema =>
            getResult(schema.inv.op.create(context(request), HyperConnectParameter(nodeSchema.op.factory, uuid, c.factory, c.op.nodeFactory, otherUuid), nestedUuid))(connectResponse)
          )
        case c@EndHyperConnectSchema(factory, op, connectSchemas)   =>
          getSchema(connectSchemas, nestedPath)(schema =>
            getResult(schema.inv.op.create(context(request), HyperConnectParameter(c.op.nodeFactory, otherUuid, c.factory, nodeSchema.op.factory, uuid), nestedUuid))(connectResponse)
          )
      })
    })
  }
}
