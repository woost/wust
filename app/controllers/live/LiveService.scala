package controllers.live

import play.api.libs.json._
import play.api.mvc._
import play.api.Play.current
import play.api.libs.concurrent.Akka
import akka.actor._
import play.api.mvc.WebSocket.FrameFormatter

case class PostRegister(nodes: List[String])
case class PostChanged(msg: String)

object LiveWebSocket {
  implicit val inEventFormat = Json.format[PostRegister]
  implicit val outEventFormat = Json.format[PostChanged]

  implicit val inEventFrameFormatter = FrameFormatter.jsonFrame[PostRegister]
  implicit val outEventFrameFormatter = FrameFormatter.jsonFrame[PostChanged]

  val registerPostsActor = Akka.system.actorOf(Props[RegisterPostsActor], "RegisterPostsActor")

  def socket() = WebSocket.acceptWithActor[PostRegister, PostChanged] { request => out =>
    LiveWebSocketActor.props(out, registerPostsActor)
  }

  def sendUpdate(nodeId: String) = {
    registerPostsActor ! (nodeId)
  }
}

class RegisterPostsActor extends Actor {
  import scala.collection.mutable
  val registeredNodes: mutable.Map[String, mutable.HashSet[ActorRef]] = mutable.HashMap.empty

  def receive = {
    case postReg: PostRegister =>
      println("Register posts received: " + postReg)
      registeredNodes.values.foreach ( _ -= sender )
      postReg.nodes.foreach { node =>
        val set = registeredNodes.get(node).getOrElse {
          val set = mutable.HashSet.empty[ActorRef]
          registeredNodes += node -> set
          set
        }
        set += sender
      }

      println(registeredNodes)
    case nodeId: String =>
      println(nodeId)
      println(registeredNodes)
      registeredNodes.get(nodeId).foreach(_.foreach( _ ! (PostChanged("hi"))))
  }
}

object LiveWebSocketActor {
  def props(out: ActorRef, registerPostsActor: ActorRef) = Props(new LiveWebSocketActor(out, registerPostsActor))
}

class LiveWebSocketActor(out: ActorRef, registerPostsActor: ActorRef) extends Actor {
  def receive = {
    case postReg: PostRegister =>
      println(postReg)
      registerPostsActor ! (postReg)
    //TODO: forward?
    case postChanged: PostChanged =>
      out ! postChanged
  }

  override def postStop() = {
    registerPostsActor ! PostRegister(nodes = List.empty)
    println("stopped!");
    //clean up after request is closed
  }
}
