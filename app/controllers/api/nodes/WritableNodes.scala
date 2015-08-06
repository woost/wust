package controllers.api.nodes

import formatters.json.ApiNodeFormat._
import formatters.json.ResponseFormat.connectWrites
import model.WustSchema._
import modules.requests._
import play.api.libs.json.Json
import play.api.mvc.Action

trait WritableNodes[NODE <: UuidNode] extends NodesBase {
  //TODO: use transactions instead of db
  def nodeSchema: NodeSchema[NODE]

  private def jsonNode(node: UuidNode) = Ok(Json.toJson(node))
  private def connectResponse(response: ConnectResponse[UuidNode]) = Ok(Json.toJson(response))

  override def create = UserAwareAction { request =>
    // TODO: HTTP status Created
    getResult(nodeSchema.op.create(context(request)))(jsonNode)
  }

  override def update(uuid: String) = UserAwareAction { request =>
    getResult(nodeSchema.op.update(context(request), uuid))(jsonNode)
  }

  override def connectMember(path: String, uuid: String) = UserAwareAction { request =>
    getSchema(nodeSchema.connectSchemas, path)(connectSchema => {
      getResult(connectSchema.op.create(context(request), uuid))(connectResponse)
    })
  }

  override def connectMember(path: String, uuid: String, otherUuid: String) = UserAwareAction { request =>
    validateConnect(uuid, otherUuid)(() => {
      getSchema(nodeSchema.connectSchemas, path)(connectSchema => {
        getResult(connectSchema.op.create(context(request), uuid, otherUuid))(connectResponse)
      })
    })
  }

  override def connectNestedMember(path: String, nestedPath: String, uuid: String, otherUuid: String) = UserAwareAction { request =>
    getNestedSchema(nodeSchema.connectSchemas, path, nestedPath)((hyper, schema) =>
        if(hyper.reverse)
          getResult(schema.op.createHyper(context(request), otherUuid, uuid))(connectResponse)
        else
          getResult(schema.op.createHyper(context(request), uuid, otherUuid))(connectResponse)
    )
  }

  //TODO: forbid self loop of hyperrelations
  // at this point, we do not know whether nestedUuid = HyperRelation(uuid, otherUuid).uuid
  override def connectNestedMember(path: String, nestedPath: String, uuid: String, otherUuid: String, nestedUuid: String) = UserAwareAction { request =>
    getNestedSchema(nodeSchema.connectSchemas, path, nestedPath)((hyper, schema) =>
      if(hyper.reverse)
        getResult(schema.op.createHyper(context(request), otherUuid, uuid))(connectResponse)
      else
        getResult(schema.op.createHyper(context(request), uuid, otherUuid))(connectResponse)
    )
  }
}
