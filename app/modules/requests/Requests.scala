package modules.requests

trait UserRequest
case class NodeAddRequest(title: String) extends UserRequest
case class ConnectRequest(uuid: String) extends UserRequest
