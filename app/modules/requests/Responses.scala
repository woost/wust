package modules.requests

import model.WustSchema.{UuidNode, Discourse}

case class ConnectResponse[+NODE <: UuidNode](graph: Discourse, node: Option[NODE])
