package renesca
package object schema {
  import renesca.graph._
  import renesca.parameter.StringPropertyValue
  import renesca.parameter.implicits._

  type SchemaAbstractRelationFactoryStartEnd[START <: SchemaNode, END <: SchemaNode] = SchemaAbstractRelationFactory[START, _ <: SchemaAbstractRelation[START,END], END]
  type SchemaAbstractRelationFactoryNode[NODE <: SchemaNode] = SchemaAbstractRelationFactory[_ <: NODE, _ <: SchemaAbstractRelation[_,_], _ <: NODE]
  type SchemaAbstractRelationFactoryAny = SchemaAbstractRelationFactory[_ <: SchemaNode, _ <: SchemaAbstractRelation[_,_], _ <: SchemaNode]

  trait SchemaNodeFilter {
    def graph: Graph

    def filterNodes[T <: SchemaNode](nodes: Set[Node], nodeFactory: SchemaNodeFactory[T]): Set[T] = {
      nodes.filter(_.labels.contains(nodeFactory.label)).map { node =>
        val schemaNode = nodeFactory.create(node)
        schemaNode.graph = graph
        schemaNode
      }
    }

    def filterRelations[STARTNODE <: SchemaNode, RELATION <: SchemaRelation[STARTNODE, ENDNODE], ENDNODE <: SchemaNode]
    (relations: Set[Relation], relationFactory: SchemaRelationFactory[STARTNODE, RELATION, ENDNODE]): Set[RELATION] = {
      relations.filter(_.relationType == relationFactory.relationType).map(relationFactory.create)
    }

    def filterHyperRelations[
    STARTNODE <: SchemaNode,
    STARTRELATION <: SchemaRelation[STARTNODE, HYPERRELATION],
    HYPERRELATION <: SchemaHyperRelation[STARTNODE, STARTRELATION, HYPERRELATION, ENDRELATION, ENDNODE],
    ENDRELATION <: SchemaRelation[HYPERRELATION, ENDNODE],
    ENDNODE <: SchemaNode]
    (nodes: Set[Node], relations: Set[Relation],
      hyperRelationFactory: SchemaHyperRelationFactory[STARTNODE, STARTRELATION, HYPERRELATION, ENDRELATION, ENDNODE])
    : Set[HYPERRELATION] = {
      nodes.filter(_.labels.contains(hyperRelationFactory.label)).map { node =>
        val startRelation = relations.find(relation => relation.relationType == hyperRelationFactory.startRelationType && relation.endNode == node)
        val endRelation = relations.find(relation => relation.relationType == hyperRelationFactory.endRelationType && relation.startNode == node)
        hyperRelationFactory.create(startRelation.get, node, endRelation.get)
      }
    }
  }

  trait SchemaGraph extends SchemaNodeFilter {
    def nodesAs[T <: SchemaNode](nodeFactory: SchemaNodeFactory[T]) = {
      filterNodes(graph.nodes.toSet, nodeFactory)
    }

    def relationsAs[RELATION <: SchemaRelation[STARTNODE, ENDNODE], STARTNODE <: SchemaNode, ENDNODE <: SchemaNode]
    (relationFactory: SchemaRelationFactory[STARTNODE, RELATION, ENDNODE]) = {
      filterRelations(graph.relations.toSet, relationFactory)
    }

    def hyperRelationsAs[
    STARTNODE <: SchemaNode,
    STARTRELATION <: SchemaRelation[STARTNODE, HYPERRELATION],
    HYPERRELATION <: SchemaHyperRelation[STARTNODE, STARTRELATION, HYPERRELATION, ENDRELATION, ENDNODE],
    ENDRELATION <: SchemaRelation[HYPERRELATION, ENDNODE],
    ENDNODE <: SchemaNode]
    (hyperRelationFactory: SchemaHyperRelationFactory[STARTNODE, STARTRELATION, HYPERRELATION, ENDRELATION, ENDNODE]) = {
      filterHyperRelations(graph.nodes.toSet, graph.relations.toSet, hyperRelationFactory)
    }

    def add(schemaItem: SchemaItem) {
      schemaItem match {
        case hyperRelation: SchemaHyperRelation[_, _, _, _, _] =>
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

  sealed trait SchemaAbstractRelation[+STARTNODE <: SchemaNode, +ENDNODE <: SchemaNode] extends SchemaItem {
    def startNode: STARTNODE
    def endNode: ENDNODE
  }

  trait SchemaRelation[+STARTNODE <: SchemaNode, +ENDNODE <: SchemaNode] extends SchemaAbstractRelation[STARTNODE, ENDNODE] {
    def relation: Relation
    def relationType: RelationType = relation.relationType
  }


  trait SchemaHyperRelation[
  +STARTNODE <: SchemaNode,
  STARTRELATION <: SchemaRelation[STARTNODE, HYPERRELATION],
  HYPERRELATION <: SchemaHyperRelation[STARTNODE, STARTRELATION, HYPERRELATION, ENDRELATION, ENDNODE],
  ENDRELATION <: SchemaRelation[HYPERRELATION, ENDNODE],
  +ENDNODE <: SchemaNode]
  extends SchemaAbstractRelation[STARTNODE, ENDNODE] with SchemaNode {
    // wraps a node and two relations
    protected[schema] var _startRelation: STARTRELATION = _
    protected[schema] var _endRelation: ENDRELATION = _

    def startRelation = _startRelation
    def endRelation = _endRelation
    def startNode = startRelation.startNode
    def endNode = endRelation.endNode
  }


  trait SchemaNodeFactory[+T <: SchemaNode] {
    def label: Label
    //TODO: rename to wrap
    def create(node: Node): T
    protected def local: T = create(Node.local(List(label)))
  }

  trait SchemaAbstractRelationFactory[STARTNODE <: SchemaNode, RELATION <: SchemaAbstractRelation[STARTNODE, ENDNODE] with SchemaItem, ENDNODE <: SchemaNode] {
    // def local(startNode: STARTNODE, endNode: ENDNODE): RELATION
    def startNodeFactory: SchemaNodeFactory[STARTNODE]
    def endNodeFactory: SchemaNodeFactory[ENDNODE]
  }

  trait SchemaRelationFactory[STARTNODE <: SchemaNode, RELATION <: SchemaRelation[STARTNODE, ENDNODE], ENDNODE <: SchemaNode] extends SchemaAbstractRelationFactory[STARTNODE, RELATION, ENDNODE] {
    def relationType: RelationType
    def create(relation: Relation): RELATION
    // def local(startNode: STARTNODE, endNode: ENDNODE): RELATION = {
    //   create(Relation.local(startNode.node, endNode.node, relationType))
    // }
  }

  trait SchemaHyperRelationFactory[
  STARTNODE <: SchemaNode,
  STARTRELATION <: SchemaRelation[STARTNODE, HYPERRELATION],
  HYPERRELATION <: SchemaHyperRelation[STARTNODE, STARTRELATION, HYPERRELATION, ENDRELATION, ENDNODE],
  ENDRELATION <: SchemaRelation[HYPERRELATION, ENDNODE],
  ENDNODE <: SchemaNode] extends SchemaNodeFactory[HYPERRELATION] with SchemaAbstractRelationFactory[STARTNODE, HYPERRELATION, ENDNODE] {

    def startRelationType: RelationType
    def endRelationType: RelationType

    def factory: SchemaNodeFactory[HYPERRELATION]

    def startRelationCreate(relation: Relation): STARTRELATION
    def endRelationCreate(relation: Relation): ENDRELATION
    def create(startRelation: Relation, middleNode: Node, endRelation: Relation): HYPERRELATION = {
      val hyperRelation = create(middleNode)
      hyperRelation._startRelation = startRelationCreate(startRelation)
      hyperRelation._endRelation = endRelationCreate(endRelation)
      hyperRelation
    }

    def startRelationLocal(startNode: STARTNODE, middleNode: HYPERRELATION): STARTRELATION = {
      startRelationCreate(Relation.local(startNode.node, middleNode.node, startRelationType))
    }

    def endRelationLocal(middleNode: HYPERRELATION, endNode: ENDNODE): ENDRELATION = {
      endRelationCreate(Relation.local(middleNode.node, endNode.node, endRelationType))
    }

    // def local(startNode: STARTNODE, endNode: ENDNODE): HYPERRELATION = {
    //   val middleNode = super[SchemaNodeFactory].local
    //   create(startRelationLocal(startNode, middleNode).relation, middleNode.node, endRelationLocal(middleNode, endNode).relation)
    // }
  }
}
