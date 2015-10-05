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
import formatters.json.UserFormat.KarmaLogFormat
import modules.requests.ConnectResponse

case class NodeRegister(nodes: List[String])

case class ConnectableUpdate(connectable: Connectable)
case class ConnectableDelete(connUuid: String)
case class ConnectsAdd[NODE <: Connectable](baseUuid: String, response: ConnectResponse[NODE])
case class DashboardUpdate()
case class KarmaUpdate(karmaLogs: Seq[KarmaLog])

case class OutEvent(kind: String, data: JsValue)

//TODO: don't send updates to request initiator
// only users can change something (initiate an event), so we could get the current user from the request, when opening the websocket and then pass the current user to event actions
object LiveWebSocket {
  implicit val nodeRegisterFormat = Json.format[NodeRegister]
  implicit val outEventFormat = Json.format[OutEvent]

  implicit val nodeRegisterFrameFormatter = FrameFormatter.jsonFrame[NodeRegister]
  implicit val outEventFrameFormatter = FrameFormatter.jsonFrame[OutEvent]

  val registerNodesActor = Akka.system.actorOf(Props[RegisterNodesActor], "RegisterNodesActor")
  val registerUsersActor = Akka.system.actorOf(Props[RegisterUsersActor], "RegisterUsersActor")

  def nodeSocket() = WebSocket.acceptWithActor[NodeRegister, OutEvent] { request => out =>
    NodeWebSocketActor.props(out, registerNodesActor)
  }

  //TODO: unidirectional? no input type
  def userSocket(userId: String) = WebSocket.acceptWithActor[String, OutEvent] { request => out =>
    UserWebSocketActor.props(out, registerUsersActor, userId)
  }

  def sendConnectableUpdate(connectable: Connectable) = {
    registerNodesActor ! ConnectableUpdate(connectable)
    registerUsersActor ! DashboardUpdate()
  }

  def sendPostAdd(post: Post) = {
    registerUsersActor ! DashboardUpdate()
  }

  def sendConnectableDelete(connUuid: String) = {
    registerNodesActor ! ConnectableDelete(connUuid)
  }

  def sendConnectsAdd[NODE <: Connectable](baseUuid: String, response: ConnectResponse[NODE]) = {
    registerNodesActor ! ConnectsAdd(baseUuid, response)
    registerUsersActor ! DashboardUpdate()
  }

  def sendKarmaUpdate(karmaLogs: Seq[KarmaLog]) = {
    registerUsersActor ! KarmaUpdate(karmaLogs)
  }
}

trait DispatchActor extends Actor {
  import scala.collection.mutable

  protected val registeredListeners: mutable.Map[String, mutable.HashSet[ActorRef]] = mutable.HashMap.empty

  def registerListener(nodeUuid: String, targets: ActorRef*) = {
    val set = registeredListeners.get(nodeUuid).getOrElse {
      val set = mutable.HashSet.empty[ActorRef]
      registeredListeners += nodeUuid -> set
      set
    }
    set ++= targets
  }

  override def receive = {
    case NodeRegister(nodes) =>
      println("Register nodes received: " + nodes)
      registeredListeners.values.foreach ( _ -= sender )
      nodes.foreach(registerListener(_, sender))
  }
}

class RegisterNodesActor extends DispatchActor {
  override def receive = super.receive orElse {
    case ConnectableUpdate(post) =>
      println("Got conncetable update: " + post.uuid)
      registeredListeners.get(post.uuid).foreach(_.foreach( _ ! OutEvent("edit", Json.toJson(post)) ))
    case ConnectableDelete(connUuid) =>
      println("Got connectable delete: " + connUuid)
      registeredListeners.get(connUuid).foreach(_.foreach( _ ! OutEvent("delete", JsString(connUuid)) ))
    case ConnectsAdd(baseUuid, response) =>
      println("Got connects add on " + baseUuid)
      registeredListeners.get(baseUuid).foreach { targets =>
        response.graph.connectables.foreach(n => registerListener(n.uuid, targets.toSeq: _*))
        targets.foreach( _ ! OutEvent("connects", Json.toJson(response)))
      }
  }
}

object NodeWebSocketActor {
  def props(out: ActorRef, registerNodesActor: ActorRef) = Props(new NodeWebSocketActor(out, registerNodesActor))
}

class NodeWebSocketActor(out: ActorRef, registerNodesActor: ActorRef) extends Actor {
  override def receive = {
    case nodeRegister: NodeRegister =>
      registerNodesActor ! nodeRegister
    case outEvent: OutEvent =>
      out ! outEvent
  }

  override def postStop() = {
    registerNodesActor ! NodeRegister(List.empty)
    println("stopped node websocket!");
  }
}

class RegisterUsersActor extends DispatchActor {
  override def receive = super.receive orElse {
    case KarmaUpdate(karmaLogs) =>
      println("Got karma update " + karmaLogs)
      karmaLogs.groupBy(_.startNodeOpt.get).foreach { case (user, karmaLogs) =>
        registeredListeners.get(user.uuid).foreach(_.foreach { target =>
          target ! OutEvent("karmalog", Json.toJson(karmaLogs))
        })
      }
    case DashboardUpdate() =>
      println("Got dashboard update trigger")
      registeredListeners.values.flatten.foreach( _ ! OutEvent("dashboard", JsBoolean(true)))
  }
}

object UserWebSocketActor {
  def props(out: ActorRef, registerUsersActor: ActorRef, userId: String) = Props(new UserWebSocketActor(out, registerUsersActor, userId))
}

class UserWebSocketActor(out: ActorRef, registerUsersActor: ActorRef, userId: String) extends Actor {
  override def receive = {
    case outEvent: OutEvent =>
      out ! outEvent
  }

  override def preStart() = {
    registerUsersActor ! NodeRegister(List(userId))
  }

  override def postStop() = {
    registerUsersActor ! NodeRegister(List.empty)
    println("stopped user websocket!");
  }
}
