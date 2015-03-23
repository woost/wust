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

trait DiscourseNodeFactory[T <: DiscourseNode] extends SchemaNodeFactory[T] {
  override def local = UUID.applyTo(super.local)
}

trait ContentNodeFactory[T <: ContentNode] extends DiscourseNodeFactory[T] {
  override def create(node: Node): T

  def local(title: String): T = {
    val node = super.local
    node.title = title
    node
  }
}

@macros.GraphSchema
object WustSchema {
  //TODO: generate indirect neighbour-accessors based on hypernodes
  //TODO: named node/relation groups

  val schemaName = "Discourse"
  val nodeType = "DiscourseNode"

  val nodes = List(
    // (className, plural, label, factoryType, nodeTraits, indirect successors, indirect predecessors
    ("Goal", "goals", "GOAL", "ContentNodeFactory", List("ContentNode"), List(), List(("ideas", "IdeaToReaches", "ReachesToGoal"))),
    ("Problem", "problems", "PROBLEM", "ContentNodeFactory", List("ContentNode"), List(), List(("ideas", "IdeaToSolves", "SolvesToProblem"))),
    ("Idea", "ideas", "IDEA", "ContentNodeFactory", List("ContentNode"), List(("problems", "IdeaToSolves", "SolvesToProblem"), ("goals", "IdeaToReaches", "ReachesToGoal")), List()),
    ("ProArgument", "proArguments", "PROARGUMENT", "ContentNodeFactory", List("ContentNode"), List(), List()),
    ("ConArgument", "conArguments", "CONARGUMENT", "ContentNodeFactory", List("ContentNode"), List(), List()),
    ("Solves", "solves", "SOLVES", "DiscourseNodeFactory", List("HyperEdgeNode"), List(), List()),
    ("Reaches", "reaches", "REACHES", "DiscourseNodeFactory", List("HyperEdgeNode"), List(), List())
  )

  val relations = List(
    // (className, plural, relationType, startNode, endNode)
    ("SubIdea", "subIdeas", "SUBIDEA", "Idea", "Idea"),
    ("SubGoal", "subGoals", "SUBGOAL", "Goal", "Goal"),
    ("Causes", "causes", "CAUSES", "Problem", "Problem"),
    ("Prevents", "prevents", "PREVENTS", "Problem", "Goal"),
    ("SupportsSolves", "supportsSolves", "SUPPORTSSOLVES", "ProArgument", "Solves"),
    ("OpposesSolves", "opposesSolves", "OPPOSESSOLVES", "ConArgument", "Solves"),
    ("SupportsReaches", "supportsReaches", "SUPPORTSREACHES", "ProArgument", "Reaches"),
    ("OpposesReaches", "opposesReaches", "OPPOSESREACHES", "ConArgument", "Reaches"),
    ("IdeaToSolves", "ideaToSolves", "IDEATOSOLVES", "Idea", "Solves"),
    ("IdeaToReaches", "ideaToReaches", "IDEATOREACHES", "Idea", "Reaches"),
    ("SolvesToProblem", "solvesToProblems", "SOLVESTOPROBLEM", "Solves", "Problem"),
    ("ReachesToGoal", "reachesToGoals", "REACHESTOGOAL", "Reaches", "Goal")
  )
}


