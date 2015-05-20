package formatters.json

import modules.requests._
import play.api.libs.json._

object RequestFormat {
  implicit object NodeAddFormat extends Format[NodeAddRequest] {
    def reads(json: JsValue) = json match {
      case JsObject(_) => {
        JsSuccess(NodeAddRequest((json \ "title").as[String], (json \ "description").as[Option[String]]))
      }
      case otherwise   => JsError()
    }

    def writes(nodeAdd: NodeAddRequest) = ???
  }

  implicit object ConnectFormat extends Format[ConnectRequest] {
    def reads(json: JsValue) = json match {
      case JsObject(_) => {
        JsSuccess(ConnectRequest((json \ "id").as[String]))
      }
      case otherwise   => JsError()
    }

    def writes(connect: ConnectRequest) = ???
  }
}
