package modules.db

import model.Helpers
import model.WustSchema.{Votes, ContentRelationFactory, UuidNode}
import renesca.graph.Label
import renesca.parameter.ParameterMap
import renesca.parameter.implicits._
import renesca.schema._

package object types {

  type FixedHyperNodeDefinition[START <: Node, RELATION <: AbstractRelation[START, END] with Node, END <: Node] = HyperNodeDefinition[START, RELATION, END, _ <: FixedNodeDefinition[START], _ <: FixedNodeDefinition[END]]
  type ContentRelationDefinition[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node] = RelationDefinitionBase[START, RELATION, END, _, _, _ <: ContentRelationFactory[START, RELATION, END]]
  type VotesRelationDefinition[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node] = RelationDefinitionBase[START, RELATION, END, _, _, _ <: Votes.type]

  type NodeRelationDefinition[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node] = RelationDefinitionBase[START, RELATION, END, _ <: NodeDefinition[START], _ <: NodeDefinition[END], _]
  type UuidRelationDefinition[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode] = RelationDefinitionBase[START, RELATION, END, _ <: UuidNodeDefinition[START], _ <: UuidNodeDefinition[END], _]
  type UuidAndNodeRelationDefinition[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: Node] = RelationDefinitionBase[START, RELATION, END, _ <: UuidNodeDefinition[START], _ <: NodeDefinition[END], _]
  type NodeAndUuidRelationDefinition[START <: Node, RELATION <: AbstractRelation[START, END], END <: UuidNode] = RelationDefinitionBase[START, RELATION, END, _ <: NodeDefinition[START], _ <: UuidNodeDefinition[END], _]
  type FactoryRelationDefinition[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode] = RelationDefinitionBase[START, RELATION, END, _ <: FactoryNodeDefinition[START], _ <: FactoryNodeDefinition[END], _]
  type FactoryAndNodeRelationDefinition[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: Node] = RelationDefinitionBase[START, RELATION, END, _ <: FactoryNodeDefinition[START], _ <: NodeDefinition[END], _]
  type NodeAndFactoryRelationDefinition[START <: Node, RELATION <: AbstractRelation[START, END], END <: UuidNode] = RelationDefinitionBase[START, RELATION, END, _ <: NodeDefinition[START], _ <: FactoryNodeDefinition[END], _]
  type FactoryUuidRelationDefinition[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode] = RelationDefinitionBase[START, RELATION, END, _ <: FactoryUuidNodeDefinition[START], _ <: FactoryUuidNodeDefinition[END], _]
  type FactoryUuidAndNodeRelationDefinition[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: Node] = RelationDefinitionBase[START, RELATION, END, _ <: FactoryUuidNodeDefinition[START], _ <: NodeDefinition[END], _]
  type NodeAndFactoryUuidRelationDefinition[START <: Node, RELATION <: AbstractRelation[START, END], END <: UuidNode] = RelationDefinitionBase[START, RELATION, END, _ <: NodeDefinition[START], _ <: FactoryUuidNodeDefinition[END], _]
  type FixedRelationDefinition[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node] = RelationDefinitionBase[START, RELATION, END, _ <: FixedNodeDefinition[START], _ <: FixedNodeDefinition[END], _]
  type NodeAndFixedRelationDefinition[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node] = RelationDefinitionBase[START, RELATION, END, _ <: FixedNodeDefinition[START], _ <: NodeDefinition[END], _]
  type FixedAndNodeRelationDefinition[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node] = RelationDefinitionBase[START, RELATION, END, _ <: NodeDefinition[START], _ <: FixedNodeDefinition[END], _]
  type HyperAndNodeRelationDefinition[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node] = RelationDefinitionBase[START, RELATION, END, _ <: HyperNodeDefinitionBase[START], _ <: NodeDefinition[END], _]
  type NodeAndHyperRelationDefinition[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node] = RelationDefinitionBase[START, RELATION, END, _ <: NodeDefinition[START], _ <: HyperNodeDefinitionBase[END], _]

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

sealed trait HyperNodeDefinitionBase[+NODE <: Node] extends FixedNodeDefinition[NODE]

sealed trait UuidNodeDefinition[+NODE <: UuidNode] extends FixedNodeDefinition[NODE] {
  val uuid: String
  val uuidVariable = randomVariable
  override def parameterMap = Map(uuidVariable -> uuid)
}

sealed trait LabelledUuidNodeDefinition[+NODE <: UuidNode] extends UuidNodeDefinition[NODE] {
  val labels: Set[Label]

  def toQuery = s"($name ${labels.map(l => s":`$l`").mkString} {uuid: {$uuidVariable}})"
}

sealed trait LabelledNodeDefinition[+NODE <: Node] extends FixedNodeDefinition[NODE] {
  val labels: Set[Label]

  def toQuery = s"($name ${labels.map(l => s":`$l`").mkString})"
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
+ENDDEF <: NodeDefinition[END],
+FACTORY <: AbstractRelationFactory[_ <: Node, _ <: AbstractRelation[_, _], _ <: Node]
] extends GraphDefinition {
  val startDefinition: STARTDEF
  val factory: AbstractRelationFactory[START, RELATION, END] with FACTORY
  val endDefinition: ENDDEF

  final val startRelationName = randomVariable
  final val endRelationName = randomVariable

  private def relationMatcher = factory match {
    case r: RelationFactory[_, RELATION, _]            => s"[$name :`${ r.relationType }`]"
    case r: HyperRelationFactory[_, _, RELATION, _, _] => s"[$startRelationName:`${ r.startRelationType }`]->($name ${ r.labels.map(l => s":`$l`").mkString })-[$endRelationName:`${ r.endRelationType }`]"
  }

  private def nodeMatcher(nodeDefinition: NodeDefinition[_]) = nodeDefinition match {
    case r: HyperNodeDefinition[_, _, _, _, _] => (Some(r.toQuery), s"(${ r.name })")
    case r                                     => (None, r.toQuery)
  }

  private def nodeReferencer(nodeDefinition: NodeDefinition[_]) = s"(${ nodeDefinition.name })"

  override def parameterMap = startDefinition.parameterMap ++ endDefinition.parameterMap

  def toQuery: String = toQuery(true)

  def toQuery(matchNodes: Boolean): String = {
    if (matchNodes) {
      val (startPre, startNode) = nodeMatcher(startDefinition)
      val (endPre, endNode) = nodeMatcher(endDefinition)
      val preMatcher = List(startPre, endPre).flatten.map(_ + ",").mkString
      s"$preMatcher$startNode-$relationMatcher->$endNode"
    } else {
      val startNode = nodeReferencer(startDefinition)
      val endNode = nodeReferencer(endDefinition)
      s"$startNode-$relationMatcher->$endNode"
    }
  }
}

case class HyperNodeDefinition[
START <: Node,
RELATION <: AbstractRelation[START, END] with Node,
END <: Node,
STARTDEF <: NodeDefinition[START],
ENDDEF <: NodeDefinition[END]
](
  startDefinition: STARTDEF,
  factory: AbstractRelationFactory[START, RELATION, END] with NodeFactory[RELATION],
  endDefinition: ENDDEF) extends HyperNodeDefinitionBase[RELATION] with RelationDefinitionBase[START, RELATION, END, STARTDEF, ENDDEF, AbstractRelationFactory[START, RELATION, END]]

case class RelationDefinition[
START <: Node,
RELATION <: AbstractRelation[START, END],
END <: Node,
STARTDEF <: NodeDefinition[START],
ENDDEF <: NodeDefinition[END],
FACTORY <: AbstractRelationFactory[_ <: Node, _ <: AbstractRelation[_ <: Node, _ <: Node], _ <: Node]
](
  startDefinition: STARTDEF,
  factory: AbstractRelationFactory[START, RELATION, END] with FACTORY,
  endDefinition: ENDDEF) extends RelationDefinitionBase[START, RELATION, END, STARTDEF, ENDDEF, AbstractRelationFactory[START, RELATION, END] with FACTORY]

