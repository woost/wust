package controllers.live

import play.api.libs.json._
import play.api.mvc._
import play.api.Play.current
import play.api.libs.concurrent.Akka
import akka.actor._
import play.api.mvc.WebSocket.FrameFormatter
import model.WustSchema._
import formatters.json.PostFormat.PostFormat

case class NodeRegister(nodes: List[String])

case class PostUpdate(post: Post)
case class PostDelete(postUuid: String)

case class OutEvent(kind: String, data: JsValue)

//TODO: don't send updates to request initiator
object LiveWebSocket {
  implicit val inEventFormat = Json.format[NodeRegister]
  implicit val outEventFormat = Json.format[OutEvent]

  implicit val inEventFrameFormatter = FrameFormatter.jsonFrame[NodeRegister]
  implicit val outEventFrameFormatter = FrameFormatter.jsonFrame[OutEvent]

  val registerNodesActor = Akka.system.actorOf(Props[RegisterNodesActor], "RegisterNodesActor")

  def socket() = WebSocket.acceptWithActor[NodeRegister, OutEvent] { request => out =>
    LiveWebSocketActor.props(out, registerNodesActor)
  }

  def sendPostUpdate(post: Post) = {
    registerNodesActor ! PostUpdate(post)
  }

  def sendPostDelete(postUuid: String) = {
    registerNodesActor ! PostDelete(postUuid)
  }
}

class RegisterNodesActor extends Actor {
  import scala.collection.mutable
  val registeredNodes: mutable.Map[String, mutable.HashSet[ActorRef]] = mutable.HashMap.empty

  def receive = {
    case NodeRegister(nodes) =>
      println("Register nodes received: " + nodes)
      registeredNodes.values.foreach ( _ -= sender )
      nodes.foreach { node =>
        val set = registeredNodes.get(node).getOrElse {
          val set = mutable.HashSet.empty[ActorRef]
          registeredNodes += node -> set
          set
        }
        set += sender
      }
    case PostUpdate(post) =>
      println("Got post update: " + post)
      registeredNodes.get(post.uuid).foreach(_.foreach( _ ! OutEvent("edit", Json.toJson(post)) ))
    case PostDelete(postUuid) =>
      println("Got post delete: " + postUuid)
      registeredNodes.get(postUuid).foreach(_.foreach( _ ! OutEvent("delete", JsString(postUuid)) ))
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
    registerNodesActor ! NodeRegister(nodes = List.empty)
    println("stopped websocket!");
  }
}
