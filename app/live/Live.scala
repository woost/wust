package live

import javax.inject.Inject
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import org.atmosphere.config.managed.{Encoder, Decoder}
import org.atmosphere.config.service.{Disconnect, Ready, ManagedService, Message}
import org.atmosphere.cpr.{AtmosphereResourceEvent, AtmosphereResource}


@ManagedService(path = "/live/v1/{page:.*}")
class Live {
  @Ready
  def onReady(resource: AtmosphereResource) {
    val uuid = resource.uuid
    val url = resource.getRequest.getRequestURL.toString.takeWhile(_ != '?')
    println(s"Browser $uuid connected ($url)")
  }

  @Message
  def send(implicit resource: AtmosphereResource, msg: String) = {
    // all messages sent to this client are passed through this method
    println(s"Sending '${ msg }' to ${ resource.uuid }")
    msg
  }
}
