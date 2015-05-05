package renesca

package object schema {

  import renesca.{graph => raw}
  import renesca.parameter.StringPropertyValue
  import renesca.parameter.implicits._

  //TODO: rename STARTNODE -> START
  type AbstractRelationFactoryStartEnd[START <: Node, END <: Node] = AbstractRelationFactory[START, _ <: AbstractRelation[START, END], END]
  type AbstractRelationFactoryNode[NODE <: Node] = AbstractRelationFactory[_ <: NODE, _ <: AbstractRelation[_, _], _ <: NODE]
  type AbstractRelationFactoryAny = AbstractRelationFactory[_ <: Node, _ <: AbstractRelation[_, _], _ <: Node]

  trait NodeFilter {
    def graph: raw.Graph

    def filterNodes[T <: Node](nodes: Set[raw.Node], nodeFactory: NodeFactory[T]): Set[T] = {
      nodes.filter(_.labels.contains(nodeFactory.label)).map { node =>
        val schemaNode = nodeFactory.create(node)
        schemaNode.graph = graph
        schemaNode
      }
    }

    def filterRelations[STARTNODE <: Node, RELATION <: Relation[STARTNODE, ENDNODE], ENDNODE <: Node]
    (relations: Set[raw.Relation], relationFactory: RelationFactory[STARTNODE, RELATION, ENDNODE]): Set[RELATION] = {
      relations.filter(_.relationType == relationFactory.relationType).map(relationFactory.create)
    }

    def filterHyperRelations[
    STARTNODE <: Node,
    STARTRELATION <: Relation[STARTNODE, HYPERRELATION],
    HYPERRELATION <: HyperRelation[STARTNODE, STARTRELATION, HYPERRELATION, ENDRELATION, ENDNODE],
    ENDRELATION <: Relation[HYPERRELATION, ENDNODE],
    ENDNODE <: Node]
    (nodes: Set[raw.Node], relations: Set[raw.Relation],
     hyperRelationFactory: HyperRelationFactory[STARTNODE, STARTRELATION, HYPERRELATION, ENDRELATION, ENDNODE])
    : Set[HYPERRELATION] = {
      nodes.filter(_.labels.contains(hyperRelationFactory.label)).map { node =>
        val startRelation = relations.find(relation => relation.relationType == hyperRelationFactory.startRelationType && relation.endNode == node)
        val endRelation = relations.find(relation => relation.relationType == hyperRelationFactory.endRelationType && relation.startNode == node)
        hyperRelationFactory.create(startRelation.get, node, endRelation.get)
      }
    }
  }

  trait Graph extends NodeFilter {
    def nodesAs[T <: Node](nodeFactory: NodeFactory[T]) = {
      filterNodes(graph.nodes.toSet, nodeFactory)
    }

    def relationsAs[RELATION <: Relation[STARTNODE, ENDNODE], STARTNODE <: Node, ENDNODE <: Node]
    (relationFactory: RelationFactory[STARTNODE, RELATION, ENDNODE]) = {
      filterRelations(graph.relations.toSet, relationFactory)
    }

    def hyperRelationsAs[
    STARTNODE <: Node,
    STARTRELATION <: Relation[STARTNODE, HYPERRELATION],
    HYPERRELATION <: HyperRelation[STARTNODE, STARTRELATION, HYPERRELATION, ENDRELATION, ENDNODE],
    ENDRELATION <: Relation[HYPERRELATION, ENDNODE],
    ENDNODE <: Node]
    (hyperRelationFactory: HyperRelationFactory[STARTNODE, STARTRELATION, HYPERRELATION, ENDRELATION, ENDNODE]) = {
      filterHyperRelations(graph.nodes.toSet, graph.relations.toSet, hyperRelationFactory)
    }

    def add(schemaItem: Item) {
      schemaItem match {
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

  trait AbstractRelation[+STARTNODE <: Node, +ENDNODE <: Node] extends Item {
    def startNode: STARTNODE
    def endNode: ENDNODE
  }

  trait Relation[+STARTNODE <: Node, +ENDNODE <: Node] extends AbstractRelation[STARTNODE, ENDNODE] {
    def relation: raw.Relation
    def relationType: raw.RelationType = relation.relationType
  }


  trait HyperRelation[
  +STARTNODE <: Node,
  STARTRELATION <: Relation[STARTNODE, HYPERRELATION],
  HYPERRELATION <: HyperRelation[STARTNODE, STARTRELATION, HYPERRELATION, ENDRELATION, ENDNODE],
  ENDRELATION <: Relation[HYPERRELATION, ENDNODE],
  +ENDNODE <: Node]
    extends AbstractRelation[STARTNODE, ENDNODE] with Node {
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
    //TODO: rename to wrap
    def create(node: raw.Node): T
  }

  trait AbstractRelationFactory[+STARTNODE <: Node, +RELATION <: AbstractRelation[STARTNODE, ENDNODE] with Item, +ENDNODE <: Node] {
    def startNodeFactory: NodeFactory[STARTNODE]
    def endNodeFactory: NodeFactory[ENDNODE]
  }

  trait RelationFactory[+STARTNODE <: Node, +RELATION <: Relation[STARTNODE, ENDNODE], +ENDNODE <: Node] extends AbstractRelationFactory[STARTNODE, RELATION, ENDNODE] {
    def relationType: raw.RelationType
    def create(relation: raw.Relation): RELATION
  }

  trait HyperRelationFactory[
  STARTNODE <: Node,
  STARTRELATION <: Relation[STARTNODE, HYPERRELATION],
  HYPERRELATION <: HyperRelation[STARTNODE, STARTRELATION, HYPERRELATION, ENDRELATION, ENDNODE],
  ENDRELATION <: Relation[HYPERRELATION, ENDNODE],
  ENDNODE <: Node] extends NodeFactory[HYPERRELATION] with AbstractRelationFactory[STARTNODE, HYPERRELATION, ENDNODE] {

    def startRelationType: raw.RelationType
    def endRelationType: raw.RelationType

    def factory: NodeFactory[HYPERRELATION]

    def startRelationCreate(relation: raw.Relation): STARTRELATION
    def endRelationCreate(relation: raw.Relation): ENDRELATION
    def create(startRelation: raw.Relation, middleNode: raw.Node, endRelation: raw.Relation): HYPERRELATION = {
      val hyperRelation = create(middleNode)
      hyperRelation._startRelation = startRelationCreate(startRelation)
      hyperRelation._endRelation = endRelationCreate(endRelation)
      hyperRelation
    }

    def startRelationLocal(startNode: STARTNODE, middleNode: HYPERRELATION): STARTRELATION = {
      startRelationCreate(raw.Relation.local(startNode.node, middleNode.node, startRelationType))
    }

    def endRelationLocal(middleNode: HYPERRELATION, endNode: ENDNODE): ENDRELATION = {
      endRelationCreate(raw.Relation.local(middleNode.node, endNode.node, endRelationType))
    }
  }
}
