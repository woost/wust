package renesca.js

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportAll}
import js.JSConverters._
import js.Dynamic.{global => g}

// http://www.scala-js.org/doc/export-to-javascript.html
object GraphAlgorithms {
  def depthFirstSearch(startNode: NodeBase, nextNodes: NodeBase => Traversable[NodeBase]): Set[NodeBase] = {
    var visited: Set[NodeBase] = Set.empty
    visitNext(startNode)

    def visitNext(node: NodeBase) {
      if(!(visited contains node)) {
        visited += node
        nextNodes(node).foreach(visitNext)
      }
    }

    visited
  }

  def calculateComponent(startNode: NodeBase): Set[NodeBase] = {
    depthFirstSearch(startNode, _.neighbours)
  }
  def calculateDeepPredecessors(startNode: NodeBase): Set[NodeBase] = {
    depthFirstSearch(startNode, _.predecessors) - startNode
  }
  def calculateDeepSuccessors(startNode: NodeBase): Set[NodeBase] = {
    depthFirstSearch(startNode, _.successors) - startNode
  }
}

case class Cacher[O](func: () => O) {
  var cached: Option[O] = None
  def apply(): O = {
    if(cached.isEmpty)
      cached = Some(func())
    cached.get
  }
  def invalidate() {
    cached = None
  }
}

sealed trait NodeLike {
  val id: String
  val label: String
  def title: String
  def description: Option[String]
}

sealed trait NodeDelegates extends NodeLike {
  def rawNode: RawNode

  @JSExport
  val id = rawNode.id
  @JSExport
  val label = rawNode.label
  @JSExport
  val hyperEdge = rawNode.hyperEdge
  @JSExport
  def title = rawNode.title
  @JSExport
  def title_=(newTitle: String) = rawNode.title = newTitle

  def description = rawNode.description
  def description_=(newDescription: Option[String]) = { rawNode.description = newDescription }


  @JSExport("description")
  def descriptionJs = description.orUndefined
  @JSExport("description_=")
  def descriptionJs_=(newDescription: js.UndefOr[String]) = { description = newDescription.toOption }

  @JSExport def startId = rawNode.startId.get
  @JSExport def endId = rawNode.endId.get
}

trait NodeBase extends NodeDelegates {
  var inRelations: Set[RelationLike] = Set.empty
  var outRelations: Set[RelationLike] = Set.empty
  def relations = inRelations ++ outRelations
  def predecessors = inRelations.map(_.startNode)
  def successors = outRelations.map(_.endNode)
  def neighbours = predecessors ++ successors
  @JSExport def inDegree = inRelations.size
  @JSExport def outDegree = outRelations.size
  @JSExport def degree = inDegree + outDegree

  @JSExport("inRelations") def inRelationsJs = inRelations.toJSArray
  @JSExport("outRelations") def outRelationsJs = outRelations.toJSArray
  @JSExport("relations") def relationsJs = relations.toJSArray
  @JSExport("predecessors") def predecessorsJs = predecessors.toJSArray
  @JSExport("successors") def successorsJs = successors.toJSArray
  @JSExport("neighbours") def neighboursJs = neighbours.toJSArray

  import GraphAlgorithms._

  val _component = Cacher(() => calculateComponent(this))
  def component = _component()
  val _componentJs = Cacher(() => calculateComponent(this).toJSArray)
  @JSExport("component") def componentJs = _componentJs()

  val _deepSuccessors = Cacher(() => calculateDeepSuccessors(this))
  def deepSuccessors = _deepSuccessors()
  val _deepSuccessorsJs = Cacher(() => calculateDeepSuccessors(this).toJSArray)
  @JSExport("deepSuccessors") def deepSuccessorsJs = _deepSuccessorsJs()

  val _deepPredecessors = Cacher(() => calculateDeepPredecessors(this))
  def deepPredecessors = _deepPredecessors()
  val _deepPredecessorsJs = Cacher(() => calculateDeepPredecessors(this).toJSArray)
  @JSExport("deepPredecessors") def deepPredecessorsJs = _deepPredecessorsJs()

  def invalidate() {
    _component.invalidate()
    _componentJs.invalidate()
    _deepSuccessors.invalidate()
    _deepSuccessorsJs.invalidate()
    _deepPredecessors.invalidate()
    _deepPredecessorsJs.invalidate()
  }
}

@JSExport
class Node(val rawNode: RawNode) extends NodeBase {
  @JSExport def encode() = js.Dynamic.literal(id = id, label = label, title = title, description.orUndefined)
}

sealed trait RelationLike {
  def startId: String
  def endId: String
  var startNode: NodeBase = _
  var endNode: NodeBase = _

}

@JSExport
@JSExportAll
case class Relation(rawRelation: RawRelation) extends RelationLike {
  def startId = rawRelation.startId
  def endId = rawRelation.endId

  @JSExport("startNode") def _startNode: NodeBase = startNode
  @JSExport("endNode") def _endNode: NodeBase = endNode
  @JSExport def source = startNode
  @JSExport def target = endNode

  @JSExport def encode() = js.Dynamic.literal(startId = startId, endId = endId)
}

case class GraphChanges[RELATION <: RelationLike](newNodes: Set[NodeBase], newRelations: Set[RELATION]) {
  @JSExport("newNodes") val newNodesJs = newNodes.toJSArray
  @JSExport("newRelations") val newRelationsJs = newRelations.toJSArray
}

sealed trait WrappedGraph[RELATION <: RelationLike] {
  private[js] def rawGraph: RawGraph
  private[js] def nodes: mutable.Set[NodeBase]
  private[js] def relations: mutable.Set[RELATION]

  var nodesJs: js.Array[NodeBase] = _
  var relationsJs: js.Array[RELATION] = _
  var nonHyperRelationNodes: js.Array[Node] = _
  var rootNode: NodeBase = _ // eigentlich soll das auch ne node sein...egal

  // propagations upwards, coming from rawGraph
  private[js] def rawAdd(node: RawNode)
  private[js] def rawAdd(relation: RawRelation)
  private[js] def rawRemove(node: RawNode)
  private[js] def rawRemove(relation: RawRelation)
  private[js] def wrapRawChanges(rawChanges: RawGraphChanges): GraphChanges[RELATION]
  private[js] def rawCommit(graphChanges: RawGraphChanges) {
    refreshIndex()
    val changes = wrapRawChanges(graphChanges)
    onCommitActions.foreach(_(changes))
  }

  @JSExport
  def onCommit(f: js.Function1[GraphChanges[RELATION], Any]) { onCommitActions += f }
  val onCommitActions: mutable.ArrayBuffer[Function[GraphChanges[RELATION], Any]] = mutable.ArrayBuffer.empty

  @JSExport
  def commit() { rawGraph.commit() }

  var nodeById: Map[String, NodeBase] = _
  var relationByIds: Map[(String, String), RELATION] = _

  @JSExport("nodeById") def nodeByIdJs(id: String) = nodeById(id)
  @JSExport("relationByIds") def relationByIdsJs(startId: String, endId: String) = {
    relationByIds((startId, endId))
  }


  private[js] def refreshIndex() {
    nodeById = nodes.map(n => n.id -> n).toMap
    relationByIds = relations.map(r => (r.startId, r.endId) -> r).toMap
    rootNode = nodeById.get(rawGraph.rootNodeId).getOrElse(null) //TODO: now what?
    for(n <- nodes) {
      n.invalidate()
      n.inRelations = Set.empty
      n.outRelations = Set.empty
    }
    for(r <- relations) {
      r.startNode = nodeById(r.startId)
      r.endNode = nodeById(r.endId)
      r.startNode.outRelations += r
      r.endNode.inRelations += r
    }
    nodesJs = nodes.toJSArray
    relationsJs = relations.toJSArray
    nonHyperRelationNodes = nodes.collect{case n:Node => n}.toJSArray
  }
}

@JSExport
case class HyperRelation(rawNode: RawNode) extends NodeBase with RelationLike {
  @JSExport override def startId = rawNode.startId.get
  @JSExport override def endId = rawNode.endId.get

  @JSExport("startNode") def _startNode: NodeBase = startNode
  @JSExport("endNode") def _endNode: NodeBase = endNode
  @JSExport def source = startNode
  @JSExport def target = endNode

  @JSExport def encode() = js.Dynamic.literal(id = id, label = label, title = title, description.orUndefined, startId = startId, endId = endId)
}

object Node {
    def apply(node: RawNode) = {
        if (node.hyperEdge)
            new HyperRelation(node)
        else
            new Node(node)
    }
}

@JSExport
class HyperGraph(val rawGraph: RawGraph) extends WrappedGraph[HyperRelation] {
  // hyperrelations are added to the nodeset as well as to the relationset,
  // because they are used in both ways:
  //  1) as a relation which connects two nodes (or hyperrelations)
  //  2) as a node which is the start- or endnode (nodeById has to be mapped) of a relation
  val nodes: mutable.Set[NodeBase] = mutable.Set.empty ++ rawGraph.nodes.map(Node(_))
  val hyperRelations: mutable.Set[HyperRelation] = mutable.Set.empty ++ nodes.collect { case n: HyperRelation => n }
  def relations = hyperRelations
  @JSExport("relations") def _relationsJs = relationsJs
  @JSExport("nodes") def _nodesJs = nodesJs
  @JSExport("rootNode") def _rootNode = rootNode
  refreshIndex()

  override def wrapRawChanges(rawChanges: RawGraphChanges): GraphChanges[HyperRelation] = {
    GraphChanges(
      rawChanges.newNodes.map(n => nodeById(n.id)).toSet,
      //TODO: this should be the general solution and can be implemented in WrappedGraph
      rawChanges.newRelations.flatMap(r => relationByIds.get(r.startId -> r.endId)).toSet ++
        rawChanges.newNodes.filter(_.hyperEdge).map(n => relationByIds(n.startId.get -> n.endId.get))
    )
  }

  // propagate downwards
  @JSExport
  def add(n: RecordNode) {
    add(new RawNode(n))
  }

  def add(n: RawNode) {
    rawGraph.add(n)
    if(n.hyperEdge) {
      if(n.startId.isEmpty || n.endId.isEmpty)
        g.console.warn(s"Adding HyperRelation ${ n.id } with empty startId or endId.")
      rawGraph.add(new RawRelation(n.startId.get, n.id))
      rawGraph.add(new RawRelation(n.id, n.endId.get))
    }
  }

  @JSExport
  def removeNode(id: String) { rawGraph.remove(nodeById(id).rawNode) }
  @JSExport
  def removeRelation(startId: String, endId: String) { rawGraph.remove(relationByIds(startId -> endId).rawNode) }

  // propagations upwards, coming from rawGraph
  private[js] def rawAdd(rawNode: RawNode) {
    val node = Node(rawNode)
    nodes += node
    if(node.hyperEdge)
      hyperRelations += node.asInstanceOf[HyperRelation]
  }
  private[js] def rawAdd(relation: RawRelation) {}
  private[js] def rawRemove(node: RawNode) {
    nodes -= nodeById(node.id)
    if (node.hyperEdge)
      relations -= relationByIds(node.startId.get -> node.endId.get)
  }
  private[js] def rawRemove(relation: RawRelation) {}
}

@JSExport // überflüssig, da graph aus der factory kommt
//nein nich ueberfluessig, der graph muss exposed sein.
class Graph(private[js] val rawGraph: RawGraph) extends WrappedGraph[Relation] {
  val nodes: mutable.Set[NodeBase] = mutable.Set.empty ++ rawGraph.nodes.map(Node(_))
  val relations: mutable.Set[Relation] = mutable.Set.empty ++ rawGraph.relations.map(new Relation(_))
  var hyperRelations: mutable.Set[HyperRelation] = _
  @JSExport("hyperRelations") var hyperRelationsJs: js.Array[HyperRelation] = _
  @JSExport("relations") def _relationsJs = relationsJs
  @JSExport def edges = relationsJs
  @JSExport("nodes") def _nodesJs = nodesJs
  @JSExport("nonHyperRelationNodes") def _nonHyperRelationNodes = nonHyperRelationNodes
  @JSExport("rootNode") def _rootNode = rootNode

  //TODO: d3_graph needs to sort the nodes: first hyperedges, then nodes...
  @JSExport def setNodes(newNodes: js.Array[NodeBase]) = nodesJs = newNodes

  override def wrapRawChanges(rawChanges: RawGraphChanges): GraphChanges[Relation] = {
    GraphChanges(
      rawChanges.newNodes.map(n => nodeById(n.id)).toSet,
      rawChanges.newRelations.map(r => relationByIds((r.startId, r.endId))).toSet
    )
  }
  override def refreshIndex() {
    super.refreshIndex()
    hyperRelations = nodes.collect { case n: HyperRelation => n }
    for(r <- hyperRelations) {
      r.startNode = nodeById(r.startId)
      r.endNode = nodeById(r.endId)
    }
    hyperRelationsJs = hyperRelations.toJSArray
  }
  refreshIndex()

  // propagate downwards
  @JSExport
  def addNode(n: RecordNode) { addNode(new RawNode(n)) }
  @JSExport
  def addNode(n: RawNode) { rawGraph.add(n) }
  @JSExport
  def addRelation(r: RecordRelation) { addRelation(new RawRelation(r)) }
  @JSExport
  def addRelation(r: RawRelation) { rawGraph.add(r) }

  @JSExport
  def removeNode(id: String) {
    rawGraph.remove(nodeById(id).rawNode)
  }
  @JSExport
  def removeRelation(startId: String, endId: String) { rawGraph.remove(relationByIds(startId -> endId).rawRelation) }

  // propagations upwards, coming from rawGraph
  def rawAdd(node: RawNode) { nodes += Node(node); }
  def rawAdd(relation: RawRelation) { relations += new Relation(relation) }
  def rawRemove(node: RawNode) { nodes -= nodeById(node.id) }
  def rawRemove(relation: RawRelation) { relations -= relationByIds((relation.startId, relation.endId)) }

  @JSExport
  def hyper() = rawGraph.hyperWrap()
}

@JSExport
@JSExportAll
class RawNode(val id: String, val label: String, var title: String, var description: Option[String], val hyperEdge: Boolean, val startId: Option[String], val endId: Option[String]) {
  def this(n: RecordNode) = this(n.id, n.label, n.title.getOrElse(n.label), n.description.toOption, n.hyperEdge.getOrElse(false), n.startId.toOption, n.endId.toOption)
  override def toString = s"RawNode($id)"

  def canEqual(other: Any): Boolean = other.isInstanceOf[RawNode]

  override def equals(other: Any): Boolean = other match {
    case that: RawNode => (that canEqual this) && this.id == that.id
    case _             => false
  }

  override def hashCode: Int = id.hashCode
}

@JSExport
@JSExportAll
class RawRelation(val startId: String, val endId: String) {
  def this(r: RecordRelation) = this(r.startId, r.endId)
  override def toString = s"RawRelation($startId -> $endId)"

  def canEqual(other: Any): Boolean = other.isInstanceOf[RawRelation]

  override def equals(other: Any): Boolean = other match {
    case that: RawRelation => (that canEqual this) && this.startId == that.startId && this.endId == that.endId
    case _                 => false
  }

  override def hashCode: Int = Seq(startId, endId).hashCode
}

case class RawGraphChanges(newNodes: Set[RawNode], newRelations: Set[RawRelation])

@JSExport
@JSExportAll
class RawGraph(private[js] var nodes: Set[RawNode], private[js] var relations: Set[RawRelation], var rootNodeId: String) {
  val wrappers = mutable.Set.empty[WrappedGraph[_]]
  def wrap(): Graph = {
    val graph = new Graph(this)
    wrappers += graph
    graph
  }

  def hyperWrap(): HyperGraph = {
    val graph = new HyperGraph(this)
    wrappers += graph
    graph
  }

  var currentNewNodes = Set.empty[RawNode]
  var currentNewRelations = Set.empty[RawRelation]

  def add(node: RawNode) {
    if(nodes.contains(node))
      return

    nodes += node
    wrappers.foreach(_.rawAdd(node));
    currentNewNodes += node
  }
  def add(relation: RawRelation) {
    if(relations.contains(relation))
      return

    relations += relation
    wrappers.foreach(_.rawAdd(relation))
    currentNewRelations += relation
  }
  //TODO: add to currentGraphChanges.removedNodes/relations
  def remove(node: RawNode) {
    nodes -= node
    wrappers.foreach(_.rawRemove(node))

    incidentRelationsWithoutHyperHelperRelations(node).foreach(remove)
    incidentHyperRelations(node).foreach(remove)
    if( nodes.size == 0) {
      g.console.warn(s"""Deleted last node""")
    }
    else {
      //TODO!: what should happen when removing the rootNode?
      if(node.id == rootNodeId) {
        rootNodeId = nodes.head.id
        g.console.warn(s"""Deleted rootNode, rootNode is now: ${nodes.head.id} - "${nodes.head.title}"""")
      }
    }
  }
  def remove(relation: RawRelation) {
    relations -= relation
    wrappers.foreach(_.rawRemove(relation))
  }

  def commit() {
    val changes = RawGraphChanges(currentNewNodes, currentNewRelations)
    wrappers.foreach(_.rawCommit(changes))
    currentNewNodes = Set.empty[RawNode]
    currentNewRelations = Set.empty[RawRelation]
  }

  //TODO: cache lookup maps/sets
  def hyperRelations = nodes.filter(_.hyperEdge)
  def hyperHelperRelations: Set[RawRelation] = {
    val hyperIds: Set[String] = hyperRelations.map(_.id)
    val hyperIdToStartId: Map[String, String] = hyperRelations.map(h => h.id -> h.startId.get).toMap
    val hyperIdToEndId: Map[String, String] = hyperRelations.map(h => h.id -> h.endId.get).toMap
    relations.filter(
      r => ((hyperIds contains r.startId) && r.endId == hyperIdToEndId(r.startId)) ||
        ((hyperIds contains r.endId) && r.startId == hyperIdToStartId(r.endId))
    )
  }

  def incidentRelations(node: RawNode): Set[RawRelation] = {
    relations.filter(r => r.startId == node.id || r.endId == node.id)
  }

  def incidentRelationsWithoutHyperHelperRelations(node: RawNode): Set[RawRelation] = {
    incidentRelations(node) diff hyperHelperRelations
  }

  def incidentHyperRelations(node: RawNode): Set[RawNode] = {
    nodes.filter(n => n.hyperEdge && (n.startId.get == node.id || n.endId.get == node.id))
  }


  override def toString = s"Graph(${ nodes.map(_.id).mkString(",") }, ${ relations.map(r => r.startId + "->" + r.endId).mkString(",") })"
}

trait RecordNode extends js.Object {
  def id: String = js.native
  def label: String = js.native
  def title: js.UndefOr[String] = js.native
  def description: js.UndefOr[String] = js.native

  def hyperEdge: js.UndefOr[Boolean] = js.native
  def startId: js.UndefOr[String] = js.native
  def endId: js.UndefOr[String] = js.native
}

trait RecordRelation extends js.Object {
  def startId: String = js.native
  def endId: String = js.native
}

trait RecordGraph extends js.Object {
  def nodes: js.Array[RecordNode] = js.native
  def edges: js.Array[RecordRelation] = js.native

  def $pk: String = js.native // rootNodeId
}

@JSExport
object GraphFactory {


  @JSExport
  def fromRecord(record: RecordGraph): RawGraph = {
    fromTraversable(
      record.nodes.map(n => new RawNode(n)),
      record.edges.map(r => new RawRelation(r)),
      record.$pk // rootNodeId
    )
  }

  def fromTraversable(nodes: Traversable[RawNode], relations: Traversable[RawRelation], rootNodeId: String): RawGraph = {
    new RawGraph(nodes.toSet, relations.toSet, rootNodeId)
  }
}
