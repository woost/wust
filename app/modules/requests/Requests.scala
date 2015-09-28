package modules.requests

// either references a new node (just by title) which should be created or it
// refers to an existing node via id (then title is ignored)
//TODO: this should be Either[String,String]!

trait TagLikeConnectRequestBase {
  val id: Option[String]
  val title: Option[String]
}
case class ClassificationConnectRequest(id: Option[String], title: Option[String]) extends TagLikeConnectRequestBase
case class TagConnectRequest(id: Option[String], title: Option[String], classificationsOption: Option[List[ClassificationConnectRequest]]) extends TagLikeConnectRequestBase {
  def classifications = classificationsOption.getOrElse(List.empty)
}

trait AddTagRequestBase {
  val addedTagsOption: Option[List[TagConnectRequest]]
  def addedTags = addedTagsOption.getOrElse(List.empty)
}
trait RemoveTagRequestBase {
  val removedTagsOption: Option[List[String]]
  def removedTags = removedTagsOption.getOrElse(List.empty)
}

trait AddClassificationRequestBase {
  val addedTagsOption: Option[List[ClassificationConnectRequest]]
  def addedTags = addedTagsOption.getOrElse(List.empty)
}

case class PostAddRequest(description: Option[String], title: String, addedTagsOption: Option[List[TagConnectRequest]]) extends AddTagRequestBase
case class PostUpdateRequest(description: Option[String], title: Option[String], addedTagsOption: Option[List[TagConnectRequest]], removedTagsOption: Option[List[String]]) extends AddTagRequestBase with RemoveTagRequestBase

case class ConnectsUpdateRequest(addedTagsOption: Option[List[ClassificationConnectRequest]], removedTagsOption: Option[List[String]]) extends AddClassificationRequestBase with RemoveTagRequestBase

case class TagAddRequest(title: String)
case class TagUpdateRequest(description: Option[String])

case class UserUpdateRequest(email: Option[String])
