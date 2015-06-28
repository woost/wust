package controllers.api.nodes

import formatters.json.ApiNodeFormat._
import model.WustSchema._
import modules.requests._
import play.api.libs.json.Json
import play.api.mvc.Action

trait WritableNodes[NODE <: UuidNode] extends NodesBase {
  //TODO: use transactions instead of db
  def nodeSchema: NodeSchema[NODE]

  private def jsonNode(node: UuidNode) = Ok(Json.toJson(node))

  override def create = UserAwareAction(parse.json) { request =>
    getUser(request.identity)(user => {
      val nodeAdd = request.body
      // TODO: HTTP status Created
      getResult(nodeSchema.op.create(user, nodeAdd))(jsonNode)
    })
  }

  override def update(uuid: String) = UserAwareAction(parse.json) { request =>
    getUser(request.identity)(user => {
      val nodeAdd = request.body
      getResult(nodeSchema.op.update(uuid, user, nodeAdd))(jsonNode)
    })
  }

  override def connectMember(path: String, uuid: String) = UserAwareAction(parse.json) { request =>
    getUser(request.identity)(user => {
      val connect = request.body
      getSchema(nodeSchema.connectSchemas, path)(connectSchema => {
        getResult(connectSchema.op.create(uuid, user, connect))(jsonNode)
      })
    })
  }

  override def connectNestedMember(path: String, nestedPath: String, uuid: String, otherUuid: String) = UserAwareAction(parse.json) { request =>
    getUser(request.identity)(user => {
      val connect = request.body
      getNestedSchema(nodeSchema.connectSchemas, path, nestedPath)((hyper, schema) =>
          if(hyper.reverse)
            getResult(schema.op.createHyper(otherUuid, uuid, user, connect))(jsonNode)
          else
            getResult(schema.op.createHyper(uuid, otherUuid, user, connect))(jsonNode)
      )
    })
  }
}
