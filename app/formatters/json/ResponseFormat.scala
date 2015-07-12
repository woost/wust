package formatters.json

import formatters.json.GraphFormat._
import model.WustSchema.UuidNode
import modules.requests._
import play.api.libs.json._

object ResponseFormat {
  implicit def connectWrites[NODE <: UuidNode](implicit fmt: Writes[NODE]) = new Writes[ConnectResponse[NODE]] {
    def writes(resp: ConnectResponse[NODE]) = JsObject(Seq(
      "graph" -> Json.toJson(resp.graph),
      "node" -> resp.node.map(Json.toJson(_)).getOrElse(JsNull)
    ))
  }
}
