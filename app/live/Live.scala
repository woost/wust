package live

import javax.inject.Inject
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import org.atmosphere.config.managed.{Encoder, Decoder}
import org.atmosphere.config.service.{Disconnect, Ready, ManagedService, Message}
import org.atmosphere.cpr.{AtmosphereResourceEvent, AtmosphereResource}


@ManagedService(path = "/live")
class Live {
  @Ready
  def onReady(resource: AtmosphereResource) {
    println(s"Browser ${ resource.uuid } connected.")
  }

  @Disconnect
  def onDisconnect(event: AtmosphereResourceEvent) = {
    if(event.isCancelled) {
      println(s"Browser ${ event.getResource.uuid } disconnected.")
    } else if(event.isClosedByClient) {
      println(s"Browser ${ event.getResource.uuid } closed the connection.")
    }
  }

  //  @Message(decoders = Array(classOf[TextMessageDecoder]))
  //  def onChange(implicit resource: AtmosphereResource, textMessage: TextMessage): TextMessage = {
  //    //    resource.write(Json.toJson(textMessage.copy(text = textMessage.text + " (reply)")).toString())
  //    ???
  //  }
}

//case class TextMessage(text: String)
//object A {
//  implicit object MessageFormat extends Format[TextMessage] {
//    override def reads(json: JsValue): JsResult[TextMessage] = JsSuccess(TextMessage((json \ "text").as[String]))
//    override def writes(o: TextMessage): JsValue = JsObject(Seq("text" -> JsString(o.text)))
//  }
//}
//
//
//class TextMessageDecoder extends Decoder[String, TextMessage] {
//  override def decode(json: String) = Json.parse(json).as[TextMessage]
//}
//
//class TextMessageEncoder extends Encoder[TextMessage, String] {
//  override def encode(s: TextMessage): String = Json.toJson(s).toString
//}
