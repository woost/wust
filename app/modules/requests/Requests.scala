package modules.requests

trait UserRequest
trait NodeRequestBase extends UserRequest {
  val title: Option[String]
}
trait NodeAddRequestBase extends NodeRequestBase {
  val description: String
}
trait NodeUpdateRequestBase extends NodeRequestBase {
  val description: Option[String]
}
trait TaggedRequestBase extends NodeRequestBase {
  val addedTagsOption: Option[List[String]]
  def addedTags = addedTagsOption.getOrElse(List.empty)
}
case class NodeAddRequest(description: String, title: Option[String]) extends NodeAddRequestBase
case class NodeUpdateRequest(description: Option[String], title: Option[String]) extends NodeUpdateRequestBase
case class TaggedNodeAddRequest(description: String, title: Option[String], addedTagsOption: Option[List[String]]) extends NodeAddRequestBase with TaggedRequestBase
case class TaggedNodeUpdateRequest(description: Option[String], title: Option[String], addedTagsOption: Option[List[String]]) extends NodeUpdateRequestBase with TaggedRequestBase
case class ConnectRequest(uuid: String) extends UserRequest
