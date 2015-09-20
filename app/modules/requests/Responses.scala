package modules.requests

import model.WustSchema.{Connectable, Discourse}

case class ConnectResponse[+NODE <: Connectable](graph: Discourse, node: Option[NODE])
