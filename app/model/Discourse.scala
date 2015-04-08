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

  def add[START <: SchemaNode, END <: SchemaNode](schemaAbstractRelation: SchemaAbstractRelation[START, END]) {
    schemaAbstractRelation match {
      case relation: HyperRelation[START, END] =>
        graph.nodes += relation.node.node
        graph.relations += relation.startRelation.relation
        graph.relations += relation.endRelation.relation

      case relation: SchemaRelation[START, END] =>
        graph.relations += relation.relation
    }
  }
}


trait SchemaNodeFactory[+T <: SchemaNode] {
  def label: Label
  def create(node: Node): T

  def local: T = create(Node.local(List(label)))
}


trait SchemaNode extends Schema with SchemaNodeFilter {
  def label = node.labels.head
  def node: Node
  implicit var graph: Graph = null

  def neighboursAs[T <: SchemaNode](nodeFactory: SchemaNodeFactory[T]) = filterNodes(node.neighbours, nodeFactory)
  def successorsAs[T <: SchemaNode](nodeFactory: SchemaNodeFactory[T]) = filterNodes(node.successors, nodeFactory)
  def predecessorsAs[T <: SchemaNode](nodeFactory: SchemaNodeFactory[T]) = filterNodes(node.predecessors, nodeFactory)
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


trait SchemaAbstractRelation[+START <: SchemaNode, +END <: SchemaNode] {
  def startNode: START
  def endNode: END
}

trait SchemaRelation[+START <: SchemaNode, +END <: SchemaNode] extends SchemaAbstractRelation[START, END] {
  def relation: Relation
  def relationType: RelationType = relation.relationType
}

trait HyperRelation[+START <: SchemaNode, +END <: SchemaNode] extends SchemaAbstractRelation[START, END] {
  def node: SchemaNode
  def startRelation: SchemaRelation[START, SchemaNode]
  def endRelation: SchemaRelation[SchemaNode, END]
}

object UUID {
  def applyTo[T <: SchemaNode](node: T) = {
    node.node.properties("uuid") = java.util.UUID.randomUUID.toString
    node
  }
}
///////////////////////////////////////////////////

@macros.GraphSchema
object WustSchema {
  @Schema class Discourse {
    def nodes: Set[DiscourseNode]
  }

  trait DiscourseNodeFactory[T <: DiscourseNode] extends SchemaNodeFactory[T] {
    override def local = UUID.applyTo(super.local)
  }

  trait ContentNodeFactory[T <: ContentNode] extends DiscourseNodeFactory[T] {
    def local(title: String): T = {
      val node = super.local
      node.title = title
      node
    }
  }

  @Node trait DiscourseNode extends SchemaNode {
    var title: String;
    val uuid: String
  }
  //TODO: infer factory name
  @Node trait ContentNode extends DiscourseNode {def factory: ContentNodeFactory }
  @Node trait HyperEdgeNode extends DiscourseNode {def factory: DiscourseNodeFactory }

  //TODO: generate indirect neighbour-accessors based on hypernodes
  //TODO: named node/relation groups (based on nodeTraits?)


  @Node
  class Goal extends ContentNode {
    //    def ideas = IdeaToReaches <-- ReachesToGoal
  }
  @Node
  class Problem extends ContentNode {
    //    def ideas = IdeaToSolves <-- SolvesToProblem
  }
  @Node
  class Idea extends ContentNode {
    //    def problems = IdeaToSolves --> SolvesToProblem
    //    def goals = IdeaToReaches --> ReachesToGoal
  }
  @Node class ProArgument extends ContentNode
  @Node class ConArgument extends ContentNode
  // @HyperRelation class Solves(startNode: Idea, endNode: Problem)
  // @HyperRelation class Reaches(startNode: Idea, endNode: Goal)

  trait HyperRelationFactory[
  HYPERRELATION <: HyperRelation[STARTNODE, ENDNODE],
  STARTNODE <: SchemaNode, STARTRELATION <: SchemaRelation[STARTNODE, MIDDLENODE],
  MIDDLENODE <: SchemaNode,
  ENDRELATION <: SchemaRelation[MIDDLENODE, ENDNODE], ENDNODE <: SchemaNode] {
    def local(startNode: STARTNODE, endNode: ENDNODE): HYPERRELATION = {
      val middleNode = middleNodeFactory.local
      val startRelation = startRelationCreate(startNode, Relation.local(startNode.node, middleNode.node, startRelationType), middleNode)
      val endRelation = endRelationCreate(middleNode, Relation.local(middleNode.node, endNode.node, endRelationType), endNode)
      create(startRelation, middleNode, endRelation)
    }

    def label: Label
    def startRelationType: RelationType
    def startRelationCreate(startNode: STARTNODE, relation: Relation, endNode: MIDDLENODE): STARTRELATION
    def endRelationType: RelationType
    def endRelationCreate(startNode: MIDDLENODE, relation: Relation, endNode: ENDNODE): ENDRELATION
    def middleNodeFactory: SchemaNodeFactory[MIDDLENODE]
    def create(startRelation: STARTRELATION, middleNode: MIDDLENODE, endRelation: ENDRELATION): HYPERRELATION
  }

  object Solves extends HyperRelationFactory[Solves, Idea, IdeaToSolves, SolvesNode, SolvesToProblem, Problem] {
    def label = SolvesNode.label
    def startRelationType = "IDEATOSOLVES"
    def startRelationCreate(startNode: Idea, relation: Relation, endNode: SolvesNode) = IdeaToSolves(startNode, relation, endNode)
    def endRelationType = "SOLVESTOIDEA"
    def endRelationCreate(startNode: SolvesNode, relation: Relation, endNode: Problem) = SolvesToProblem(startNode, relation, endNode)
    def middleNodeFactory = SolvesNode
    def create(startRelation: IdeaToSolves, middleNode: SolvesNode, endRelation: SolvesToProblem) = new Solves(startRelation, middleNode, endRelation)
  }
  object SolvesNode extends SchemaNodeFactory[SolvesNode] {
    override def label: Label = "SOLVES"
    override def create(node: Node): SolvesNode = new SolvesNode(node)
  }
  case class SolvesNode(node: Node) extends HyperEdgeNode
  case class IdeaToSolves(startNode: Idea, relation: Relation, endNode: SolvesNode) extends SchemaRelation[Idea, SolvesNode]
  case class SolvesToProblem(startNode: SolvesNode, relation: Relation, endNode: Problem) extends SchemaRelation[SolvesNode, Problem]
  case class Solves(startRelation: IdeaToSolves, node: SolvesNode, endRelation: SolvesToProblem) extends HyperRelation[Idea, Problem] {
    def startNode = startRelation.startNode
    def endNode = endRelation.endNode
  }

  object Reaches extends HyperRelationFactory[Reaches, Idea, IdeaToReaches, ReachesNode, ReachesToGoal, Goal] {
    def label = ReachesNode.label
    def startRelationType = "IDEATOREACHES"
    def startRelationCreate(startNode: Idea, relation: Relation, endNode: ReachesNode) = IdeaToReaches(startNode, relation, endNode)
    def endRelationType = "REACHESTOIDEA"
    def endRelationCreate(startNode: ReachesNode, relation: Relation, endNode: Goal) = ReachesToGoal(startNode, relation, endNode)
    def middleNodeFactory = ReachesNode
    def create(startRelation: IdeaToReaches, middleNode: ReachesNode, endRelation: ReachesToGoal) = new Reaches(startRelation, middleNode, endRelation)
  }
  object ReachesNode extends SchemaNodeFactory[ReachesNode] {
    override def label: Label = "REACHES"
    override def create(node: Node): ReachesNode = new ReachesNode(node)
  }
  case class ReachesNode(node: Node) extends HyperEdgeNode
  case class IdeaToReaches(startNode: Idea, relation: Relation, endNode: ReachesNode) extends SchemaRelation[Idea, ReachesNode]
  case class ReachesToGoal(startNode: ReachesNode, relation: Relation, endNode: Goal) extends SchemaRelation[ReachesNode, Goal]
  case class Reaches(startRelation: IdeaToReaches, node: ReachesNode, endRelation: ReachesToGoal) extends HyperRelation[Idea, Goal] {
    def startNode = startRelation.startNode
    def endNode = endRelation.endNode
  }

  @Relation class SubIdea(startNode: Idea, endNode: Idea)
  @Relation class SubGoal(startNode: Goal, endNode: Goal)
  @Relation class Causes(startNode: Problem, endNode: Problem)
  @Relation class Prevents(startNode: Problem, endNode: Goal)
  @Relation class SupportsSolves(startNode: ProArgument, endNode: SolvesNode)
  @Relation class OpposesSolves(startNode: ConArgument, endNode: SolvesNode)
  @Relation class SupportsReaches(startNode: ProArgument, endNode: ReachesNode)
  @Relation class OpposesReaches(startNode: ConArgument, endNode: ReachesNode)
}

