package controllers.api.nodes

import formatters.json.ApiNodeFormat._
import model.WustSchema._
import modules.requests._
import play.api.libs.json.Json
import play.api.mvc.Action

trait WritableNodes[NODE <: UuidNode] extends NodesBase {
  //TODO: use transactions instead of db
  protected val nodeSchema: NodeSchema[NODE]

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
      val baseNode = nodeSchema.op.toNodeDefinition(uuid)
      getSchema(nodeSchema.connectSchemas, path)(connectSchema => {
        getResult(connectSchema.op.create(baseNode, user, connect))(jsonNode)
      })
    })
  }

  override def connectNestedMember(path: String, nestedPath: String, uuid: String, otherUuid: String) = UserAwareAction(parse.json) { request =>
    getUser(request.identity)(user => {
      val connect = request.body
      val baseNode = nodeSchema.op.toNodeDefinition(uuid)
      getHyperSchema(nodeSchema.connectSchemas, path)({
        case c@StartHyperConnectSchema(_,_,connectSchemas)  =>
          val hyperRel = c.toNodeDefinition(baseNode, otherUuid)
          getSchema(connectSchemas, nestedPath)(schema =>
            getResult(schema.op.createHyper(hyperRel, user, connect))(jsonNode)
          )
        case c@EndHyperConnectSchema(_, _, connectSchemas) =>
          val hyperRel = c.toNodeDefinition(baseNode, otherUuid)
          getSchema(connectSchemas, nestedPath)(schema =>
            getResult(schema.op.createHyper(hyperRel, user, connect))(jsonNode)
          )
      })
    })
  }
}
