package modules.db

import model.Helpers
import model.WustSchema.{ConstructRelationFactory, UuidNode}
import renesca.graph.Label
import renesca.parameter.ParameterMap
import renesca.parameter.implicits._
import renesca.schema._

package object types {

  type FixedRelationDefinition[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node] = RelationDefinitionBase[START, RELATION, END, _ <: FixedNodeDefinition[START], _ <: FixedNodeDefinition[END]]
  type NodeAndFixedRelationDefinition[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node] = RelationDefinitionBase[START, RELATION, END, _ <: FixedNodeDefinition[START], _ <: NodeDefinition[END]]
  type FixedAndNodeRelationDefinition[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node] = RelationDefinitionBase[START, RELATION, END, _ <: NodeDefinition[START], _ <: FixedNodeDefinition[END]]
  type NodeRelationDefinition[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node] = RelationDefinitionBase[START, RELATION, END, _ <: NodeDefinition[START], _ <: NodeDefinition[END]]

}

class QueryContext {
  var counter = 0
  def newVariable = {
    val tmp = counter
    counter = counter + 1
    "V" + tmp
  }
}

sealed trait GraphDefinition {
  protected def ctx: QueryContext
  def toQuery: String
  def parameterMap: ParameterMap = Map.empty
  final val name = ctx.newVariable
}

sealed trait NodeDefinition[+NODE <: Node] extends GraphDefinition
sealed trait FixedNodeDefinition[+NODE <: Node] extends NodeDefinition[NODE]

sealed trait UuidNodeDefinition[+NODE <: UuidNode] extends FixedNodeDefinition[NODE] {
  val uuid: String
  val uuidVariable = ctx.newVariable
  override def parameterMap = Map(uuidVariable -> uuid)
}

sealed trait LabelledUuidNodeDefinition[+NODE <: UuidNode] extends UuidNodeDefinition[NODE] {
  val labels: Set[Label]
  def toQuery = s"($name ${ labels.map(l => s":`$l`").mkString } {uuid: {$uuidVariable}})"
}

sealed trait LabelledNodeDefinition[+NODE <: Node] extends NodeDefinition[NODE] {
  val labels: Set[Label]
  def toQuery = s"($name ${ labels.map(l => s":`$l`").mkString })"
}

case class FactoryUuidNodeDefinition[+NODE <: UuidNode](
  factory: NodeFactory[NODE],
  uuid: String
  )(implicit val ctx: QueryContext) extends LabelledUuidNodeDefinition[NODE] {
  val labels = factory.labels
}

case class ConcreteFactoryNodeDefinition[+NODE <: Node](
  factory: NodeFactory[NODE]
  )(implicit val ctx: QueryContext) extends LabelledNodeDefinition[NODE] {
  val labels = factory.labels
}

case class ConcreteNodeDefinition[+NODE <: UuidNode](
  node: NODE
  )(implicit val ctx: QueryContext) extends LabelledUuidNodeDefinition[NODE] {
  val uuid = node.uuid
  val labels = node.labels
}

case class LabelUuidNodeDefinition[+NODE <: UuidNode](
  labels: Set[Label],
  uuid: String
  )(implicit val ctx: QueryContext) extends LabelledUuidNodeDefinition[NODE]

case class LabelNodeDefinition[+NODE <: Node](
  labels: Set[Label])(implicit val ctx: QueryContext) extends LabelledNodeDefinition[NODE]

sealed trait HyperNodeDefinitionBase[+NODE <: Node] extends FixedNodeDefinition[NODE] {
  val startName: String
  val endName: String
  val startRelationName: String
  val endRelationName: String
  val startDefinition: NodeDefinition[_]
  val endDefinition: NodeDefinition[_]
}

sealed trait RelationDefinitionBase[
START <: Node,
RELATION <: AbstractRelation[START, END],
END <: Node,
+STARTDEF <: NodeDefinition[START],
+ENDDEF <: NodeDefinition[END]
] extends GraphDefinition {
  val startDefinition: STARTDEF
  val endDefinition: ENDDEF
  val nodeUuid: Option[String]
  val uuidVariable = ctx.newVariable

  val startName = startDefinition.name
  val endName = endDefinition.name
  val startRelationName = ctx.newVariable
  val endRelationName = ctx.newVariable

  def relationMatcher: String

  protected def nodeUuidMatcher = nodeUuid.map(uuid => s"{uuid: {$uuidVariable}}").getOrElse("")

  protected def nodeMatcher(nodeDefinition: NodeDefinition[_]) = nodeDefinition match {
    case r: HyperNodeDefinition[_, _, _, _, _] => (Some(r.toQuery), s"(${ r.name })")
    case r                                     => (None, r.toQuery)
  }

  protected def nodeReferencer(nodeDefinition: NodeDefinition[_]) = s"(${ nodeDefinition.name })"

  override def parameterMap = startDefinition.parameterMap ++ endDefinition.parameterMap ++ nodeUuid.map(uuid => Map(uuidVariable -> uuid)).getOrElse(Map.empty)

  def toQuery: String = toQuery(true)
  def toQuery(matchNodes: Boolean): String = toQuery(matchNodes, matchNodes)
  def toQuery(matchStart: Boolean, matchEnd: Boolean): String = {
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

sealed trait SingleRelationDefinitionBase[
START <: Node,
RELATION <: AbstractRelation[START, END],
END <: Node,
+STARTDEF <: NodeDefinition[START],
+ENDDEF <: NodeDefinition[END]
] extends RelationDefinitionBase[START,RELATION,END,STARTDEF,ENDDEF] {
  val factory: AbstractRelationFactory[START, RELATION, END]

  def relationMatcher = factory match {
    case r: RelationFactory[_, RELATION, _]            => s"[$name :`${ r.relationType }`]"
    case r: HyperRelationFactory[_, _, RELATION, _, _] => s"[$startRelationName:`${ r.startRelationType }`]->($name ${ r.labels.map(l => s":`$l`").mkString } ${ nodeUuidMatcher })-[$endRelationName:`${ r.endRelationType }`]"
  }
}

case class HyperNodeDefinition[
START <: Node,
RELATION <: AbstractRelation[START, END] with UuidNode,
END <: Node,
STARTDEF <: NodeDefinition[START],
ENDDEF <: NodeDefinition[END]
](
  startDefinition: STARTDEF,
  factory: AbstractRelationFactory[START, RELATION, END] with NodeFactory[RELATION],
  endDefinition: ENDDEF,
  nodeUuid: Option[String] = None)(implicit val ctx: QueryContext) extends HyperNodeDefinitionBase[RELATION] with SingleRelationDefinitionBase[START, RELATION, END, STARTDEF, ENDDEF]

case class RelationDefinition[
START <: Node,
RELATION <: AbstractRelation[START, END],
END <: Node,
STARTDEF <: NodeDefinition[START],
ENDDEF <: NodeDefinition[END]
](
  startDefinition: STARTDEF,
  factory: AbstractRelationFactory[START, RELATION, END],
  endDefinition: ENDDEF)(implicit val ctx: QueryContext) extends SingleRelationDefinitionBase[START, RELATION, END, STARTDEF, ENDDEF] {
  val nodeUuid = None
}
