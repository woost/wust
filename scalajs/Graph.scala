import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExportNamed, JSExport, JSExportAll}
import collection.mutable

// http://www.scala-js.org/doc/export-to-javascript.html
object GraphAlgorithms {
  def depthFirstSearch(startNode: Node, nextNodes: Node => Traversable[Node]): Set[Node] = {
    var visited: Set[Node] = Set.empty
    visitNext(startNode)

    def visitNext(node: Node) {
      if(!(visited contains node)) {
        visited += node
        nextNodes(node).foreach(nextNodes)
      }
    }

    visited
  }

  def calculateComponent(startNode: Node): Set[Node] = {
    depthFirstSearch(startNode, _.neighbours)
  }
  def calculateDeepPredecessors(startNode: Node): Set[Node] = {
    depthFirstSearch(startNode, _.predecessors)
  }
  def calculateDeepSuccessors(startNode: Node): Set[Node] = {
    depthFirstSearch(startNode, _.successors)
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

trait NodeLike {
  val id: String
  val label: String
  def title: String
  def description: Option[String]

  import js.JSConverters._

  def $encode = js.Dynamic.literal(id = id, label = label, title = title, description.orUndefined)
}

trait NodeDelegates extends NodeLike {
  def rawNode: RawNode

  @JSExport
  val id = rawNode.id
  @JSExport
  val label = rawNode.label
  @JSExport
  def title = rawNode.title
  @JSExport
  def title_=(newTitle: String) = rawNode.title = newTitle
  @JSExport
  def description = rawNode.description
  @JSExport
  def description_=(newDescription: Option[String]) = rawNode.description = newDescription
}

@JSExport
@JSExportAll
case class Node(rawNode: RawNode) extends NodeDelegates {
  var inRelations: Set[RelationLike] = Set.empty
  var outRelations: Set[RelationLike] = Set.empty
  def relations = inRelations ++ outRelations
  def predecessors = inRelations.map(_.startNode)
  def successors = outRelations.map(_.endNode)
  def neighbours = predecessors ++ successors
  def inDegree = inRelations.size
  def outDegree = outRelations.size
  def degree = inDegree + outDegree

  import GraphAlgorithms._

  private val _component = Cacher(() => calculateComponent(this))
  def component = _component()
  private val _deepSuccessors = Cacher(() => calculateDeepSuccessors(this))
  def deepSuccessors = _deepSuccessors()
  private val _deepPredecessors = Cacher(() => calculateDeepPredecessors(this))
  def deepPredecessors = _deepPredecessors()

  def invalidate() {
    _component.invalidate()
    _deepSuccessors.invalidate()
    _deepPredecessors.invalidate()
  }
}

trait RelationLike {
  def startId: String
  def endId: String
  var startNode: Node = _
  var endNode: Node = _
  @deprecated def source = startNode
  @deprecated def target = endNode

  def $encode = js.Dynamic.literal(startId = startId, endId = endId)
}

@JSExport
@JSExportAll
case class Relation(rawRelation: RawRelation) extends RelationLike {
  def startId = rawRelation.startId
  def endId = rawRelation.endId
}

case class GraphChanges[RELATION <: RelationLike](newNodes: Set[Node], newRelations: Set[RELATION])

trait WrappedGraph[RELATION <: RelationLike] {
  def rawGraph: RawGraph
  def nodeSet: mutable.Set[Node]
  def relationSet: mutable.Set[RELATION]

  @JSExport var nodes: js.Array[Node] = _
  @JSExport @deprecated var edges: js.Array[RELATION] = _
  @JSExport var rootNode: Node = _

  // propagations upwards, coming from rawGraph
  def rawAdd(node: RawNode)
  def rawAdd(relation: RawRelation)
  def rawRemove(node: RawNode)
  def rawRemove(relation: RawRelation)
  def rawCommit(graphChanges: RawGraphChanges) {
    val changes = GraphChanges(
      graphChanges.newNodes.map(n => nodeById(n.id)).toSet,
      graphChanges.newRelations.map(r => relationByIds((r.startId, r.endId))).toSet
    )
    onCommitAction(changes)
    refreshIndex()
  }

  @JSExport
  def onCommit(f: js.Function1[GraphChanges[RELATION], Any]) { onCommitAction = f }
  var onCommitAction: Function[GraphChanges[RELATION], Any] = (x) => {}

  @JSExport
  var nodeById: Map[String, Node] = _
  @JSExport
  var relationByIds: Map[(String, String), RELATION] = _

  import js.JSConverters._

  def refreshIndex() {
    nodes = nodeSet.toJSArray
    edges = relationSet.toJSArray
    nodeById = nodeSet.map(n => n.id -> n).toMap
    relationByIds = relationSet.map(r => (r.startId, r.endId) -> r).toMap
    rootNode = nodeById(rawGraph.rootNodeId)
    for(n <- nodeSet) {
      n.invalidate()
      n.inRelations = Set.empty
      n.outRelations = Set.empty
    }
    for(r <- relationSet) {
      r.startNode = nodeById(r.startId)
      r.endNode = nodeById(r.endId)
      r.startNode.outRelations += r
      r.endNode.inRelations += r
    }
  }
}

@JSExport
@JSExportAll
case class HyperRelation(rawNode: RawNode) extends NodeDelegates with RelationLike {
  def startId = rawNode.startId.get
  def endId = rawNode.endId.get

  import js.JSConverters._

  override def $encode = js.Dynamic.literal(id = id, label = label, title = title, description.orUndefined, startId = startId, endId = endId)
}

@JSExport
@JSExportAll
class HyperGraph(val rawGraph: RawGraph) extends WrappedGraph[HyperRelation] {
  val nodeSet: mutable.Set[Node] = mutable.Set.empty ++ rawGraph.nodes.map(new Node(_))
  val hyperRelations: mutable.Set[HyperRelation] = mutable.Set.empty ++ rawGraph.nodes.filter(_.hyperEdge).map(new HyperRelation(_))
  def relationSet = hyperRelations
  refreshIndex()

  // propagate downwards
  def add(n: RecordNode) {
    rawGraph.add(new RawNode(n))
    if(n.hyperEdge.getOrElse(false)) {
      if(n.startId.isEmpty || n.endId.isEmpty)
        js.Dynamic.global.console.warn(s"Adding HyperRelation ${ n.id } with empty startId or endId.")
      rawGraph.add(new RawRelation(n.startId.get, n.id))
      rawGraph.add(new RawRelation(n.id, n.endId.get))
    }
  }
  def remove(node: Node) { rawGraph.remove(node.rawNode) }

  // propagations upwards, coming from rawGraph
  def rawAdd(node: RawNode) {
    nodeSet += new Node(node)
    if(node.hyperEdge)
      hyperRelations += new HyperRelation(node)
  }
  def rawAdd(relation: RawRelation) {}
  def rawRemove(node: RawNode) { nodeSet -= nodeById(node.id) }
  def rawRemove(relation: RawRelation) {}
}

@JSExport
@JSExportAll
class Graph(val rawGraph: RawGraph) extends WrappedGraph[Relation] {
  val nodeSet: mutable.Set[Node] = mutable.Set.empty ++ rawGraph.nodes.map(new Node(_))
  val relationSet: mutable.Set[Relation] = mutable.Set.empty ++ rawGraph.relations.map(new Relation(_))
  refreshIndex()

  // propagate downwards
  def addNode(n: RecordNode) { rawGraph.add(new RawNode(n)) }
  def addRelation(r: RecordRelation) { rawGraph.add(new RawRelation(r)) }
  def remove(node: Node) { rawGraph.remove(node.rawNode) }
  def remove(relation: Relation) { rawGraph.remove(relation.rawRelation) }

  // propagations upwards, coming from rawGraph
  def rawAdd(node: RawNode) { nodeSet += new Node(node); }
  def rawAdd(relation: RawRelation) { relationSet += new Relation(relation) }
  def rawRemove(node: RawNode) { nodeSet -= nodeById(node.id) }
  def rawRemove(relation: RawRelation) { relationSet -= relationByIds((relation.startId, relation.endId)) }

  def hyper() = rawGraph.hyperWrap()
}

@JSExport
@JSExportAll
class RawNode(val id: String, val label: String, var title: String, var description: Option[String], val hyperEdge: Boolean, val startId: Option[String], val endId: Option[String]) extends NodeLike {
  def this(n: RecordNode) = this(n.id, n.label, n.title.getOrElse(n.label), n.description.toOption, n.hyperEdge.getOrElse(false), n.startId.toOption, n.endId.toOption)
}

@JSExport
@JSExportAll
class RawRelation(val startId: String, val endId: String) extends RelationLike {
  def this(r: RecordRelation) = this(r.startId, r.endId)
}

class RawGraphChanges {
  val newNodes = mutable.Set.empty[RawNode]
  val newRelations = mutable.Set.empty[RawRelation]
}

@JSExport
@JSExportAll
class RawGraph(var nodes: Set[RawNode], var relations: Set[RawRelation], var rootNodeId: String) {
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

  var currentGraphChanges = new RawGraphChanges

  def add(node: RawNode) { nodes += node; wrappers.foreach(_.rawAdd(node)); currentGraphChanges.newNodes += node }
  def add(relation: RawRelation) { relations += relation; wrappers.foreach(_.rawAdd(relation)); currentGraphChanges.newRelations += relation }
  //TODO: add to currentGraphChanges.removedNodes
  def remove(node: RawNode) { nodes -= node; wrappers.foreach(_.rawRemove(node)) }
  def remove(relation: RawRelation) { relations -= relation; wrappers.foreach(_.rawRemove(relation)) }
  def commit() { wrappers.foreach(_.rawCommit(currentGraphChanges)); currentGraphChanges = new RawGraphChanges }

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
    println("fromRecord")
    js.Dynamic.global.console.warn("jooo")
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
