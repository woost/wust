package modules.requests

trait UserRequest
case class NodeAddRequest(description: String, title: Option[String] = None) extends UserRequest
case class ConnectRequest(uuid: String) extends UserRequest
