package renesca

package object schema {

  import renesca.{graph => raw}
  import renesca.parameter.StringPropertyValue
  import renesca.parameter.implicits._

  type AbstractRelationFactoryStartEnd[START <: Node, END <: Node] = AbstractRelationFactory[START, _ <: AbstractRelation[START, END], END]
  type AbstractRelationFactoryNode[NODE <: Node] = AbstractRelationFactory[_ <: NODE, _ <: AbstractRelation[_, _], _ <: NODE]
  type AbstractRelationFactoryAny = AbstractRelationFactory[_ <: Node, _ <: AbstractRelation[_, _], _ <: Node]
  //TODO: implicits from Graph to raw.Graph

  trait NodeFilter {
    def graph: raw.Graph

    def filterNodes[T <: Node](nodes: Set[raw.Node], nodeFactory: NodeFactory[T]): Set[T] = {
      nodes.filter(_.labels.contains(nodeFactory.label)).map { node =>
        val schemaNode = nodeFactory.wrap(node)
        schemaNode.graph = graph
        schemaNode
      }
    }

    def filterRelations[START <: Node, RELATION <: Relation[START, END], END <: Node]
    (relations: Set[raw.Relation], relationFactory: RelationFactory[START, RELATION, END]): Set[RELATION] = {
      relations.filter(_.relationType == relationFactory.relationType).map(relationFactory.wrap)
    }

    def filterHyperRelations[
    START <: Node,
    STARTRELATION <: Relation[START, HYPERRELATION],
    HYPERRELATION <: HyperRelation[START, STARTRELATION, HYPERRELATION, ENDRELATION, END],
    ENDRELATION <: Relation[HYPERRELATION, END],
    END <: Node]
    (nodes: Set[raw.Node], relations: Set[raw.Relation],
     hyperRelationFactory: HyperRelationFactory[START, STARTRELATION, HYPERRELATION, ENDRELATION, END])
    : Set[HYPERRELATION] = {
      nodes.filter(_.labels.contains(hyperRelationFactory.label)).map { node =>
        val startRelation = relations.find(relation => relation.relationType == hyperRelationFactory.startRelationType && relation.endNode == node)
        val endRelation = relations.find(relation => relation.relationType == hyperRelationFactory.endRelationType && relation.startNode == node)
        hyperRelationFactory.wrap(startRelation.get, node, endRelation.get)
      }
    }
  }

  trait Graph extends NodeFilter {
    def nodesAs[T <: Node](nodeFactory: NodeFactory[T]) = {
      filterNodes(graph.nodes.toSet, nodeFactory)
    }

    def relationsAs[RELATION <: Relation[START, END], START <: Node, END <: Node]
    (relationFactory: RelationFactory[START, RELATION, END]) = {
      filterRelations(graph.relations.toSet, relationFactory)
    }

    def hyperRelationsAs[
    START <: Node,
    STARTRELATION <: Relation[START, HYPERRELATION],
    HYPERRELATION <: HyperRelation[START, STARTRELATION, HYPERRELATION, ENDRELATION, END],
    ENDRELATION <: Relation[HYPERRELATION, END],
    END <: Node]
    (hyperRelationFactory: HyperRelationFactory[START, STARTRELATION, HYPERRELATION, ENDRELATION, END]) = {
      filterHyperRelations(graph.nodes.toSet, graph.relations.toSet, hyperRelationFactory)
    }

    def add(schemaItems: Item*) {
      schemaItems.foreach {
        case hyperRelation: HyperRelation[_, _, _, _, _] =>
          graph.nodes += hyperRelation.node
          graph.relations += hyperRelation.startRelation.relation
          graph.relations += hyperRelation.endRelation.relation

        case relation: Relation[_, _] =>
          graph.relations += relation.relation

        case schemaNode: Node =>
          graph.nodes += schemaNode.node
      }
    }
  }

  trait Item

  trait Node extends Item with NodeFilter {
    def label = node.labels.head
    def node: raw.Node
    implicit var graph: raw.Graph = null

    def neighboursAs[T <: Node](nodeFactory: NodeFactory[T]) = filterNodes(node.neighbours, nodeFactory)
    def successorsAs[T <: Node](nodeFactory: NodeFactory[T]) = filterNodes(node.successors, nodeFactory)
    def predecessorsAs[T <: Node](nodeFactory: NodeFactory[T]) = filterNodes(node.predecessors, nodeFactory)
    def getStringProperty(key: String) = node.properties(key).asInstanceOf[StringPropertyValue]
  }

  trait AbstractRelation[+START <: Node, +END <: Node] extends Item {
    def startNode: START
    def endNode: END
  }

  trait Relation[+START <: Node, +END <: Node] extends AbstractRelation[START, END] {
    def relation: raw.Relation
    def relationType: raw.RelationType = relation.relationType
  }


  trait HyperRelation[
  +START <: Node,
  STARTRELATION <: Relation[START, HYPERRELATION],
  HYPERRELATION <: HyperRelation[START, STARTRELATION, HYPERRELATION, ENDRELATION, END],
  ENDRELATION <: Relation[HYPERRELATION, END],
  +END <: Node]
    extends AbstractRelation[START, END] with Node {
    // wraps a node and two relations
    protected[schema] var _startRelation: STARTRELATION = _
    protected[schema] var _endRelation: ENDRELATION = _

    def startRelation = _startRelation
    def endRelation = _endRelation
    def startNode = startRelation.startNode
    def endNode = endRelation.endNode
  }


  trait NodeFactory[+T <: Node] {
    def label: raw.Label
    def wrap(node: raw.Node): T
  }

  trait AbstractRelationFactory[+START <: Node, +RELATION <: AbstractRelation[START, END] with Item, +END <: Node] {
    def startNodeFactory: NodeFactory[START]
    def endNodeFactory: NodeFactory[END]
  }

  trait RelationFactory[+START <: Node, +RELATION <: Relation[START, END], +END <: Node] extends AbstractRelationFactory[START, RELATION, END] {
    def relationType: raw.RelationType
    def wrap(relation: raw.Relation): RELATION
  }

  trait HyperRelationFactory[
  START <: Node,
  STARTRELATION <: Relation[START, HYPERRELATION],
  HYPERRELATION <: HyperRelation[START, STARTRELATION, HYPERRELATION, ENDRELATION, END],
  ENDRELATION <: Relation[HYPERRELATION, END],
  END <: Node] extends NodeFactory[HYPERRELATION] with AbstractRelationFactory[START, HYPERRELATION, END] {

    def startRelationType: raw.RelationType
    def endRelationType: raw.RelationType

    def factory: NodeFactory[HYPERRELATION]

    def startRelationWrap(relation: raw.Relation): STARTRELATION
    def endRelationWrap(relation: raw.Relation): ENDRELATION
    def wrap(startRelation: raw.Relation, middleNode: raw.Node, endRelation: raw.Relation): HYPERRELATION = {
      val hyperRelation = wrap(middleNode)
      hyperRelation._startRelation = startRelationWrap(startRelation)
      hyperRelation._endRelation = endRelationWrap(endRelation)
      hyperRelation
    }

    def startRelationLocal(startNode: START, middleNode: HYPERRELATION): STARTRELATION = {
      startRelationWrap(raw.Relation.local(startNode.node, middleNode.node, startRelationType))
    }

    def endRelationLocal(middleNode: HYPERRELATION, endNode: END): ENDRELATION = {
      endRelationWrap(raw.Relation.local(middleNode.node, endNode.node, endRelationType))
    }
  }
}
