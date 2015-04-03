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

@macros.GraphSchema
object WustSchema {
  @Schema class Discourse {
    def nodes:Set[DiscourseNode]
  }

  trait DiscourseNodeFactory[T <: DiscourseNode] extends SchemaNodeFactory[T] {
    override def local = UUID.applyTo(super.local)
  }

  trait ContentNodeFactory[T <: ContentNode] extends DiscourseNodeFactory[T] {
    def local(title: String): T = {
      val node = super.local
      node.title = title
      println(node.uuid)
      node
    }
  }

  @Node trait DiscourseNode extends SchemaNode {
    var title: String;
    val uuid: String
  }
  @Node trait ContentNode extends DiscourseNode {def factory: ContentNodeFactory}
  @Node trait HyperEdgeNode extends DiscourseNode {def factory: DiscourseNodeFactory}

  //TODO: generate indirect neighbour-accessors based on hypernodes
  //TODO: named node/relation groups (based on nodeTraits?)


  @Node("GOAL")
  class Goal extends ContentNode {
    def ideas = IdeaToReaches <-- ReachesToGoal
  }
  @Node("PROBLEM")
  class Problem extends ContentNode {
    def ideas = IdeaToSolves <-- SolvesToProblem
  }
  @Node("IDEA")
  class Idea extends ContentNode {
    def problems = IdeaToSolves --> SolvesToProblem
    def goals = IdeaToReaches --> ReachesToGoal
  }
  @Node("PROARGUMENT") class ProArgument extends ContentNode
  @Node("CONARGUMENT") class ConArgument extends ContentNode
  @Node("SOLVES") class Solves extends HyperEdgeNode
  @Node("REACHES") class Reaches extends HyperEdgeNode

  @Relation("SUBIDEA") class SubIdea(startNode: Idea, endNode: Idea)
  @Relation("SUBGOAL") class SubGoal(startNode: Goal, endNode: Goal)
  @Relation("CAUSES") class Causes(startNode: Problem, endNode: Problem)
  @Relation("PREVENTS") class Prevents(startNode: Problem, endNode: Goal)
  @Relation("SUPPORTSSOLVES") class SupportsSolves(startNode: ProArgument, endNode: Solves)
  @Relation("OPPOSESSOLVES") class OpposesSolves(startNode: ConArgument, endNode: Solves)
  @Relation("SUPPORTSREACHES") class SupportsReaches(startNode: ProArgument, endNode: Reaches)
  @Relation("OPPOSESREACHES") class OpposesReaches(startNode: ConArgument, endNode: Reaches)
  @Relation("IDEATOSOLVES") class IdeaToSolves(startNode: Idea, endNode: Solves)
  @Relation("IDEATOREACHES") class IdeaToReaches(startNode: Idea, endNode: Reaches)
  @Relation("SOLVESTOPROBLEM") class SolvesToProblem(startNode: Solves, endNode: Problem)
  @Relation("REACHESTOGOAL") class ReachesToGoal(startNode: Reaches, endNode: Goal)
}

