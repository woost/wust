package live

import org.atmosphere.play.AtmosphereCoordinator.{instance => atmosphere}
import play.api.libs.json._

import model.WustSchema._
import renesca.schema._
import modules.json.GraphFormat._
import modules.requests._
import controllers.Application

object Broadcaster {
  private def jsonChange(changeType: String, data: JsValue) = JsObject(Seq(
    ("type", JsString(changeType)),
    ("data", data)
  ))

  private def broadcast(apiname: String, uuid: String, data: JsValue, path: String = ""): Unit = {
    val postfix = if (path.isEmpty) "" else "/" + path
    val broadcaster = atmosphere.framework.metaBroadcaster
    broadcaster.broadcastTo(s"${Application.apiDefinition.websocketRoot}/$apiname/$uuid$postfix", data)
  }

  private def connectionDistributor[START <: ContentNode, RELATION <: SchemaAbstractRelation[START,END] with SchemaItem, END <: ContentNode](startHandler: (String,String) => Unit, factory: SchemaAbstractRelationFactory[START,RELATION,END], endHandler: (String,String) => Unit) {
    Application.nodeSchemas.foreach(nodeSchema => {
      nodeSchema.connectSchemas.foreach {
        case (k, StartConnection(f)) if factory == f => startHandler(nodeSchema.path, k)
        case (k, EndConnection(f)) if factory == f => endHandler(nodeSchema.path, k)
        case _ =>
      }
    })
  }

  def broadcastEdit(apiname: String, node: ContentNode): Unit = {
    //TODO: broadcast to neighbors
    broadcast(apiname, node.uuid, jsonChange("edit", Json.toJson(node)))
  }

  def broadcastConnect[START <: ContentNode, RELATION <: SchemaAbstractRelation[START,END] with SchemaItem, END <: ContentNode](startNode: START, factory: SchemaAbstractRelationFactory[START,RELATION,END], endNode: END): Unit = {
    connectionDistributor(
      (apiname, path) => broadcast(apiname, startNode.uuid, jsonChange("connect", Json.toJson(endNode)), path),
      factory,
      (apiname, path) => broadcast(apiname, endNode.uuid, jsonChange("connect", Json.toJson(startNode)), path)
    )
  }

  def broadcastDisconnect[START <: ContentNode, RELATION <: SchemaAbstractRelation[START,END] with SchemaItem, END <: ContentNode](startUuid: String, factory: SchemaAbstractRelationFactory[START,RELATION,END], endUuid: String): Unit = {
    connectionDistributor(
      (apiname, path) => broadcast(apiname, startUuid, jsonChange("disconnect", JsObject(Seq(("id", JsString(endUuid))))), path),
      factory,
      (apiname, path) => broadcast(apiname, endUuid, jsonChange("disconnect", JsObject(Seq(("id", JsString(startUuid))))), path)
    )
  }
}
