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

  implicit object NodeUpdateFormat extends Format[NodeUpdateRequest] {
    def reads(json: JsValue) = json match {
      case JsObject(_) => {
        JsSuccess(NodeUpdateRequest((json \ "description").as[Option[String]], (json \ "title").as[Option[String]]))
      }
      case otherwise   => JsError()
    }

    def writes(nodeAdd: NodeUpdateRequest) = ???
  }

  implicit object TaggedNodeUpdateFormat extends Format[TaggedNodeUpdateRequest] {
    def reads(json: JsValue) = json match {
      case JsObject(_) => {
        JsSuccess(TaggedNodeUpdateRequest((json \ "description").as[Option[String]], (json \ "title").as[Option[String]], (json \ "addedTags").as[Option[List[String]]].getOrElse(List.empty)))
      }
      case otherwise   => JsError()
    }

    def writes(nodeAdd: TaggedNodeUpdateRequest) = ???
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
