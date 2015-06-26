package modules.live

import controllers.Application
import formatters.json.ApiNodeFormat._
import model.WustSchema._
import modules.db.types._
import modules.db.{NodeDefinition, RelationDefinition, FactoryUuidNodeDefinition, UuidNodeDefinition}
import modules.requests._
import org.atmosphere.play.AtmosphereCoordinator.{instance => atmosphere}
import play.api.libs.json._
import renesca.schema._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

// TODO: rewrite!!
// TODO: do not use nodedef/relationdef, we dont need them anymore, except for
// seldom cases...
// TODO: we want graph updates, not list updates (keep list updates anyhow?)
object Broadcaster {
  private def jsonChange(changeType: String, data: JsValue, reference: String = "") = JsObject(Seq(
    ("type", JsString(changeType)),
    ("data", data),
    ("reference", JsString(reference))
  ))

  private def broadcast(path: String, data: JsValue): Unit = {
    val broadcaster = atmosphere.framework.metaBroadcaster
    println(s"Broadcast to -> ${ Application.apiDefinition.websocketRoot }/$path")
    broadcaster.broadcastTo(s"${ Application.apiDefinition.websocketRoot }/$path", data)
  }

  private def optionalFactory(definition: NodeDefinition[_]) = definition match {
    case FactoryUuidNodeDefinition(factory, _) => Some(factory)
    case _ => None
  }

  private def connectionDistributor[START <: UuidNode, END <: UuidNode](startHandler: (String, String) => Unit, definition: NodeRelationDefinition[START,_,END], endHandler: (String, String) => Unit) { import definition._
    val startFactory = optionalFactory(startDefinition)
    val endFactory = optionalFactory(endDefinition)
    Application.nodeSchemas.foreach(nodeSchema => {
      nodeSchema.connectSchemas.foreach {
        case (k, StartConnection(op)) if op.acceptsUpdateFrom(factory, endFactory)  =>
          startHandler(nodeSchema.path, k)
        case (k, EndConnection(op)) if op.acceptsUpdateFrom(factory, startFactory)  =>
          endHandler(nodeSchema.path, k)
        case _                                                                      =>
      }
    })
  }

  private def hyperConnectionDistributor(startHandler: (String, String, String) => Unit, definition: UuidHyperNodeDefinitionBase[_], nestedRelFactory: AbstractRelationFactory[_, _, _], nestedFactory: NodeFactory[_], endHandler: (String, String, String) => Unit): Unit = { import definition._
    val startFactory = optionalFactory(startDefinition)
    val endFactory = optionalFactory(endDefinition)
    Application.nodeSchemas.foreach(nodeSchema => {
      nodeSchema.connectSchemas.foreach {
        case (ok, StartHyperConnectSchema(f,op,connectSchemas)) if op.acceptsUpdateFrom(factory, endFactory) => connectSchemas.foreach {
          case (k, v) if v.op.acceptsUpdateFrom(nestedRelFactory, Some(nestedFactory)) =>
            startHandler(nodeSchema.path, ok, k)
          case _                                                                 =>
        }
        case (ok, EndHyperConnectSchema(f,op,connectSchemas)) if op.acceptsUpdateFrom(factory, startFactory) => connectSchemas.foreach {
          case (k, v) if v.op.acceptsUpdateFrom(nestedRelFactory, Some(nestedFactory)) =>
            endHandler(nodeSchema.path, ok, k)
          case _                                                                 =>
        }
        case _                                                                                               =>
      }
    })
  }

  def broadcastCreate[NODE <: UuidNode](factory: NodeFactory[NODE], node: NODE): Unit = {
    Future {
      Application.nodeSchemas.filter(_.op.acceptsUpdateFrom(factory)).foreach(nodeSchema => {
        broadcast(s"${nodeSchema.path}", jsonChange("create", Json.toJson(node)))
      })
    }
  }

  def broadcastEdit[NODE <: UuidNode](factory: NodeFactory[NODE], node: NODE): Unit = {
    Future {
      //TODO: broadcast to neighbors
      Application.nodeSchemas.filter(_.op.acceptsUpdateFrom(factory)).foreach(nodeSchema => {
        broadcast(s"${nodeSchema.path}/${node.uuid}", jsonChange("edit", Json.toJson(node)))
      })
    }
  }

  def broadcastDelete[NODE <: UuidNode](factory: NodeFactory[NODE], uuid: String): Unit = {
    Future {
      //TODO: broadcast to neighbors
      //TODO: should this broadcast on /nodes/:id or /nodes with id as payload?
      Application.nodeSchemas.filter(_.op.acceptsUpdateFrom(factory)).foreach(nodeSchema => {
        broadcast(s"${nodeSchema.path}", jsonChange("delete", Json.toJson(JsObject(Seq(("id", JsString(uuid)))))))
      })
    }
  }

  def broadcastConnect[START <: UuidNode, RELATION <: AbstractRelation[START,END], END <: UuidNode](start: START, relationDefinition: NodeRelationDefinition[START,RELATION,END], end: END) { import relationDefinition._
    Future {
      connectionDistributor(
        (apiname, path) => broadcast(s"$apiname/${ start.uuid }/$path", jsonChange("connect", Json.toJson(end))),
        relationDefinition,
        (apiname, path) => broadcast(s"$apiname/${ end.uuid }/$path", jsonChange("connect", Json.toJson(start)))
      )
    }
  }

  def broadcastDisconnect[START <: UuidNode, RELATION <: AbstractRelation[START,END], END <: UuidNode](relationDefinition: UuidRelationDefinition[START, RELATION, END]): Unit = { import relationDefinition._
    Future {
      connectionDistributor(
        (apiname, path) => broadcast(s"$apiname/${ startDefinition.uuid }/$path", jsonChange("disconnect", JsObject(Seq(("id", JsString(endDefinition.uuid)))))),
        relationDefinition,
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
