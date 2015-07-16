package modules.requests

trait TaggedRequestBase {
  val addedTagsOption: Option[List[String]]
  def addedTags = addedTagsOption.getOrElse(List.empty)
}

case class PostAddRequest(description: Option[String], title: String)
case class PostUpdateRequest(description: Option[String], title: Option[String])
case class TaggedPostAddRequest(description: Option[String], title: String, addedTagsOption: Option[List[String]]) extends TaggedRequestBase
case class TaggedPostUpdateRequest(description: Option[String], title: Option[String], addedTagsOption: Option[List[String]]) extends TaggedRequestBase

case class TagAddRequest(title: String)
case class TagUpdateRequest(description: Option[String])
