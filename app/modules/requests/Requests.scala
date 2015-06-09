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
case class NodeAddRequest(description: String, title: Option[String]) extends NodeAddRequestBase
case class NodeUpdateRequest(description: Option[String], title: Option[String]) extends NodeUpdateRequestBase
case class TaggedNodeAddRequest(description: String, title: Option[String], addedTags: Seq[String]) extends NodeAddRequestBase
case class TaggedNodeUpdateRequest(description: Option[String], title: Option[String] = None, addedTags: Seq[String]) extends NodeUpdateRequestBase
case class ConnectRequest(uuid: String) extends UserRequest
