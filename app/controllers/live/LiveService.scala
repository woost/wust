package controllers.live

import play.api.libs.json._
import play.api.mvc._
import play.api.Play.current
import play.api.libs.concurrent.Akka
import akka.actor._
import play.api.mvc.WebSocket.FrameFormatter
import model.WustSchema._
import formatters.json.GraphFormat.ConnectableFormat
import formatters.json.ResponseFormat._
import modules.requests.ConnectResponse

case class NodeRegister(nodes: List[String])

case class ConnectableUpdate(connectable: Connectable)
case class ConnectableDelete(connUuid: String)
case class ConnectsAdd[NODE <: Connectable](baseUuid: String, response: ConnectResponse[NODE])

case class OutEvent(kind: String, data: JsValue)

//TODO: don't send updates to request initiator
// only users can change something (initiate an event), so we could get the current user from the request, when opening the websocket and then pass the current user to event actions
object LiveWebSocket {
  implicit val inEventFormat = Json.format[NodeRegister]
  implicit val outEventFormat = Json.format[OutEvent]

  implicit val inEventFrameFormatter = FrameFormatter.jsonFrame[NodeRegister]
  implicit val outEventFrameFormatter = FrameFormatter.jsonFrame[OutEvent]

  val registerNodesActor = Akka.system.actorOf(Props[RegisterNodesActor], "RegisterNodesActor")

  def socket() = WebSocket.acceptWithActor[NodeRegister, OutEvent] { request => out =>
    LiveWebSocketActor.props(out, registerNodesActor)
  }

  def sendConnectableUpdate(connectable: Connectable) = {
    registerNodesActor ! ConnectableUpdate(connectable)
  }

  def sendConnectableDelete(connUuid: String) = {
    registerNodesActor ! ConnectableDelete(connUuid)
  }

  def sendConnectsAdd[NODE <: Connectable](baseUuid: String, response: ConnectResponse[NODE]) = {
    registerNodesActor ! ConnectsAdd(baseUuid, response)
  }
}

class RegisterNodesActor extends Actor {
  import scala.collection.mutable

  private val registeredNodes: mutable.Map[String, mutable.HashSet[ActorRef]] = mutable.HashMap.empty

  private def registerNode(nodeUuid: String, targets: ActorRef*) = {
    val set = registeredNodes.get(nodeUuid).getOrElse {
      val set = mutable.HashSet.empty[ActorRef]
      registeredNodes += nodeUuid -> set
      set
    }
    set ++= targets
  }

  def receive = {
    case NodeRegister(nodes) =>
      println("Register nodes received: " + nodes)
      registeredNodes.values.foreach ( _ -= sender )
      nodes.foreach(registerNode(_, sender))
    case ConnectableUpdate(post) =>
      println("Got conncetable update: " + post.uuid)
      registeredNodes.get(post.uuid).foreach(_.foreach( _ ! OutEvent("edit", Json.toJson(post)) ))
    case ConnectableDelete(connUuid) =>
      println("Got connectable delete: " + connUuid)
      registeredNodes.get(connUuid).foreach(_.foreach( _ ! OutEvent("delete", JsString(connUuid)) ))
    case ConnectsAdd(baseUuid, response) =>
      println("Got connects add on " + baseUuid)
      registeredNodes.get(baseUuid).foreach { targets =>
        response.graph.connectables.foreach(n => registerNode(n.uuid, targets.toSeq: _*))
        targets.foreach( _ ! OutEvent("connects", Json.toJson(response)))
      }
  }
}

object LiveWebSocketActor {
  def props(out: ActorRef, registerNodesActor: ActorRef) = Props(new LiveWebSocketActor(out, registerNodesActor))
}

class LiveWebSocketActor(out: ActorRef, registerNodesActor: ActorRef) extends Actor {
  def receive = {
    case nodeRegister: NodeRegister =>
      registerNodesActor ! nodeRegister
    case outEvent: OutEvent =>
      out ! outEvent
  }

  override def postStop() = {
    registerNodesActor ! NodeRegister(List.empty)
    println("stopped websocket!");
  }
}
