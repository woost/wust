package modules.live

import controllers.Application
import formatters.json.GraphFormat._
import model.WustSchema._
import modules.db.UuidNodeDefinition
import modules.db.types._
import modules.requests._
import org.atmosphere.play.AtmosphereCoordinator.{instance => atmosphere}
import play.api.libs.json._
import renesca.schema._

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

  private def connectionDistributor(startHandler: (String, String) => Unit, factory: AbstractRelationFactory[_, _, _], endHandler: (String, String) => Unit) {
    Application.nodeSchemas.foreach(nodeSchema => {
      nodeSchema.connectSchemas.foreach {
        //TODO: check for same nodefactory
        case (k, StartConnection(op)) if factory == op.factory => startHandler(nodeSchema.path, k)
        case (k, EndConnection(op)) if factory == op.factory   => endHandler(nodeSchema.path, k)
        case _                                                 =>
      }
    })
  }

  //TODO does not assure correct combination of nested relation factories...
  private def hyperConnectionDistributor(startHandler: (String, String, String) => Unit, factory: AbstractRelationFactory[_, _, _], nestedRelFactory: AbstractRelationFactory[_, _, _], endHandler: (String, String, String) => Unit): Unit = {
    Application.nodeSchemas.foreach(nodeSchema => {
      nodeSchema.connectSchemas.foreach {
        //TODO code duplication
        case (ok, StartHyperConnectSchema(of,_,connectSchemas)) if factory == of => connectSchemas.foreach {
          case (k, StartConnection(op)) if nestedRelFactory == op.factory => startHandler(nodeSchema.path, ok, k)
          case (k, EndConnection(op)) if nestedRelFactory == op.factory   => startHandler(nodeSchema.path, ok, k)
          case _                                                          =>
        }
        case (ok, EndHyperConnectSchema(of,_,connectSchemas)) if factory == of   => connectSchemas.foreach {
          case (k, StartConnection(op)) if nestedRelFactory == op.factory => endHandler(nodeSchema.path, ok, k)
          case (k, EndConnection(op)) if nestedRelFactory == op.factory   => endHandler(nodeSchema.path, ok, k)
          case _                                                          =>
        }
        case _                                                                   =>
      }
    })
  }

  def broadcastEdit[NODE <: UuidNode](factory: NodeFactory[NODE], node: NODE): Unit = {
    //TODO: broadcast to neighbors
    println("muh")
    Application.nodeSchemas.filter(_.op.factory == factory).foreach(nodeSchema => {
      println("meh")
      broadcast(s"${nodeSchema.path}/${ node.uuid }", jsonChange("edit", Json.toJson(node)))
    })
  }

  def broadcastConnect[START <: UuidNode, RELATION <: AbstractRelation[START,END], END <: UuidNode](startNode: START, factory: AbstractRelationFactory[START, RELATION, END], endNode: END): Unit = {
    connectionDistributor(
      (apiname, path) => broadcast(s"$apiname/${ startNode.uuid }/$path", jsonChange("connect", Json.toJson(endNode))),
      factory,
      (apiname, path) => broadcast(s"$apiname/${ endNode.uuid }/$path", jsonChange("connect", Json.toJson(startNode)))
    )
  }

  def broadcastDisconnect[START <: UuidNode, RELATION <: AbstractRelation[START,END], END <: UuidNode](relationDefinition: UuidRelationDefinition[START, RELATION, END]): Unit = { import relationDefinition._
    connectionDistributor(
      (apiname, path) => broadcast(s"$apiname/${startDefinition.uuid}/$path", jsonChange("disconnect", JsObject(Seq(("id", JsString(endDefinition.uuid)))))),
      factory,
      (apiname, path) => broadcast(s"$apiname/${endDefinition.uuid}/$path", jsonChange("disconnect", JsObject(Seq(("id", JsString(startDefinition.uuid))))))
    )
  }

  private def broadcastHyperConnect[NODE <: UuidNode](definition: UuidHyperNodeDefinitionBase[_], factory: AbstractRelationFactory[_,_,_], nestedNode: NODE): Unit = {
    hyperConnectionDistributor(
      (apiname, path, nestedPath) => broadcast(s"$apiname/${definition.startDefinition.uuid}/$path/$nestedPath", jsonChange("connect", Json.toJson(nestedNode), definition.endDefinition.uuid)),
      definition.factory,
      factory,
      (apiname, path, nestedPath) => broadcast(s"$apiname/${definition.endDefinition.uuid}/$path/$nestedPath", jsonChange("connect", Json.toJson(nestedNode), definition.startDefinition.uuid))
    )
  }

  private def broadcastHyperDisconnect(definition: UuidHyperNodeDefinitionBase[_], factory: AbstractRelationFactory[_,_,_], nestedDefinition: UuidNodeDefinition[_]): Unit = {
    hyperConnectionDistributor(
      (apiname, path, nestedPath) => broadcast(s"$apiname/${definition.startDefinition.uuid}/$path/$nestedPath", jsonChange("disconnect", JsObject(Seq(("id", JsString(nestedDefinition.uuid)))), definition.endDefinition.uuid)),
      definition.factory,
      factory,
      (apiname, path, nestedPath) => broadcast(s"$apiname/${definition.endDefinition.uuid}/$path/$nestedPath", jsonChange("disconnect", JsObject(Seq(("id", JsString(nestedDefinition.uuid)))), definition.startDefinition.uuid))
    )
  }

  def broadcastStartHyperConnect[START <: Node, RELATION <: AbstractRelation[START,END], END <: UuidNode](relationDefinition: UuidHyperAndNodeRelationDefinition[START,RELATION,END], nestedNode: END): Unit = { import relationDefinition._
    broadcastHyperConnect(startDefinition, factory, nestedNode)
  }

  def broadcastEndHyperConnect[START <: UuidNode, RELATION <: AbstractRelation[START,END], END <: Node](relationDefinition: NodeAndUuidHyperRelationDefinition[START,RELATION,END], nestedNode: START): Unit = { import relationDefinition._
    broadcastHyperConnect(endDefinition, factory, nestedNode)
  }

  def broadcastStartHyperDisconnect[START <: Node, RELATION <: AbstractRelation[START,END], END <: UuidNode](relationDefinition: UuidHyperAndNodeRelationDefinition[START,RELATION,END] with NodeAndUuidRelationDefinition[START,RELATION,END]): Unit = { import relationDefinition._
    broadcastHyperDisconnect(startDefinition, factory, endDefinition)
  }

  def broadcastEndHyperDisconnect[START <: UuidNode, RELATION <: AbstractRelation[START,END], END <: Node](relationDefinition: NodeAndUuidHyperRelationDefinition[START,RELATION,END] with UuidAndNodeRelationDefinition[START,RELATION,END]): Unit = { import relationDefinition._
    broadcastHyperDisconnect(endDefinition, factory, startDefinition)
  }
}
