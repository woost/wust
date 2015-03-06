package model


import renesca.graph._
import renesca.parameter.StringPropertyValue
import renesca.parameter.implicits._

trait Schema {
  def graph: Graph
}

trait SchemaNodeFilter extends Schema {
  def filterNodes[T <: SchemaNode](nodes: Set[Node], nodeFactory: SchemaNodeFactory[T]): Set[T] = {
    nodes.filter(_.labels.contains(nodeFactory.label)).map { node =>
      val schemaNode = nodeFactory.apply(node)
      schemaNode.graph = graph
      schemaNode
    }
  }
}

trait SchemaGraph extends Schema with SchemaNodeFilter {
  def nodesAs[T <: SchemaNode](NodeFactory: SchemaNodeFactory[T]) = {
    filterNodes(graph.nodes.toSet, NodeFactory)
  }

  def add(schemaNode: SchemaNode): Unit = {
    graph.nodes += schemaNode.node
  }
}


trait SchemaNodeFactory[T <: SchemaNode] {
  def label: Label
  def apply: (Node) => T //TODO: does not work as expected: instance(node)
}


trait SchemaNode extends Schema with SchemaNodeFilter {
  def node: Node
  implicit var graph: Graph = null

  def neighboursAs[T <: SchemaNode](nodeFactory: SchemaNodeFactory[T]) = filterNodes(node.neighbours, nodeFactory)
  def getStringProperty(key: String) = node.properties(key).asInstanceOf[StringPropertyValue]
}

trait SchemaRelation[START <: SchemaNode, END <: SchemaNode] {
  def relation: Relation

  def startNodeFactory: SchemaNodeFactory[START]
  def endNodeFactory: SchemaNodeFactory[END]

  def startNode: START = startNodeFactory.apply(relation.startNode)
  def endNode: END = endNodeFactory.apply(relation.endNode)
  def relationType: RelationType = relation.relationType
}

object UUID {
  def applyTo(node: Node) {
    node.properties("uuid") = java.util.UUID.randomUUID.toString
  }
}
///////////////////////////////////////////////////


trait DiscourseNode extends SchemaNode {
  def label = node.labels.head
  def uuid: String = getStringProperty("uuid")

  def title: String = getStringProperty("title")
  def title_=(newTitle: String) { node.properties("title") = newTitle }
}

trait HyperEdgeNode extends DiscourseNode {
  // TODO: HyperEdgeNode does not have a title
  override def title = ???
  override def title_=(newTitle: String) = ???
}
trait ContentNode extends DiscourseNode {
}

trait DiscourseRelation[START <: DiscourseNode, END <: DiscourseNode] extends SchemaRelation[START, END]


trait DiscourseNodeFactory[T <: DiscourseNode] extends SchemaNodeFactory[T] {
  def local = {
    val node = Node.local
    UUID.applyTo(node)
    node.labels += label
    this.apply(node)
  }
}

trait ContentNodeFactory[T <: ContentNode] extends DiscourseNodeFactory[T] {
  override def apply: (Node) => T
}

object Goal extends ContentNodeFactory[Goal] {
  val apply = new Goal(_)
  val label = Label("GOAL")
}
object Problem extends ContentNodeFactory[Problem] {
  val apply = new Problem(_)
  val label = Label("PROBLEM")
}
object Idea extends ContentNodeFactory[Idea] {
  val apply = new Idea(_)
  val label = Label("IDEA")
}
object ProArgument extends ContentNodeFactory[ProArgument] {
  val apply = new ProArgument(_)
  val label = Label("PROARGUMENT")
}
object ConArgument extends ContentNodeFactory[ConArgument] {
  val apply = new ConArgument(_)
  val label = Label("CONARGUMENT")
}

object IdeaProblemGoal extends DiscourseNodeFactory[IdeaProblemGoal] {
  val apply = new IdeaProblemGoal(_)
  val label = Label("IDEAPROBLEMGOAL")
}
object ProblemGoal extends DiscourseNodeFactory[ProblemGoal] {
  val apply = new ProblemGoal(_)
  val label = Label("PROBLEMGOAL")
}


trait ProblemGoals extends SchemaNode {
  def problemGoals: Set[ProblemGoal] = neighboursAs(ProblemGoal)
  def ideas: Set[Idea] = problemGoals.flatMap(_.ideas)
}
case class Goal(node: Node) extends ContentNode with ProblemGoals
case class Problem(node: Node) extends ContentNode with ProblemGoals
case class Idea(node: Node) extends ContentNode
case class ProArgument(node: Node) extends ContentNode
case class ConArgument(node: Node) extends ContentNode

case class ProblemGoal(node: Node) extends HyperEdgeNode {
  def ideaProblemGoals: Set[IdeaProblemGoal] = neighboursAs(IdeaProblemGoal)
  def ideas: Set[Idea] = ideaProblemGoals.flatMap(_.ideas)
}

case class IdeaProblemGoal(node: Node) extends HyperEdgeNode {
  def ideas: Set[Idea] = neighboursAs(Idea)
}

case class ProblemToProblemGoal(relation: Relation) extends DiscourseRelation[Problem, ProblemGoal] {
  def startNodeFactory = Problem
  def endNodeFactory = ProblemGoal
}


object Discourse {def empty = Discourse(Graph.empty) }

case class Discourse(graph: Graph) extends SchemaGraph {
  def goals: Set[Goal] = nodesAs(Goal)
  def problems: Set[Problem] = nodesAs(Problem)
  def ideas: Set[Idea] = nodesAs(Idea)

  def discourseNodes: Set[DiscourseNode] = goals ++ problems ++ ideas
  def discourseRelations: Set[DiscourseRelation[DiscourseNode, DiscourseNode]] = graph.relations.toSet.map { rawRelation: Relation =>
    new DiscourseRelation[DiscourseNode, DiscourseNode] {
      val relation = rawRelation
      object endNodeFactory extends DiscourseNodeFactory[DiscourseNode] {
        val label = rawRelation.endNode.labels.head
        val apply = (node: Node) => new DiscourseNode {
          override def node: Node = rawRelation.endNode
        }
      }
      object startNodeFactory extends DiscourseNodeFactory[DiscourseNode] {
        val label = rawRelation.startNode.labels.head
        val apply = (node: Node) => new DiscourseNode {
          override def node: Node = rawRelation.startNode
        }
      }
    }
  }

}
