package model


import renesca.graph._
import renesca.parameter.StringPropertyValue
import renesca.parameter.implicits._

package object schema {

  trait SchemaNodeFilter {
    def graph: Graph

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

  trait SchemaGraph extends SchemaNodeFilter {
    def nodesAs[T <: SchemaNode](nodeFactory: SchemaNodeFactory[T]) = {
      filterNodes(graph.nodes.toSet, nodeFactory)
    }

    def relationsAs[RELATION <: SchemaRelation[START, END], START <: SchemaNode, END <: SchemaNode]
    (relationFactory: SchemaRelationFactory[RELATION, START, END]) = {
      filterRelations(graph.relations.toSet, relationFactory)
    }

    def add(schemaItem: SchemaItem) {
      schemaItem match {
        case hyperRelation: HyperRelation[_, _, _, _, _] =>
          graph.nodes += hyperRelation.node
          graph.relations += hyperRelation.startRelation.relation
          graph.relations += hyperRelation.endRelation.relation

        case relation: SchemaRelation[_, _] =>
          graph.relations += relation.relation

        case schemaNode: SchemaNode =>
          graph.nodes += schemaNode.node
      }
    }
  }

  sealed trait SchemaItem

  trait SchemaNode extends SchemaItem with SchemaNodeFilter {
    def label = node.labels.head
    def node: Node
    implicit var graph: Graph = null

    def neighboursAs[T <: SchemaNode](nodeFactory: SchemaNodeFactory[T]) = filterNodes(node.neighbours, nodeFactory)
    def successorsAs[T <: SchemaNode](nodeFactory: SchemaNodeFactory[T]) = filterNodes(node.successors, nodeFactory)
    def predecessorsAs[T <: SchemaNode](nodeFactory: SchemaNodeFactory[T]) = filterNodes(node.predecessors, nodeFactory)
    def getStringProperty(key: String) = node.properties(key).asInstanceOf[StringPropertyValue]
  }

  trait SchemaAbstractRelation[+START <: SchemaNode, +END <: SchemaNode] {
    def startNode: START
    def endNode: END
  }

  trait SchemaRelation[+START <: SchemaNode, +END <: SchemaNode] extends SchemaItem with SchemaAbstractRelation[START, END] {
    def relation: Relation
    def relationType: RelationType = relation.relationType
  }


  trait HyperRelation[+STARTNODE <: SchemaNode, STARTRELATION <: SchemaRelation[STARTNODE, HYPERRELATION],
  HYPERRELATION <: HyperRelation[STARTNODE, STARTRELATION, HYPERRELATION, ENDRELATION, ENDNODE],
  ENDRELATION <: SchemaRelation[HYPERRELATION, ENDNODE],
  +ENDNODE <: SchemaNode]
    extends SchemaItem with SchemaAbstractRelation[STARTNODE, ENDNODE] with SchemaNode {
    protected[schema] var _startRelation: STARTRELATION = _
    protected[schema] var _endRelation: ENDRELATION = _
    def startRelation = _startRelation
    def endRelation = _endRelation
    def startNode = startRelation.startNode
    def endNode = endRelation.endNode
  }


  trait SchemaNodeFactory[+T <: SchemaNode] {
    def label: Label
    def create(node: Node): T

    def local: T = create(Node.local(List(label)))
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

  trait SchemaHyperRelationFactory[
  STARTNODE <: SchemaNode, STARTRELATION <: SchemaRelation[STARTNODE, HYPERRELATION],
  HYPERRELATION <: HyperRelation[STARTNODE, STARTRELATION, HYPERRELATION, ENDRELATION, ENDNODE],
  ENDRELATION <: SchemaRelation[HYPERRELATION, ENDNODE], ENDNODE <: SchemaNode] extends SchemaNodeFactory[HYPERRELATION] {

    def local(startNode: STARTNODE, endNode: ENDNODE): HYPERRELATION = {
      val middleNode = super[SchemaNodeFactory].local
      middleNode._startRelation = startRelationCreate(startNode, Relation.local(startNode.node, middleNode.node, startRelationType), middleNode)
      middleNode._endRelation = endRelationCreate(middleNode, Relation.local(middleNode.node, endNode.node, endRelationType), endNode)
      middleNode
    }

    def startRelationType: RelationType
    def startRelationCreate(startNode: STARTNODE, relation: Relation, endNode: HYPERRELATION): STARTRELATION
    def endRelationType: RelationType
    def endRelationCreate(startNode: HYPERRELATION, relation: Relation, endNode: ENDNODE): ENDRELATION
  }


  object UUID {
    def applyTo[T <: SchemaNode](node: T) = {
      node.node.properties("uuid") = java.util.UUID.randomUUID.toString
      node
    }
  }
}
///////////////////////////////////////////////////

import schema._

@macros.GraphSchema
object WustSchema {
  @Schema class Discourse {
    // TODO: generate Sets for all node traits
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
    //TODO: title only belongs to ContentNode
    var title: String
    val uuid: String
  }
  //TODO: infer factory name
  @Node trait ContentNode extends DiscourseNode {def factory: ContentNodeFactory }

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
  // @HyperRelation class Achieves(startNode: Idea, endNode: Goal)

  object Solves extends SchemaHyperRelationFactory[Idea, IdeaToSolves, Solves, SolvesToProblem, Problem] {
    override def label: Label = "SOLVES"
    override def create(node: Node): Solves = new Solves(node)
    override def startRelationType = "IDEATOSOLVES"
    override def startRelationCreate(startNode: Idea, relation: Relation, endNode: Solves) = IdeaToSolves(startNode, relation, endNode)
    override def endRelationType = "SOLVESTOPROBLEM"
    override def endRelationCreate(startNode: Solves, relation: Relation, endNode: Problem) = SolvesToProblem(startNode, relation, endNode)
  }
  case class IdeaToSolves(startNode: Idea, relation: Relation, endNode: Solves) extends SchemaRelation[Idea, Solves]
  case class SolvesToProblem(startNode: Solves, relation: Relation, endNode: Problem) extends SchemaRelation[Solves, Problem]
  case class Solves(node: Node) extends HyperRelation[Idea, IdeaToSolves, Solves, SolvesToProblem, Problem] with DiscourseNode

  object Achieves extends SchemaHyperRelationFactory[Idea, IdeaToAchieves, Achieves, AchievesToGoal, Goal] {
    override def label: Label = "ACHIEVES"
    override def create(node: Node): Achieves = new Achieves(node)
    override def startRelationType = "IDEATOACHIEVES"
    override def startRelationCreate(startNode: Idea, relation: Relation, endNode: Achieves) = IdeaToAchieves(startNode, relation, endNode)
    override def endRelationType = "ACHIEVESTOGOAL"
    override def endRelationCreate(startNode: Achieves, relation: Relation, endNode: Goal) = AchievesToGoal(startNode, relation, endNode)
  }
  case class IdeaToAchieves(startNode: Idea, relation: Relation, endNode: Achieves) extends SchemaRelation[Idea, Achieves]
  case class AchievesToGoal(startNode: Achieves, relation: Relation, endNode: Goal) extends SchemaRelation[Achieves, Goal]
  case class Achieves(node: Node) extends HyperRelation[Idea, IdeaToAchieves, Achieves, AchievesToGoal, Goal] with DiscourseNode

  @Relation class SubIdea(startNode: Idea, endNode: Idea)
  @Relation class SubGoal(startNode: Goal, endNode: Goal)
  @Relation class Causes(startNode: Problem, endNode: Problem)
  @Relation class Prevents(startNode: Problem, endNode: Goal)
  @Relation class SupportsSolution(startNode: ProArgument, endNode: Solves)
  @Relation class OpposesSolution(startNode: ConArgument, endNode: Solves)
  @Relation class SupportsAchievement(startNode: ProArgument, endNode: Achieves)
  @Relation class OpposesAchievement(startNode: ConArgument, endNode: Achieves)
}

