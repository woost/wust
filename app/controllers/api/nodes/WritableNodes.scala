package controllers.api.nodes

import formatters.json.DiscourseNodeFormat._
import model.WustSchema._
import modules.requests._
import play.api.libs.json.Json
import play.api.mvc.Action

trait WritableNodes[NODE <: UuidNode] extends NodesBase {
  //TODO: use transactions instead of db
  protected val nodeSchema: NodeSchema[NODE]

  private def jsonNode(node: UuidNode) = Ok(Json.toJson(node))
  private def unauthorized = Unauthorized("Only users who are logged in can create Nodes")

  override def create = UserAwareAction { request =>
    request.identity match {
      case Some(user) =>
        val nodeAdd = request.body.asJson.get
        // TODO: HTTP status Created
        getResult(nodeSchema.op.create(user, nodeAdd))(jsonNode)
      case None => unauthorized
    }
  }

  override def update(uuid: String) = UserAwareAction { request =>
    request.identity match {
      case Some(user) =>
        val nodeAdd = request.body.asJson.get
        getResult(nodeSchema.op.update(uuid, user, nodeAdd))(jsonNode)
      case None => unauthorized
    }
  }

  override def connectMember(path: String, uuid: String) = Action(parse.json) { request =>
    val connect = request.body
    val baseNode = nodeSchema.op.toNodeDefinition(uuid)
    getSchema(nodeSchema.connectSchemas, path)(connectSchema => {
      getResult(connectSchema.op.create(baseNode, connect))(jsonNode)
    })
  }

  override def connectNestedMember(path: String, nestedPath: String, uuid: String, otherUuid: String) = Action(parse.json) { request =>
    val connect = request.body
    val baseNode = nodeSchema.op.toNodeDefinition(uuid)
    getHyperSchema(nodeSchema.connectSchemas, path)({
      case c@StartHyperConnectSchema(_,_,connectSchemas)  =>
        val hyperRel = c.toNodeDefinition(baseNode, otherUuid)
        getSchema(connectSchemas, nestedPath)(schema =>
          getResult(schema.op.createHyper(hyperRel, connect))(jsonNode)
        )
      case c@EndHyperConnectSchema(_, _, connectSchemas) =>
        val hyperRel = c.toNodeDefinition(baseNode, otherUuid)
        getSchema(connectSchemas, nestedPath)(schema =>
          getResult(schema.op.createHyper(hyperRel, connect))(jsonNode)
        )
    })
  }
}
