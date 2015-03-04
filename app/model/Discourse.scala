package model

import renesca.graph.{Label, Graph, Node}
import renesca.parameter.StringPropertyValue
import renesca.parameter.implicits._

trait SchemaGraph {
  def graph: Graph
  def nodesAs[T](label: Label, factory: Node => T): Set[T] = graph.nodes.filter(_.labels contains label).toSet.map(factory)
}

trait SchemaNode {
  def node: Node
  def getStringProperty(key: String) = node.properties(key).asInstanceOf[StringPropertyValue]
}

// http://yefremov.net/blog/scala-code-generation/
// Generate:
// - JSON serialization
// - GraphChange Handling
// - Predefined queries as fields (on nodes and graph)

// Discourse(Problem,Idea)
case class Problem(node: Node) extends SchemaNode {
  private var _title: String = getStringProperty("title")
  def title = _title
  def title_=(newTitle: String) { node.properties("title") = newTitle }

  def ideas: Set[Idea] = ???
}

case class Idea(node: Node) extends SchemaNode

case class Discourse(graph: Graph) extends SchemaGraph {
  def problems: Set[Problem] = nodesAs("PROBLEM", new Problem(_))
  def ideas: Set[Idea] = nodesAs("IDEA", new Idea(_))
}
