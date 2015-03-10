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

  def filterRelations[RELATION <: SchemaRelation[START, END], START <: SchemaNode, END <: SchemaNode]
  (relations: Set[Relation], relationFactory: SchemaRelationFactory[RELATION, START, END]): Set[RELATION] = {
    relations.filter(_.relationType == relationFactory.relationType).map(relationFactory.create)
  }
}

trait SchemaGraph extends Schema with SchemaNodeFilter {
  def nodesAs[T <: SchemaNode](nodeFactory: SchemaNodeFactory[T]) = {
    filterNodes(graph.nodes.toSet, nodeFactory)
  }

  def relationsAs[RELATION <: SchemaRelation[START, END], START <: SchemaNode, END <: SchemaNode]
  (relationFactory: SchemaRelationFactory[RELATION, START, END]) = {
    filterRelations(graph.relations.toSet, relationFactory)
  }

  def add(schemaNode: SchemaNode) {
    graph.nodes += schemaNode.node
  }

  def add[START <: SchemaNode, END <: SchemaNode](schemaRelation: SchemaRelation[START, END]) {
    graph.relations += schemaRelation.relation
  }
}


trait SchemaNodeFactory[+T <: SchemaNode] {
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
  def relationType: RelationType

  def create(relation: Relation): RELATION
  def startNodeFactory: SchemaNodeFactory[START]
  def endNodeFactory: SchemaNodeFactory[END]

  def local(startNode: START, endNode: END): RELATION = {
    create(Relation.local(startNode.node, endNode.node, relationType))
  }
}


trait SchemaRelation[+START <: SchemaNode, +END <: SchemaNode] {
  def relation: Relation
  def startNode: START
  def endNode: END

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

trait DiscourseRelation[+START <: DiscourseNode, +END <: DiscourseNode] extends SchemaRelation[START, END]


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
  def create(relation: Relation) = Causes(relation, startNodeFactory.create(relation.startNode), endNodeFactory.create(relation.endNode))
  def relationType = RelationType("CAUSES")
  def startNodeFactory = Problem
  def endNodeFactory = Problem
}
case class Causes(relation: Relation, startNode: Problem, endNode: Problem) extends DiscourseRelation[Problem, Problem]

object Prevents extends SchemaRelationFactory[Prevents, Problem, Goal] {
  def create(relation: Relation) = Prevents(relation, startNodeFactory.create(relation.startNode), endNodeFactory.create(relation.endNode))
  def relationType = RelationType("PREVENTS")
  def startNodeFactory = Problem
  def endNodeFactory = Goal
}
case class Prevents(relation: Relation, startNode: Problem, endNode: Goal) extends DiscourseRelation[Problem, Goal]

object SupportsSolves extends SchemaRelationFactory[SupportsSolves, ProArgument, Solves] {
  def create(relation: Relation) = SupportsSolves(relation, startNodeFactory.create(relation.startNode), endNodeFactory.create(relation.endNode))
  def relationType = RelationType("SUPPORTSSOLVES")
  def startNodeFactory = ProArgument
  def endNodeFactory = Solves
}
case class SupportsSolves(relation: Relation, startNode: ProArgument, endNode: Solves) extends DiscourseRelation[ProArgument, Solves]

object OpposesSolves extends SchemaRelationFactory[OpposesSolves, ConArgument, Solves] {
  def create(relation: Relation) = OpposesSolves(relation, startNodeFactory.create(relation.startNode), endNodeFactory.create(relation.endNode))
  def relationType = RelationType("OPPOSESSOLVES")
  def startNodeFactory = ConArgument
  def endNodeFactory = Solves
}
case class OpposesSolves(relation: Relation, startNode: ConArgument, endNode: Solves) extends DiscourseRelation[ConArgument, Solves]

object SupportsReaches extends SchemaRelationFactory[SupportsReaches, ProArgument, Reaches] {
  def create(relation: Relation) = SupportsReaches(relation, startNodeFactory.create(relation.startNode), endNodeFactory.create(relation.endNode))
  def relationType = RelationType("SUPPORTSREACHES")
  def startNodeFactory = ProArgument
  def endNodeFactory = Reaches
}
case class SupportsReaches(relation: Relation, startNode: ProArgument, endNode: Reaches) extends DiscourseRelation[ProArgument, Reaches]

object OpposesReaches extends SchemaRelationFactory[OpposesReaches, ConArgument, Reaches] {
  def create(relation: Relation) = OpposesReaches(relation, startNodeFactory.create(relation.startNode), endNodeFactory.create(relation.endNode))
  def relationType = RelationType("OPPOSESREACHES")
  def startNodeFactory = ConArgument
  def endNodeFactory = Reaches
}
case class OpposesReaches(relation: Relation, startNode: ConArgument, endNode: Reaches) extends DiscourseRelation[ConArgument, Reaches]

object IdeaToSolves extends SchemaRelationFactory[IdeaToSolves, Idea, Solves] {
  def create(relation: Relation) = IdeaToSolves(relation, startNodeFactory.create(relation.startNode), endNodeFactory.create(relation.endNode))
  def relationType = RelationType("IDEATOSOLVES")
  def startNodeFactory = Idea
  def endNodeFactory = Solves
}
case class IdeaToSolves(relation: Relation, startNode: Idea, endNode: Solves) extends DiscourseRelation[Idea, Solves]

object IdeaToReaches extends SchemaRelationFactory[IdeaToReaches, Idea, Reaches] {
  def create(relation: Relation) = IdeaToReaches(relation, startNodeFactory.create(relation.startNode), endNodeFactory.create(relation.endNode))
  def relationType = RelationType("IDEATOREACHES")
  def startNodeFactory = Idea
  def endNodeFactory = Reaches
}
case class IdeaToReaches(relation: Relation, startNode: Idea, endNode: Reaches) extends DiscourseRelation[Idea, Reaches]

object SolvesToProblem extends SchemaRelationFactory[SolvesToProblem, Solves, Problem] {
  def create(relation: Relation) = SolvesToProblem(relation, startNodeFactory.create(relation.startNode), endNodeFactory.create(relation.endNode))
  def relationType = RelationType("SOLVESTOPROBLEM")
  def startNodeFactory = Solves
  def endNodeFactory = Problem
}
case class SolvesToProblem(relation: Relation, startNode: Solves, endNode: Problem) extends DiscourseRelation[Solves, Problem]

object ReachesToGoal extends SchemaRelationFactory[ReachesToGoal, Reaches, Goal] {
  def create(relation: Relation) = ReachesToGoal(relation, startNodeFactory.create(relation.startNode), endNodeFactory.create(relation.endNode))
  def relationType = RelationType("SOLVESTOPROBLEM")
  def startNodeFactory = Reaches
  def endNodeFactory = Goal
}
case class ReachesToGoal(relation: Relation, startNode: Reaches, endNode: Goal) extends DiscourseRelation[Reaches, Goal]

object Discourse {def empty = Discourse(Graph.empty) }

case class Discourse(graph: Graph) extends SchemaGraph {
  // nodes
  def goals: Set[Goal] = nodesAs(Goal)
  def problems: Set[Problem] = nodesAs(Problem)
  def ideas: Set[Idea] = nodesAs(Idea)

  // hyperedge nodes
  def reaches: Set[Reaches] = nodesAs(Reaches)
  def solves: Set[Solves] = nodesAs(Solves)

  def nodes: Set[DiscourseNode] = {
    goals ++ problems ++ ideas ++ reaches ++ solves
  }


  // relations
  def causes: Set[Causes] = relationsAs(Causes)
  def prevents: Set[Prevents] = relationsAs(Prevents)
  def supportsSolves: Set[SupportsSolves] = relationsAs(SupportsSolves)
  def opposesSolves: Set[OpposesSolves] = relationsAs(OpposesSolves)
  def supportsReaches: Set[SupportsReaches] = relationsAs(SupportsReaches)
  def opposesReaches: Set[OpposesReaches] = relationsAs(OpposesReaches)
  def ideaToSolves: Set[IdeaToSolves] = relationsAs(IdeaToSolves)
  def ideaToReaches: Set[IdeaToReaches] = relationsAs(IdeaToReaches)
  def solvesToProblem: Set[SolvesToProblem] = relationsAs(SolvesToProblem)
  def reachesToGoal: Set[ReachesToGoal] = relationsAs(ReachesToGoal)

  def relations: Set[_ <: DiscourseRelation[DiscourseNode, DiscourseNode]] = {
    causes ++ prevents ++
      ideaToSolves ++ ideaToReaches ++ solvesToProblem ++ reachesToGoal ++
      supportsSolves ++ opposesSolves ++ supportsReaches ++ opposesReaches
  }
}
