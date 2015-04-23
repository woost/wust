package live

import org.atmosphere.play.AtmosphereCoordinator.{instance => atmosphere}
import play.api.libs.json._

import model.WustSchema._
import modules.json.GraphFormat._

class Broadcaster(apiname: String) {
  private def jsonChange(changeType: String, data: JsValue) = JsObject(Seq(
    ("type", JsString(changeType)),
    ("data", data)
  ))

  private def broadcast(uuid: String, data: JsValue): Unit = {
    val broadcaster = atmosphere.framework.metaBroadcaster
    broadcaster.broadcastTo(s"/live/v1/$apiname/$uuid", data)
  }

  def broadcastConnect(uuid: String, node: ContentNode): Unit = {
    broadcast(uuid, jsonChange("connect", Json.toJson(node)))
  }

  def broadcastDisconnect(uuid: String, otherUuid: String, label: String): Unit = {
    broadcast(uuid,
      jsonChange("disconnect", JsObject(Seq(("id", JsString(otherUuid)), ("label", JsString(label))))))
  }
}
