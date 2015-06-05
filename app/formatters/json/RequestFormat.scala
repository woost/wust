package formatters.json

import modules.requests._
import play.api.libs.json._

object RequestFormat {
  implicit object NodeAddFormat extends Format[NodeAddRequest] {
    def reads(json: JsValue) = json match {
      case JsObject(_) => {
        JsSuccess(NodeAddRequest((json \ "description").as[String], (json \ "title").as[Option[String]]))
      }
      case otherwise   => JsError()
    }

    def writes(nodeAdd: NodeAddRequest) = ???
  }

  implicit object TaggedNodeAddFormat extends Format[TaggedNodeAddRequest] {
    def reads(json: JsValue) = json match {
      case JsObject(_) => {
        JsSuccess(TaggedNodeAddRequest((json \ "description").as[String], (json \ "title").as[Option[String]], (json \ "addedTags").as[Option[List[String]]].getOrElse(List.empty)))
      }
      case otherwise   => JsError()
    }

    def writes(nodeAdd: TaggedNodeAddRequest) = ???
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
