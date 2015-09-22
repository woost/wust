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

sealed trait GraphDefinition {
  def toQuery: String
  final val name = randomVariable
  def parameterMap: ParameterMap = Map.empty

  protected def randomVariable = "V" + Helpers.uuidBase64.replace("-", "") // "-" is not allowed as variable identifier
}

sealed trait NodeDefinition[+NODE <: Node] extends GraphDefinition

sealed trait FactoryNodeDefinition[+NODE <: Node] extends NodeDefinition[NODE] {
  val factory: NodeFactory[NODE]
  val labels = factory.labels
}

sealed trait FixedNodeDefinition[+NODE <: Node] extends NodeDefinition[NODE]

sealed trait HyperNodeDefinitionBase[+NODE <: Node] extends FixedNodeDefinition[NODE] {
  val startName: String
  val endName: String
  val startRelationName: String
  val endRelationName: String
  val startDefinition: NodeDefinition[_]
  val endDefinition: NodeDefinition[_]
}

sealed trait UuidNodeDefinition[+NODE <: UuidNode] extends FixedNodeDefinition[NODE] {
  val uuid: String
  val uuidVariable = randomVariable
  override def parameterMap = Map(uuidVariable -> uuid)
}

sealed trait LabelledUuidNodeDefinition[+NODE <: UuidNode] extends UuidNodeDefinition[NODE] {
  val labels: Set[Label]

  def toQuery = s"($name ${ labels.map(l => s":`$l`").mkString } {uuid: {$uuidVariable}})"
}

sealed trait LabelledNodeDefinition[+NODE <: Node] extends FixedNodeDefinition[NODE] {
  val labels: Set[Label]

  def toQuery = s"($name ${ labels.map(l => s":`$l`").mkString })"
}

case class FactoryUuidNodeDefinition[+NODE <: UuidNode](
  factory: NodeFactory[NODE],
  uuid: String
  ) extends LabelledUuidNodeDefinition[NODE] with FactoryNodeDefinition[NODE]

case class ConcreteFactoryNodeDefinition[+NODE <: Node](
  factory: NodeFactory[NODE]
  ) extends LabelledNodeDefinition[NODE] with FactoryNodeDefinition[NODE]

case class ConcreteNodeDefinition[+NODE <: UuidNode](
  node: NODE
  ) extends LabelledUuidNodeDefinition[NODE] {

  val uuid = node.uuid
  val labels = node.labels
}

case class LabelUuidNodeDefinition[+NODE <: UuidNode](
  labels: Set[Label],
  uuid: String
  ) extends LabelledUuidNodeDefinition[NODE]

case class LabelNodeDefinition[+NODE <: Node](
  labels: Set[Label]) extends LabelledNodeDefinition[NODE]

case class AnyUuidNodeDefinition[+NODE <: UuidNode](
  uuid: String
  ) extends UuidNodeDefinition[NODE] {

  override def toQuery = s"($name {uuid: {$uuidVariable}})"
}

case class AnyNodeDefinition[+NODE <: Node]() extends NodeDefinition[NODE] {
  override def toQuery: String = s"($name)"
}

sealed trait RelationDefinitionBase[
START <: Node,
RELATION <: AbstractRelation[START, END],
END <: Node,
+STARTDEF <: NodeDefinition[START],
+ENDDEF <: NodeDefinition[END]
] extends GraphDefinition {
  val startDefinition: STARTDEF
  val factory: AbstractRelationFactory[START, RELATION, END]
  val endDefinition: ENDDEF
  val nodeUuid: Option[String]
  val uuidVariable = randomVariable

  val startName = startDefinition.name
  val endName = endDefinition.name
  final val startRelationName = randomVariable
  final val endRelationName = randomVariable

  private def nodeUuidMatcher = nodeUuid.map(uuid => s"{uuid: {$uuidVariable}}").getOrElse("")

  private def relationMatcher = factory match {
    case r: RelationFactory[_, RELATION, _]            => s"[$name :`${ r.relationType }`]"
    case r: HyperRelationFactory[_, _, RELATION, _, _] => s"[$startRelationName:`${ r.startRelationType }`]->($name ${ r.labels.map(l => s":`$l`").mkString } ${ nodeUuidMatcher })-[$endRelationName:`${ r.endRelationType }`]"
  }

  private def nodeMatcher(nodeDefinition: NodeDefinition[_]) = nodeDefinition match {
    case r: HyperNodeDefinition[_, _, _, _, _] => (Some(r.toQuery), s"(${ r.name })")
    case r                                     => (None, r.toQuery)
  }

  private def nodeReferencer(nodeDefinition: NodeDefinition[_]) = s"(${ nodeDefinition.name })"

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
  nodeUuid: Option[String] = None) extends HyperNodeDefinitionBase[RELATION] with RelationDefinitionBase[START, RELATION, END, STARTDEF, ENDDEF]

case class RelationDefinition[
START <: Node,
RELATION <: AbstractRelation[START, END],
END <: Node,
STARTDEF <: NodeDefinition[START],
ENDDEF <: NodeDefinition[END]
](
  startDefinition: STARTDEF,
  factory: AbstractRelationFactory[START, RELATION, END],
  endDefinition: ENDDEF) extends RelationDefinitionBase[START, RELATION, END, STARTDEF, ENDDEF] {
  val nodeUuid = None
}

