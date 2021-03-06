package modules.db

import model.WustSchema._
import renesca.schema._

object GraphHelper {
  def nodeWithUuid[NODE <: UuidNode](discourse: Discourse, uuid: String): Option[NODE] = discourse.uuidNodes.find(_.uuid == uuid) match {
    case Some(node) => Some(node.asInstanceOf[NODE])
    case None       => None
  }

  def nodesWithType[NODE <: Node](nodes: Seq[Node]) = nodes.map(_.asInstanceOf[NODE])

  def findNodes[NODE <: UuidNode](discourse: Discourse, factory: NodeFactory[NODE], uuids: String*): Seq[NODE] = {
    if(uuids.isEmpty)
      nodesWithType[NODE](discourse.nodes).toSeq
    else
      uuids.flatMap { uuid => nodeWithUuid[NODE](discourse, uuid) }
  }

  def findNodes[START <: UuidNode, END <: UuidNode](discourse: Discourse, startDefinition: UuidNodeDef[START], endDefinition: UuidNodeDef[END]): (Option[START], Option[END]) = {
    (nodeWithUuid[START](discourse, startDefinition.uuid), nodeWithUuid[END](discourse, endDefinition.uuid))
  }

  def findNodes[NODE <: UuidNode](discourse: Discourse, definitions: UuidNodeDef[NODE]*): Seq[NODE] = {
    definitions.flatMap(d => nodeWithUuid[NODE](discourse, d.uuid))
  }
}
