package modules.requests

trait TaggedRequestBase {
  val addedTagsOption: Option[List[TagConnectRequest]]
  def addedTags = addedTagsOption.getOrElse(List.empty)
}

case class PostAddRequest(description: Option[String], title: String)
case class PostUpdateRequest(description: Option[String], title: Option[String])
case class TaggedPostAddRequest(description: Option[String], title: String, addedTagsOption: Option[List[TagConnectRequest]]) extends TaggedRequestBase
case class TaggedPostUpdateRequest(description: Option[String], title: Option[String], addedTagsOption: Option[List[TagConnectRequest]]) extends TaggedRequestBase

case class TagAddRequest(title: String)
case class TagUpdateRequest(description: Option[String])

// either references a new node (just by title) which should be created or it
// refers to an existing node via id (then title is ignored)
case class TagConnectRequest(id: Option[String], title: Option[String])

case class UserUpdateRequest(email: Option[String])
