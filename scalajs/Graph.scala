import scala.scalajs.js
import js.annotation.JSExportAll
import js.annotation.JSExport

import collection.mutable

// http://www.scala-js.org/doc/export-to-javascript.html

@JSExport
@JSExportAll case class Node(id: String)
@JSExport
@JSExportAll case class Relation(startId: String, endId: String)

@JSExport
object GraphFactory {
  @JSExport
  def fromRecord(record: js.Dynamic) = {
    import scalajs.js.DynamicImplicits._
    implicit def DynamicToString(d: js.Dynamic): String = d.asInstanceOf[String]

    new Graph(
      mutable.Set.empty ++ record.nodes.asInstanceOf[js.Array[js.Dynamic]].map(n => Node(n.id)),
      mutable.Set.empty ++ record.edges.asInstanceOf[js.Array[js.Dynamic]].map(r => Relation(r.startId, r.endId)))
  }
}

@JSExport
@JSExportAll
case class Graph(nodes: mutable.Set[Node] = mutable.Set.empty, relations: mutable.Set[Relation] = mutable.Set.empty) {
  def add(node: Node) { nodes += node }
  override def toString = s"Graph(${ nodes.map(_.id).mkString(",") }, ${ relations.map(r => r.startId + "->" + r.endId).mkString(",") })"
}
