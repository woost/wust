package modules.requests

// either references a new node (just by title) which should be created or it
// refers to an existing node via id (then title is ignored)
case class ClassificationRequest(id: String)
case class TagConnectRequest(id: Option[String], title: Option[String], classificationsOption: Option[List[ClassificationRequest]]) {
  def classifications = classificationsOption.getOrElse(List.empty)
}
case class TagDisconnectRequest(id: String, classificationsOption: Option[List[ClassificationRequest]]) {
  def classifications = classificationsOption.getOrElse(List.empty)
}

case class PostAddRequest(description: Option[String], title: String, addedTagsOption: Option[List[TagConnectRequest]]) {
  def addedTags = addedTagsOption.getOrElse(List.empty)
}
case class PostUpdateRequest(description: Option[String], title: Option[String], addedTagsOption: Option[List[TagConnectRequest]], removedTagsOption: Option[List[TagDisconnectRequest]]) {
  def addedTags = addedTagsOption.getOrElse(List.empty)
  def removedTags = removedTagsOption.getOrElse(List.empty)
}

case class ConnectsUpdateRequest(addedTagsOption: Option[List[ClassificationRequest]], removedTagsOption: Option[List[ClassificationRequest]]) {
  def addedTags = addedTagsOption.getOrElse(List.empty)
  def removedTags = removedTagsOption.getOrElse(List.empty)
}

case class TagAddRequest(title: String)
case class TagUpdateRequest(description: Option[String])

case class UserUpdateRequest(email: Option[String], password: Option[String])
