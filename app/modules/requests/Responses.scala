package modules.requests

import model.WustSchema.{UuidNode, Discourse}

trait UserResponse

case class ConnectResponse[+NODE <: UuidNode](graph: Discourse, node: Option[NODE]) extends UserResponse
