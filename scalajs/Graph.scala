import scala.scalajs.js
import js.annotation.JSExport

import collection.mutable

// http://www.scala-js.org/doc/export-to-javascript.html

@JSExport case class Node(id:String)
@JSExport case class Relation(startId:String, endId:String)

@JSExport
case class Graph(nodes:mutable.Set[Node] = mutable.Set.empty, relations:mutable.Set[Relation] = mutable.Set.empty) {
  @JSExport
  def add(node:Node) {nodes += node}
  override def toString = s"Graph(${nodes.map(_.id).mkString(",")}, ${relations.map(r => r.startId + "->" + r.endId).mkString(",")})"
}
