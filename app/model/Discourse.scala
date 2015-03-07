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
      val schemaNode = nodeFactory.create(node)
      schemaNode.graph = graph
      schemaNode
    }
  }
}

trait SchemaGraph extends Schema with SchemaNodeFilter {
  def nodesAs[T <: SchemaNode](NodeFactory: SchemaNodeFactory[T]) = {
    filterNodes(graph.nodes.toSet, NodeFactory)
  }

  def add(schemaNode: SchemaNode) {
    graph.nodes += schemaNode.node
  }

  def add[START <: SchemaNode, END <: SchemaNode](schemaRelation: SchemaRelation[START, END]) {
    graph.relations += schemaRelation.relation
  }
}


trait SchemaNodeFactory[T <: SchemaNode] {
  def label: Label
  def create(node: Node): T

  def local: T = create(Node.local(List(label)))
}


trait SchemaNode extends Schema with SchemaNodeFilter {
  def node: Node
  implicit var graph: Graph = null

  def neighboursAs[T <: SchemaNode](nodeFactory: SchemaNodeFactory[T]) = filterNodes(node.neighbours, nodeFactory)
  def getStringProperty(key: String) = node.properties(key).asInstanceOf[StringPropertyValue]
}

trait SchemaRelationFactory[RELATION <: SchemaRelation[START, END], START <: SchemaNode, END <: SchemaNode] {
  def create(relation: Relation): RELATION
  def relationType: RelationType
  def local(startNode: START, endNode: END): RELATION = {
    create(Relation.local(startNode.node, endNode.node, relationType))
  }
}


trait SchemaRelation[START <: SchemaNode, END <: SchemaNode] {
  def relation: Relation

  def startNodeFactory: SchemaNodeFactory[START]
  def endNodeFactory: SchemaNodeFactory[END]

  def startNode: START = startNodeFactory.create(relation.startNode)
  def endNode: END = endNodeFactory.create(relation.endNode)
  def relationType: RelationType = relation.relationType
}

object UUID {
  def applyTo[T <: SchemaNode](node: T) = {
    node.node.properties("uuid") = java.util.UUID.randomUUID.toString
    node
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
  override def title = ""
  override def title_=(newTitle: String) {}
}
trait ContentNode extends DiscourseNode {
}

trait DiscourseRelation[START <: DiscourseNode, END <: DiscourseNode] extends SchemaRelation[START, END]


trait DiscourseNodeFactory[T <: DiscourseNode] extends SchemaNodeFactory[T] {
  override def local = UUID.applyTo(super.local)
}

trait ContentNodeFactory[T <: ContentNode] extends DiscourseNodeFactory[T] {
  override def create(node: Node): T

  def local(title: String): T = {
    val idea = super.local
    idea.title = title
    idea
  }
}

object Goal extends ContentNodeFactory[Goal] {
  def create(node: Node) = new Goal(node)
  val label = Label("GOAL")
}
object Problem extends ContentNodeFactory[Problem] {
  def create(node: Node) = new Problem(node)
  val label = Label("PROBLEM")
}
object Idea extends ContentNodeFactory[Idea] {
  def create(node: Node) = new Idea(node)
  val label = Label("IDEA")
}
object ProArgument extends ContentNodeFactory[ProArgument] {
  def create(node: Node) = new ProArgument(node)
  val label = Label("PROARGUMENT")
}
object ConArgument extends ContentNodeFactory[ConArgument] {
  def create(node: Node) = new ConArgument(node)
  val label = Label("CONARGUMENT")
}

object Prevents extends DiscourseNodeFactory[Prevents] {
  def create(node: Node) = new Prevents(node)
  val label = Label("PREVENTS")
}
object Solves extends DiscourseNodeFactory[Solves] {
  def create(node: Node) = new Solves(node)
  val label = Label("SOLVES")
}


trait ProblemGoals extends SchemaNode {
  def problemGoals: Set[Prevents] = neighboursAs(Prevents)
  def ideas: Set[Idea] = problemGoals.flatMap(_.ideas)
}
case class Goal(node: Node) extends ContentNode with ProblemGoals
case class Problem(node: Node) extends ContentNode with ProblemGoals
case class Idea(node: Node) extends ContentNode
case class ProArgument(node: Node) extends ContentNode
case class ConArgument(node: Node) extends ContentNode

case class Prevents(node: Node) extends HyperEdgeNode {
  def ideaProblemGoals: Set[Solves] = neighboursAs(Solves)
  def ideas: Set[Idea] = ideaProblemGoals.flatMap(_.ideas)
}

case class Solves(node: Node) extends HyperEdgeNode {
  def ideas: Set[Idea] = neighboursAs(Idea)
}

object ProblemToPrevents extends SchemaRelationFactory[ProblemToPrevents, Problem, Prevents] {
  def create(relation: Relation) = ProblemToPrevents(relation)
  def relationType = RelationType("PROBLEMTOPREVENTS")
}
case class ProblemToPrevents(relation: Relation) extends DiscourseRelation[Problem, Prevents] {
  def startNodeFactory = Problem
  def endNodeFactory = Prevents
}

object IdeaToSolves extends SchemaRelationFactory[IdeaToSolves, Idea, Solves] {
  def create(relation: Relation) = IdeaToSolves(relation)
  def relationType = RelationType("IDEATOSOLVES")
}
case class IdeaToSolves(relation: Relation) extends DiscourseRelation[Idea, Solves] {
  def startNodeFactory = Idea
  def endNodeFactory = Solves
}

object SolvesToPrevents extends SchemaRelationFactory[SolvesToPrevents, Solves, Prevents] {
  def create(relation: Relation) = SolvesToPrevents(relation)
  def relationType = RelationType("SOLVESTOPREVENTS")
}
case class SolvesToPrevents(relation: Relation) extends DiscourseRelation[Solves, Prevents] {
  def startNodeFactory = Solves
  def endNodeFactory = Prevents
}

object PreventsToGoal extends SchemaRelationFactory[PreventsToGoal, Prevents, Goal] {
  def create(relation: Relation) = PreventsToGoal(relation)
  def relationType = RelationType("PREVENTSTOGOAL")
}
case class PreventsToGoal(relation: Relation) extends DiscourseRelation[Prevents, Goal] {
  def startNodeFactory = Prevents
  def endNodeFactory = Goal
}

object Discourse {def empty = Discourse(Graph.empty) }

case class Discourse(graph: Graph) extends SchemaGraph {
  def goals: Set[Goal] = nodesAs(Goal)
  def problems: Set[Problem] = nodesAs(Problem)
  def ideas: Set[Idea] = nodesAs(Idea)
  def problemGoals: Set[Prevents] = nodesAs(Prevents)
  def ideaProblemGoals: Set[Solves] = nodesAs(Solves)

  def discourseNodes: Set[DiscourseNode] = goals ++ problems ++ ideas ++ problemGoals ++ ideaProblemGoals
  def discourseRelations: Set[DiscourseRelation[DiscourseNode, DiscourseNode]] = graph.relations.toSet.map { rawRelation: Relation =>
    new DiscourseRelation[DiscourseNode, DiscourseNode] {
      val relation = rawRelation
      object endNodeFactory extends DiscourseNodeFactory[DiscourseNode] {
        val label = rawRelation.endNode.labels.head
        def create(node: Node) = new DiscourseNode {
          override def node: Node = rawRelation.endNode
        }
      }
      object startNodeFactory extends DiscourseNodeFactory[DiscourseNode] {
        val label = rawRelation.startNode.labels.head
        def create(node: Node) = new DiscourseNode {
          override def node: Node = rawRelation.startNode
        }
      }
    }
  }

}
