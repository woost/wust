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

object Solves extends DiscourseNodeFactory[Solves] {
  def create(node: Node) = new Solves(node)
  val label = Label("SOLVES")
}
object Reaches extends DiscourseNodeFactory[Reaches] {
  def create(node: Node) = new Reaches(node)
  val label = Label("REACHES")
}

case class Goal(node: Node) extends ContentNode {
  def reaches: Set[Reaches] = neighboursAs(Reaches)
  def ideas: Set[Idea] = reaches.flatMap(_.ideas)
}
case class Problem(node: Node) extends ContentNode {
  def solves: Set[Solves] = neighboursAs(Solves)
  def ideas: Set[Idea] = solves.flatMap(_.ideas)
}
case class Idea(node: Node) extends ContentNode {
  def solves: Set[Solves] = neighboursAs(Solves)
  def reaches: Set[Reaches] = neighboursAs(Reaches)
  def problems: Set[Problem] = solves.flatMap(_.problems)
  def goals: Set[Goal] = reaches.flatMap(_.goals)
}
case class ProArgument(node: Node) extends ContentNode
case class ConArgument(node: Node) extends ContentNode

case class Solves(node: Node) extends HyperEdgeNode {
  def problems: Set[Problem] = neighboursAs(Problem)
  def ideas: Set[Idea] = neighboursAs(Idea)
}
case class Reaches(node: Node) extends HyperEdgeNode {
  def goals: Set[Goal] = neighboursAs(Goal)
  def ideas: Set[Idea] = neighboursAs(Idea)
}

object Causes extends SchemaRelationFactory[Causes, Problem, Problem] {
  def create(relation: Relation) = Causes(relation)
  def relationType = RelationType("CAUSES")
}
case class Causes(relation: Relation) extends DiscourseRelation[Problem, Problem] {
  def startNodeFactory = Problem
  def endNodeFactory = Problem
}

object Prevents extends SchemaRelationFactory[Prevents, Problem, Goal] {
  def create(relation: Relation) = Prevents(relation)
  def relationType = RelationType("PREVENTS")
}
case class Prevents(relation: Relation) extends DiscourseRelation[Problem, Goal] {
  def startNodeFactory = Problem
  def endNodeFactory = Goal
}

object SupportsSolves extends SchemaRelationFactory[SupportsSolves, ProArgument, Solves] {
  def create(relation: Relation) = SupportsSolves(relation)
  def relationType = RelationType("SUPPORTSSOLVES")
}
case class SupportsSolves(relation: Relation) extends DiscourseRelation[ProArgument, Solves] {
  def startNodeFactory = ProArgument
  def endNodeFactory = Solves
}

object OpposesSolves extends SchemaRelationFactory[OpposesSolves, ConArgument, Solves] {
  def create(relation: Relation) = OpposesSolves(relation)
  def relationType = RelationType("OPPOSESSOLVES")
}
case class OpposesSolves(relation: Relation) extends DiscourseRelation[ConArgument, Solves] {
  def startNodeFactory = ConArgument
  def endNodeFactory = Solves
}

object SupportsReaches extends SchemaRelationFactory[SupportsReaches, ProArgument, Reaches] {
  def create(relation: Relation) = SupportsReaches(relation)
  def relationType = RelationType("SUPPORTSREACHES")
}
case class SupportsReaches(relation: Relation) extends DiscourseRelation[ProArgument, Reaches] {
  def startNodeFactory = ProArgument
  def endNodeFactory = Reaches
}

object OpposesReaches extends SchemaRelationFactory[OpposesReaches, ConArgument, Reaches] {
  def create(relation: Relation) = OpposesReaches(relation)
  def relationType = RelationType("OPPOSESREACHES")
}
case class OpposesReaches(relation: Relation) extends DiscourseRelation[ConArgument, Reaches] {
  def startNodeFactory = ConArgument
  def endNodeFactory = Reaches
}

object IdeaToSolves extends SchemaRelationFactory[IdeaToSolves, Idea, Solves] {
  def create(relation: Relation) = IdeaToSolves(relation)
  def relationType = RelationType("IDEATOSOLVES")
}
case class IdeaToSolves(relation: Relation) extends DiscourseRelation[Idea, Solves] {
  def startNodeFactory = Idea
  def endNodeFactory = Solves
}

object IdeaToReaches extends SchemaRelationFactory[IdeaToReaches, Idea, Reaches] {
  def create(relation: Relation) = IdeaToReaches(relation)
  def relationType = RelationType("IDEATOREACHES")
}
case class IdeaToReaches(relation: Relation) extends DiscourseRelation[Idea, Reaches] {
  def startNodeFactory = Idea
  def endNodeFactory = Reaches
}

object SolvesToProblem extends SchemaRelationFactory[SolvesToProblem, Solves, Problem] {
  def create(relation: Relation) = SolvesToProblem(relation)
  def relationType = RelationType("SOLVESTOPROBLEM")
}
case class SolvesToProblem(relation: Relation) extends DiscourseRelation[Solves, Problem] {
  def startNodeFactory = Solves
  def endNodeFactory = Problem
}

object ReachesToGoal extends SchemaRelationFactory[ReachesToGoal, Reaches, Goal] {
  def create(relation: Relation) = ReachesToGoal(relation)
  def relationType = RelationType("SOLVESTOPROBLEM")
}
case class ReachesToGoal(relation: Relation) extends DiscourseRelation[Reaches, Goal] {
  def startNodeFactory = Reaches
  def endNodeFactory = Goal
}

object Discourse {def empty = Discourse(Graph.empty) }

case class Discourse(graph: Graph) extends SchemaGraph {
  def goals: Set[Goal] = nodesAs(Goal)
  def problems: Set[Problem] = nodesAs(Problem)
  def ideas: Set[Idea] = nodesAs(Idea)
  def reaches: Set[Reaches] = nodesAs(Reaches)
  def solves: Set[Solves] = nodesAs(Solves)

  def discourseNodes: Set[DiscourseNode] = goals ++ problems ++ ideas ++ reaches ++ solves
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
