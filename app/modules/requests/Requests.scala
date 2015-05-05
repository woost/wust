package modules.requests

trait UserRequest
case class NodeAddRequest(title: String, description: Option[String] = None) extends UserRequest
case class ConnectRequest(uuid: String) extends UserRequest
