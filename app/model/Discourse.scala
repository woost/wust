package model

import renesca.graph.{Label, Graph, Node}
import renesca.parameter.StringPropertyValue
import renesca.parameter.implicits._

trait Schema {
  def graph: Graph
  def filterNodes[T <: SchemaNode](nodes:Set[Node])(label: Label, factory: Node => T): Set[T] = nodes.filter(_.labels.contains(label)).map { node => factory(node).withGraph(graph) }
}

trait SchemaGraph extends Schema {
  val nodesAs = filterNodes(graph.nodes)
}

trait SchemaNode extends Schema {
  protected implicit var graph:Graph

  def withGraph(graph: Graph) = {
    this.graph = graph
    this
  }

  val neighboursAs = filterNodes(node.neighbours)

  def node: Node
  def getStringProperty(key: String) = node.properties(key).asInstanceOf[StringPropertyValue]
}

trait ProblemGoals {
  def problemGoals: Set[ProblemGoal] = neighboursAs("PROBLEMGOAL", new ProblemGoal(_))
  def ideas: Set[Idea] = problemGoals.flatMap(_.ideas)
}

trait DiscourseNode extends SchemaNode {

  private var _title: String = getStringProperty("title")

  def title = _title
  def title_=(newTitle: String) { node.properties("title") = newTitle }
}

trait ConnectorNode extends SchemaNode

case class ProblemGoal(node: Node, problem: Problem, goal: Goal) extends ConnectorNode {
  def ideaProblemGoals: Set[IdeaProblemGoal] = node.neighboursAs("IDEAPROBLEMGOAL", new IdeaProblemGoal(_))
  def ideas: Set[Idea] = ideaProblemGoals.flatMap(_.ideas)
}

case class IdeaProblemGoal(node: Node, idea: Idea, problemGoal: ProblemGoal) extends ConnectorNode {
  def ideas: Set[Idea] = node.neighboursAs("IDEA", new Idea(_))
}

case class Goal(node: Node) extends DiscourseNode with ProblemGoals

case class Problem(node: Node) extends DiscourseNode with ProblemGoals

case class Idea(node: Node) extends DiscourseNode

case class ProArgument(node: Node) extends DiscourseNode

case class ConArgument(node: Node) extends DiscourseNode

case class Discourse(graph: Graph) extends SchemaGraph {
  def goals: Set[Goal] = nodesAs("GOAL", new Goal(_))
  def problems: Set[Problem] = nodesAs("PROBLEM", new Problem(_))
  def ideas: Set[Idea] = nodesAs("IDEA", new Idea(_))
}
