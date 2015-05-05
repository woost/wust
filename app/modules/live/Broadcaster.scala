package modules.live

import org.atmosphere.play.AtmosphereCoordinator.{instance => atmosphere}
import play.api.libs.json._

import model.WustSchema._
import renesca.schema._
import formatters.json.GraphFormat._
import modules.requests._
import controllers.Application

object Broadcaster {
  private def jsonChange(changeType: String, data: JsValue, reference: String = "") = JsObject(Seq(
    ("type", JsString(changeType)),
    ("data", data),
    ("reference", JsString(reference))
  ))

  private def broadcast(path: String, data: JsValue): Unit = {
    val broadcaster = atmosphere.framework.metaBroadcaster
    broadcaster.broadcastTo(s"${ Application.apiDefinition.websocketRoot }/$path", data)
  }

  private def connectionDistributor[START <: ContentNode, RELATION <: AbstractRelation[START, END], END <: ContentNode](startHandler: (String, String) => Unit, factory: AbstractRelationFactory[START, RELATION, END], endHandler: (String, String) => Unit) {
    Application.nodeSchemas.foreach(nodeSchema => {
      nodeSchema.connectSchemas.foreach {
        case (k, StartConnection(f)) if factory == f => startHandler(nodeSchema.path, k)
        case (k, EndConnection(f)) if factory == f   => endHandler(nodeSchema.path, k)
        case _                                       =>
      }
    })
  }

  //TODO does not assure correct combination of nested relation factories...
  private def hyperConnectionDistributor[START <: ContentNode, RELATION <: AbstractRelation[START, END] with Node, END <: ContentNode, NESTEDSTART <: Node, NESTEDREL <: AbstractRelation[NESTEDSTART, NESTEDEND], NESTEDEND <: Node](startHandler: (String, String, String) => Unit, factory: AbstractRelationFactory[START, RELATION, END], nestedRelFactory: AbstractRelationFactory[NESTEDSTART, NESTEDREL, NESTEDEND], endHandler: (String, String, String) => Unit): Unit = {
    Application.nodeSchemas.foreach(nodeSchema => {
      nodeSchema.connectSchemas.foreach {
        //TODO code duplication
        case (ok, StartHyperConnectSchema(of, connectSchemas)) if factory == of => connectSchemas.foreach {
          case (k, StartConnectSchema(f)) if nestedRelFactory == f => startHandler(nodeSchema.path, ok, k)
          case (k, EndConnectSchema(f)) if nestedRelFactory == f   => startHandler(nodeSchema.path, ok, k)
          case _                                                   =>
        }
        case (ok, EndHyperConnectSchema(of, connectSchemas)) if factory == of   => connectSchemas.foreach {
          case (k, StartConnectSchema(f)) if nestedRelFactory == f => endHandler(nodeSchema.path, ok, k)
          case (k, EndConnectSchema(f)) if nestedRelFactory == f   => endHandler(nodeSchema.path, ok, k)
          case _                                                   =>
        }
        case _                                                                  =>
      }
    })
  }

  def broadcastEdit(apiname: String, node: ContentNode): Unit = {
    //TODO: broadcast to neighbors
    broadcast(s"$apiname/${ node.uuid }", jsonChange("edit", Json.toJson(node)))
  }

  def broadcastConnect[START <: ContentNode, END <: ContentNode](startNode: START, factory: AbstractRelationFactoryStartEnd[START, END], endNode: END): Unit = {
    connectionDistributor(
      (apiname, path) => broadcast(s"$apiname/${ startNode.uuid }/$path", jsonChange("connect", Json.toJson(endNode))),
      factory,
      (apiname, path) => broadcast(s"$apiname/${ endNode.uuid }/$path", jsonChange("connect", Json.toJson(startNode)))
    )
  }

  def broadcastDisconnect[START <: ContentNode, END <: ContentNode](startUuid: String, factory: AbstractRelationFactoryStartEnd[START, END], endUuid: String): Unit = {
    connectionDistributor(
      (apiname, path) => broadcast(s"$apiname/$startUuid/$path", jsonChange("disconnect", JsObject(Seq(("id", JsString(endUuid)))))),
      factory,
      (apiname, path) => broadcast(s"$apiname/$endUuid/$path", jsonChange("disconnect", JsObject(Seq(("id", JsString(startUuid))))))
    )
  }

  //TODO signature should be SchemaHyperRelation -> see RequestSchema
  def broadcastHyperConnect[START <: ContentNode, RELATION <: AbstractRelation[START, END] with Node, END <: ContentNode, NESTEDSTART <: Node, NESTEDREL <: AbstractRelation[NESTEDSTART, NESTEDEND], NESTEDEND <: Node, NESTEDNODE <: ContentNode](startUuid: String, factory: AbstractRelationFactory[START, RELATION, END], endUuid: String, nestedRelFactory: AbstractRelationFactory[NESTEDSTART, NESTEDREL, NESTEDEND], nestedNode: NESTEDNODE): Unit = {
    hyperConnectionDistributor(
      (apiname, path, nestedPath) => broadcast(s"$apiname/$startUuid/$path/$nestedPath", jsonChange("connect", Json.toJson(nestedNode), endUuid)),
      factory,
      nestedRelFactory,
      (apiname, path, nestedPath) => broadcast(s"$apiname/$endUuid/$path/$nestedPath", jsonChange("connect", Json.toJson(nestedNode), startUuid))
    )
  }

  def broadcastHyperDisconnect[START <: ContentNode, RELATION <: AbstractRelation[START, END] with Node, END <: ContentNode, NESTEDSTART <: Node, NESTEDREL <: AbstractRelation[NESTEDSTART, NESTEDEND], NESTEDEND <: Node, NESTEDNODE <: Node](startUuid: String, factory: AbstractRelationFactory[START, RELATION, END], endUuid: String, nestedRelFactory: AbstractRelationFactory[NESTEDSTART, NESTEDREL, NESTEDEND], nestedUuid: String): Unit = {
    hyperConnectionDistributor(
      (apiname, path, nestedPath) => broadcast(s"$apiname/$startUuid/$path/$nestedPath", jsonChange("disconnect", JsObject(Seq(("id", JsString(nestedUuid)))), endUuid)),
      factory,
      nestedRelFactory,
      (apiname, path, nestedPath) => broadcast(s"$apiname/$endUuid/$path/$nestedPath", jsonChange("disconnect", JsObject(Seq(("id", JsString(nestedUuid)))), startUuid))
    )
  }
}
