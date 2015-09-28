package modules.requests

// either references a new node (just by title) which should be created or it
// refers to an existing node via id (then title is ignored)
//TODO: this should be Either[String,String]!
case class ClassificationConnectRequest(id: Option[String], title: Option[String])
case class ClassificationDisconnectRequest(id: String)
case class TagConnectRequest(id: Option[String], title: Option[String], classificationsOption: Option[List[ClassificationConnectRequest]]) {
  def classifications = classificationsOption.getOrElse(List.empty)
}
case class TagDisconnectRequest(id: String, classificationsOption: Option[List[ClassificationDisconnectRequest]]) {
  def classifications = classificationsOption.getOrElse(List.empty)
}

case class PostAddRequest(description: Option[String], title: String, addedTagsOption: Option[List[TagConnectRequest]]) {
  def addedTags = addedTagsOption.getOrElse(List.empty)
}
case class PostUpdateRequest(description: Option[String], title: Option[String], addedTagsOption: Option[List[TagConnectRequest]], removedTagsOption: Option[List[TagDisconnectRequest]]) {
  def addedTags = addedTagsOption.getOrElse(List.empty)
  def removedTags = removedTagsOption.getOrElse(List.empty)
}

case class ConnectsUpdateRequest(addedTagsOption: Option[List[ClassificationConnectRequest]], removedTagsOption: Option[List[ClassificationDisconnectRequest]]) {
  def addedTags = addedTagsOption.getOrElse(List.empty)
  def removedTags = removedTagsOption.getOrElse(List.empty)
}

case class TagAddRequest(title: String)
case class TagUpdateRequest(description: Option[String])

case class UserUpdateRequest(email: Option[String])
