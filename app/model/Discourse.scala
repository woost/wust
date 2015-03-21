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

@macros.GraphSchema
object WustSchema {

  val nodes = List(
    ("Goal","goals","GOAL"),
    ("Problem","problems","PROBLEM"),
    ("Idea","ideas","IDEA"),
    ("ProArgument","proArguments","PROARGUMENT"),
    ("ConArgument","conArguments","CONARGUMENT")
  )

  val relations = List(
    ("SubIdea","subIdeas", "SUBIDEA","Idea","Idea"),
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




  object Solves extends DiscourseNodeFactory[Solves] {
    def create(node: Node) = new Solves(node)
    val label = Label("SOLVES")
  }
  object Reaches extends DiscourseNodeFactory[Reaches] {
    def create(node: Node) = new Reaches(node)
    val label = Label("REACHES")
  }


  case class Solves(node: Node) extends HyperEdgeNode {
    def problems: Set[Problem] = neighboursAs(Problem)
    def ideas: Set[Idea] = neighboursAs(Idea)
  }
  case class Reaches(node: Node) extends HyperEdgeNode {
    def goals: Set[Goal] = neighboursAs(Goal)
    def ideas: Set[Idea] = neighboursAs(Idea)
  }


  case class Discourse(graph: Graph) extends SchemaGraph {
    // hyperedge nodes
    def reaches: Set[Reaches] = nodesAs(Reaches)
    def solves: Set[Solves] = nodesAs(Solves)

    def nodes: Set[DiscourseNode] = {
      goals ++ problems ++ ideas ++ reaches ++ solves
    }

    def relations: Set[_ <: DiscourseRelation[DiscourseNode, DiscourseNode]] = {
      subIdeas ++ subGoals ++ causes ++ prevents ++
        ideaToSolves ++ ideaToReaches ++ solvesToProblems ++ reachesToGoals ++
        supportsSolves ++ opposesSolves ++ supportsReaches ++ opposesReaches
    }

}

}


