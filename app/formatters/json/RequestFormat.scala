package formatters.json

import modules.requests._
import play.api.libs.json._
import play.api.libs.functional.syntax._

object RequestFormat {
  implicit val nodeAddRead = (
    (__ \ "description").readNullable[String] and
      (__ \ "title").read[String]
    )(NodeAddRequest)

  implicit val taggedNodeAddFormat = (
    (__ \ "description").readNullable[String] and
      (__ \ "title").read[String] and
      (__ \ "addedTags").readNullable[List[String]]
    )(TaggedNodeAddRequest)

  implicit val nodeUpdateFormat = (
    (__ \ "description").readNullable[String] and
      (__ \ "title").readNullable[String]
    )(NodeUpdateRequest)

  implicit val taggedNodeUpdateFormat = (
    (__ \ "description").readNullable[String] and
      (__ \ "title").readNullable[String] and
      (__ \ "addedTags").readNullable[List[String]]
    )(TaggedNodeUpdateRequest)
}
