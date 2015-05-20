package modules.live

import controllers.Application
import formatters.json.GraphFormat._
import model.WustSchema._
import modules.db.types._
import modules.db.{FactoryUuidNodeDefinition, UuidNodeDefinition}
import modules.requests._
import org.atmosphere.play.AtmosphereCoordinator.{instance => atmosphere}
import play.api.libs.json._
import renesca.schema._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Broadcaster {
  private def jsonChange(changeType: String, data: JsValue, reference: String = "") = JsObject(Seq(
    ("type", JsString(changeType)),
    ("data", data),
    ("reference", JsString(reference))
  ))

  private def broadcast(path: String, data: JsValue): Unit = {
    val broadcaster = atmosphere.framework.metaBroadcaster
    println(s"${ Application.apiDefinition.websocketRoot }/$path")
    broadcaster.broadcastTo(s"${ Application.apiDefinition.websocketRoot }/$path", data)
  }

  private def connectionDistributor[START <: UuidNode, END <: UuidNode](startHandler: (String, String) => Unit, startFactory: NodeFactory[START], factory: AbstractRelationFactory[START,_,END], endFactory: NodeFactory[END], endHandler: (String, String) => Unit) {
    Application.nodeSchemas.foreach(nodeSchema => {
      nodeSchema.connectSchemas.foreach {
        case (k, StartConnection(op)) if op.acceptsUpdateFrom(factory, Some(endFactory))  =>
          startHandler(nodeSchema.path, k)
        case (k, EndConnection(op)) if op.acceptsUpdateFrom(factory, Some(startFactory))  =>
          endHandler(nodeSchema.path, k)
        case _                                                                      =>
      }
    })
  }

  private def optionalFactory(definition: UuidNodeDefinition[_]) = definition match {
    case FactoryUuidNodeDefinition(factory, _) => Some(factory)
    case _ => None
  }

  private def hyperConnectionDistributor(startHandler: (String, String, String) => Unit, definition: UuidHyperNodeDefinitionBase[_], nestedRelFactory: AbstractRelationFactory[_, _, _], nestedFactory: NodeFactory[_], endHandler: (String, String, String) => Unit): Unit = {
    val startFactory = optionalFactory(definition.startDefinition)
    val endFactory = optionalFactory(definition.endDefinition)
    Application.nodeSchemas.foreach(nodeSchema => {
      nodeSchema.connectSchemas.foreach {
        case (ok, StartHyperConnectSchema(f,op,connectSchemas)) if op.acceptsUpdateFrom(definition.factory, endFactory) => connectSchemas.foreach {
          case (k, v) if v.op.acceptsUpdateFrom(nestedRelFactory, Some(nestedFactory)) =>
            startHandler(nodeSchema.path, ok, k)
          case _                                                                 =>
        }
        case (ok, EndHyperConnectSchema(f,op,connectSchemas)) if op.acceptsUpdateFrom(definition.factory, startFactory) => connectSchemas.foreach {
          case (k, v) if v.op.acceptsUpdateFrom(nestedRelFactory, Some(nestedFactory)) =>
            endHandler(nodeSchema.path, ok, k)
          case _                                                                 =>
        }
        case _                                                                                               =>
      }
    })
  }

  def broadcastEdit[NODE <: UuidNode](factory: NodeFactory[NODE], node: NODE): Unit = {
    Future {
      //TODO: broadcast to neighbors
      Application.nodeSchemas.filter(_.op.factory == factory).foreach(nodeSchema => {
        broadcast(s"${ nodeSchema.path }/${ node.uuid }", jsonChange("edit", Json.toJson(node)))
      })
    }
  }

  def broadcastConnect[START <: UuidNode, RELATION <: AbstractRelation[START,END], END <: UuidNode](start: START, relationDefinition: FactoryRelationDefinition[START,RELATION,END], end: END): Unit = { import relationDefinition._
    Future {
      connectionDistributor(
        (apiname, path) => broadcast(s"$apiname/${ start.uuid }/$path", jsonChange("connect", Json.toJson(end))),
        startDefinition.factory,
        factory,
        endDefinition.factory,
        (apiname, path) => broadcast(s"$apiname/${ end.uuid }/$path", jsonChange("connect", Json.toJson(start)))
      )
    }
  }

  def broadcastDisconnect[START <: UuidNode, RELATION <: AbstractRelation[START,END], END <: UuidNode](relationDefinition: FactoryUuidRelationDefinition[START, RELATION, END]): Unit = { import relationDefinition._
    Future {
      connectionDistributor(
        (apiname, path) => broadcast(s"$apiname/${ startDefinition.uuid }/$path", jsonChange("disconnect", JsObject(Seq(("id", JsString(endDefinition.uuid)))))),
        startDefinition.factory,
        factory,
        endDefinition.factory,
        (apiname, path) => broadcast(s"$apiname/${ endDefinition.uuid }/$path", jsonChange("disconnect", JsObject(Seq(("id", JsString(startDefinition.uuid))))))
      )
    }
  }

  private def broadcastHyperConnect[NODE <: UuidNode](definition: UuidHyperNodeDefinitionBase[_], factory: AbstractRelationFactory[_,_,_], nestedNode: NODE, nestedFactory: NodeFactory[NODE]): Unit = {
    Future {
      hyperConnectionDistributor(
        (apiname, path, nestedPath) => broadcast(s"$apiname/${definition.startDefinition.uuid}/$path/$nestedPath", jsonChange("connect", Json.toJson(nestedNode), definition.endDefinition.uuid)),
        definition,
        factory,
        nestedFactory,
        (apiname, path, nestedPath) => broadcast(s"$apiname/${definition.endDefinition.uuid}/$path/$nestedPath", jsonChange("connect", Json.toJson(nestedNode), definition.startDefinition.uuid))
      )
    }
  }

  private def broadcastHyperDisconnect[NODE <: UuidNode](definition: UuidHyperNodeDefinitionBase[_], factory: AbstractRelationFactory[_,_,_], nestedDefinition: UuidNodeDefinition[NODE], nestedFactory: NodeFactory[NODE]): Unit = {
    Future {
      hyperConnectionDistributor(
        (apiname, path, nestedPath) => broadcast(s"$apiname/${ definition.startDefinition.uuid }/$path/$nestedPath", jsonChange("disconnect", JsObject(Seq(("id", JsString(nestedDefinition.uuid)))), definition.endDefinition.uuid)),
        definition,
        factory,
        nestedFactory,
        (apiname, path, nestedPath) => broadcast(s"$apiname/${ definition.endDefinition.uuid }/$path/$nestedPath", jsonChange("disconnect", JsObject(Seq(("id", JsString(nestedDefinition.uuid)))), definition.startDefinition.uuid))
      )
    }
  }

  def broadcastStartHyperConnect[START <: Node, RELATION <: AbstractRelation[START,END], END <: UuidNode](relationDefinition: UuidHyperAndNodeRelationDefinition[START,RELATION,END] with NodeAndFactoryRelationDefinition[START,RELATION,END], nestedNode: END): Unit = { import relationDefinition._
    broadcastHyperConnect(startDefinition, factory, nestedNode, endDefinition.factory)
  }

  def broadcastEndHyperConnect[START <: UuidNode, RELATION <: AbstractRelation[START,END], END <: Node](relationDefinition: NodeAndUuidHyperRelationDefinition[START,RELATION,END] with FactoryAndNodeRelationDefinition[START,RELATION,END], nestedNode: START): Unit = { import relationDefinition._
    broadcastHyperConnect(endDefinition, factory, nestedNode, startDefinition.factory)
  }

  def broadcastStartHyperDisconnect[START <: Node, RELATION <: AbstractRelation[START,END], END <: UuidNode](relationDefinition: UuidHyperAndNodeRelationDefinition[START,RELATION,END] with NodeAndFactoryUuidRelationDefinition[START,RELATION,END]): Unit = { import relationDefinition._
    broadcastHyperDisconnect(startDefinition, factory, endDefinition, endDefinition.factory)
  }

  def broadcastEndHyperDisconnect[START <: UuidNode, RELATION <: AbstractRelation[START,END], END <: Node](relationDefinition: NodeAndUuidHyperRelationDefinition[START,RELATION,END] with FactoryUuidAndNodeRelationDefinition[START,RELATION,END]): Unit = { import relationDefinition._
    broadcastHyperDisconnect(endDefinition, factory, startDefinition, startDefinition.factory)
  }
}
