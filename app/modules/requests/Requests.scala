package modules.requests

// either references a new node (just by title) which should be created or it
// refers to an existing node via id (then title is ignored)
//TODO: this should be Either[String,String]!
case class TagConnectRequest(id: Option[String], title: Option[String])

trait AddTagRequestBase {
  val addedTagsOption: Option[List[TagConnectRequest]]
  def addedTags = addedTagsOption.getOrElse(List.empty)
}
trait RemoveTagRequestBase {
  val removedTagsOption: Option[List[String]]
  def removedTags = removedTagsOption.getOrElse(List.empty)
}

case class PostAddRequest(description: Option[String], title: String, addedTagsOption: Option[List[TagConnectRequest]]) extends AddTagRequestBase
case class PostUpdateRequest(description: Option[String], title: Option[String], addedTagsOption: Option[List[TagConnectRequest]], removedTagsOption: Option[List[String]]) extends AddTagRequestBase with RemoveTagRequestBase

case class ConnectableUpdateRequest(addedTagsOption: Option[List[TagConnectRequest]], removedTagsOption: Option[List[String]]) extends AddTagRequestBase with RemoveTagRequestBase

case class TagAddRequest(title: String)
case class TagUpdateRequest(description: Option[String])

case class UserUpdateRequest(email: Option[String])
