package modules.live

import org.atmosphere.config.service.{ManagedService, Message, Ready}
import org.atmosphere.cpr.AtmosphereResource


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
