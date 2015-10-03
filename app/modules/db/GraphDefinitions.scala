package modules.db

import scala.collection
import model.Helpers
import model.WustSchema.{ConstructRelationFactory, UuidNode}
import renesca.graph.Label
import renesca.parameter.ParameterMap
import renesca.parameter.implicits._
import renesca.schema._

package object types {

  type FixedRelationDef[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node] = RelationDefBase[START, RELATION, END, _ <: FixedNodeDef[START], _ <: FixedNodeDef[END]]
  type NodeAndFixedRelationDef[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node] = RelationDefBase[START, RELATION, END, _ <: FixedNodeDef[START], _ <: NodeDef[END]]
  type FixedAndNodeRelationDef[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node] = RelationDefBase[START, RELATION, END, _ <: NodeDef[START], _ <: FixedNodeDef[END]]
  type NodeRelationDef[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node] = RelationDefBase[START, RELATION, END, _ <: NodeDef[START], _ <: NodeDef[END]]

}

class QueryContext {
  var counter = 0
  def newVariable = {
    val tmp = counter
    counter = counter + 1
    "V" + tmp
  }

  private[db] var parameterMap: ParameterMap = Map.empty
  def params = parameterMap
}

sealed trait GraphDefinition {
  protected def ctx: QueryContext
  def toPattern: String
  final val name = ctx.newVariable
}

sealed trait NodeDef[+NODE <: Node] extends GraphDefinition
sealed trait FixedNodeDef[+NODE <: Node] extends NodeDef[NODE]

sealed trait UuidNodeDef[+NODE <: UuidNode] extends FixedNodeDef[NODE] {
  val uuid: String
  val uuidVariable = ctx.newVariable

  ctx.parameterMap += uuidVariable -> uuid
}

sealed trait LabelledUuidNodeDef[+NODE <: UuidNode] extends UuidNodeDef[NODE] {
  val labels: Set[Label]
  def toPattern = s"($name ${ labels.map(l => s":`$l`").mkString } {uuid: {$uuidVariable}})"
}

sealed trait LabelledNodeDef[+NODE <: Node] extends NodeDef[NODE] {
  val labels: Set[Label]
  def toPattern = s"($name ${ labels.map(l => s":`$l`").mkString })"
}

case class FactoryUuidNodeDef[+NODE <: UuidNode](
  factory: NodeFactory[NODE],
  uuid: String
  )(implicit val ctx: QueryContext) extends LabelledUuidNodeDef[NODE] {
  val labels = factory.labels
}

case class FactoryNodeDef[+NODE <: Node](
  factory: NodeFactory[NODE]
  )(implicit val ctx: QueryContext) extends LabelledNodeDef[NODE] {
  val labels = factory.labels
}

case class ConcreteNodeDef[+NODE <: UuidNode](
  node: NODE
  )(implicit val ctx: QueryContext) extends LabelledUuidNodeDef[NODE] {
  val uuid = node.uuid
  val labels = node.labels
}

case class LabelUuidNodeDef[+NODE <: UuidNode](
  labels: Set[Label],
  uuid: String
  )(implicit val ctx: QueryContext) extends LabelledUuidNodeDef[NODE]

case class LabelNodeDef[+NODE <: Node](
  labels: Set[Label])(implicit val ctx: QueryContext) extends LabelledNodeDef[NODE]

sealed trait HyperNodeDefBase[+NODE <: Node] extends FixedNodeDef[NODE] {
  val startName: String
  val endName: String
  val startRelationName: String
  val endRelationName: String
  val startDefinition: NodeDef[_]
  val endDefinition: NodeDef[_]
}

sealed trait RelationDefBase[
START <: Node,
RELATION <: AbstractRelation[START, END],
END <: Node,
+STARTDEF <: NodeDef[START],
+ENDDEF <: NodeDef[END]
] extends GraphDefinition {
  val startDefinition: STARTDEF
  val endDefinition: ENDDEF

  val startName = startDefinition.name
  val endName = endDefinition.name
  val startRelationName = ctx.newVariable
  val endRelationName = ctx.newVariable

  def relationMatcher: String

  protected def nodeMatcher(nodeDefinition: NodeDef[_]) = nodeDefinition match {
    case r: HyperNodeDef[_, _, _, _, _] => (Some(r.toPattern), s"(${ r.name })")
    case r                                     => (None, r.toPattern)
  }

  protected def nodeReferencer(nodeDefinition: NodeDef[_]) = s"(${ nodeDefinition.name })"

  def toPattern: String = toPattern(true)
  def toPattern(matchNodes: Boolean): String = toPattern(matchNodes, matchNodes)
  def toPattern(matchStart: Boolean, matchEnd: Boolean): String = {
    val (startPre, startNode) = if(matchStart)
                                  nodeMatcher(startDefinition)
                                else
                                  (None, nodeReferencer(startDefinition))

    val (endPre, endNode) = if(matchEnd)
                              nodeMatcher(endDefinition)
                            else
                              (None, nodeReferencer(endDefinition))

    val preMatcher = List(startPre, endPre).flatten.map(_ + ",").mkString
    s"$preMatcher$startNode-$relationMatcher->$endNode"
  }
}

sealed trait SingleRelationDefBase[
START <: Node,
RELATION <: AbstractRelation[START, END],
END <: Node,
+STARTDEF <: NodeDef[START],
+ENDDEF <: NodeDef[END]
] extends RelationDefBase[START,RELATION,END,STARTDEF,ENDDEF] {
  val factory: AbstractRelationFactory[START, RELATION, END]

  protected def relationPropertyMatcher: String

  def relationMatcher = factory match {
    case r: RelationFactory[_, RELATION, _]            => s"[$name :`${ r.relationType }`]"
    case r: HyperRelationFactory[_, _, RELATION, _, _] => s"[$startRelationName:`${ r.startRelationType }`]->($name ${ r.labels.map(l => s":`$l`").mkString } ${ relationPropertyMatcher })-[$endRelationName:`${ r.endRelationType }`]"
  }
}

case class HyperNodeDef[
START <: Node,
RELATION <: AbstractRelation[START, END] with UuidNode,
END <: Node,
STARTDEF <: NodeDef[START],
ENDDEF <: NodeDef[END]
](
  startDefinition: STARTDEF,
  factory: AbstractRelationFactory[START, RELATION, END] with NodeFactory[RELATION],
  endDefinition: ENDDEF,
  nodeUuid: Option[String] = None)(implicit val ctx: QueryContext) extends HyperNodeDefBase[RELATION] with SingleRelationDefBase[START, RELATION, END, STARTDEF, ENDDEF] {

  val uuidVariable = ctx.newVariable
  protected def relationPropertyMatcher = nodeUuid.map(uuid => s"{uuid: {$uuidVariable}}").getOrElse("")

  nodeUuid.foreach(uuid => ctx.parameterMap += uuidVariable -> uuid)
}

case class RelationDef[
START <: Node,
RELATION <: AbstractRelation[START, END],
END <: Node,
STARTDEF <: NodeDef[START],
ENDDEF <: NodeDef[END]
](
  startDefinition: STARTDEF,
  factory: AbstractRelationFactory[START, RELATION, END],
  endDefinition: ENDDEF)(implicit val ctx: QueryContext) extends SingleRelationDefBase[START, RELATION, END, STARTDEF, ENDDEF] {

  val relationPropertyMatcher = ""
}
