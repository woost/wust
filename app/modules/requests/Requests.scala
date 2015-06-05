package modules.requests

trait UserRequest
trait NodeAddRequestBase extends UserRequest {
  val description: String
  val title: Option[String]
}
case class NodeAddRequest(description: String, title: Option[String] = None) extends NodeAddRequestBase
case class TaggedNodeAddRequest(description: String, title: Option[String] = None, addedTags: Seq[String]) extends NodeAddRequestBase
case class ConnectRequest(uuid: String) extends UserRequest
