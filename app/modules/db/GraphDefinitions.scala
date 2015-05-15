package modules.db

import model.WustSchema.{ContentRelationFactory, UuidNode}
import renesca.parameter.ParameterMap
import renesca.parameter.implicits._
import renesca.schema._

package object types {

  type ContentRelationDefinition[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node] = RelationDefinitionBase[START, RELATION, END, _, _, _ <: ContentRelationFactory[START, RELATION, END]]

  type NodeRelationDefinition[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node] = RelationDefinitionBase[START, RELATION, END, _ <: NodeDefinition[START], _ <: NodeDefinition[END], _ <: AbstractRelationFactory[START, RELATION, END]]
  type UuidNodeRelationDefinition[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode] = RelationDefinitionBase[START, RELATION, END, _ <: UuidNodeDefinition[START], _ <: UuidNodeDefinition[END], _ <: AbstractRelationFactory[START, RELATION, END]]
  type FixedNodeRelationDefinition[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node] = RelationDefinitionBase[START, RELATION, END, _ <: FixedNodeDefinition[START], _ <: FixedNodeDefinition[END], _ <: AbstractRelationFactory[START, RELATION, END]]
  type StartFixedNodeRelationDefinition[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node] = RelationDefinitionBase[START, RELATION, END, _ <: FixedNodeDefinition[START], _ <: NodeDefinition[END], _ <: AbstractRelationFactory[START, RELATION, END]]
  type EndFixedNodeRelationDefinition[START <: Node, RELATION <: AbstractRelation[START, END], END <: Node] = RelationDefinitionBase[START, RELATION, END, _ <: NodeDefinition[START], _ <: FixedNodeDefinition[END], _ <: AbstractRelationFactory[START, RELATION, END]]
  type FixedUuidNodeRelationDefinition[START <: Node, RELATION <: AbstractRelation[START, END], END <: UuidNode] = RelationDefinitionBase[START, RELATION, END, _ <: FixedNodeDefinition[START], _ <: UuidNodeDefinition[END], _ <: AbstractRelationFactory[START, RELATION, END]]
  type UuidFixedNodeRelationDefinition[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: Node] = RelationDefinitionBase[START, RELATION, END, _ <: UuidNodeDefinition[START], _ <: FixedNodeDefinition[END], _ <: AbstractRelationFactory[START, RELATION, END]]
  type HyperUuidNodeRelationDefinition[START <: Node, RELATION <: AbstractRelation[START, END], END <: UuidNode] = RelationDefinitionBase[START, RELATION, END, _ <: HyperNodeDefinitionBase[START], _ <: UuidNodeDefinition[END], _ <: AbstractRelationFactory[START, RELATION, END]]
  type UuidHyperNodeRelationDefinition[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: Node] = RelationDefinitionBase[START, RELATION, END, _ <: UuidNodeDefinition[START], _ <: HyperNodeDefinitionBase[END], _ <: AbstractRelationFactory[START, RELATION, END]]

}

trait GraphDefinition {
  def toQuery: String
  final val name = randomVariable
  def parameterMap: ParameterMap = Map.empty

  protected def randomVariable = "V" + java.util.UUID.randomUUID.toString.replace("-", "")
}

trait NodeDefinition[+NODE <: Node] extends GraphDefinition

trait FixedNodeDefinition[+NODE <: Node] extends NodeDefinition[NODE]

trait HyperNodeDefinitionBase[+NODE <: Node] extends FixedNodeDefinition[NODE]

trait UuidNodeDefinitionBase extends GraphDefinition {
  val uuid: String
  val uuidVariable = randomVariable
  override def parameterMap = Map(uuidVariable -> uuid)
}
case class UuidNodeDefinition[+NODE <: UuidNode](
  factory: NodeFactory[NODE],
  uuid: String
) extends FixedNodeDefinition[NODE] with UuidNodeDefinitionBase {

  def toQuery = s"($name: `${factory.label}` {uuid: {$uuidVariable}})"
}

case class AnyUuidNodeDefinition[NODE <: UuidNode](
  uuid: String
) extends FixedNodeDefinition[NODE] with UuidNodeDefinitionBase {

  def toQuery = s"($name {uuid: {$uuidVariable}})"
}

case class LabelNodeDefinition[+NODE <: Node](
  factory: NodeFactory[NODE]) extends NodeDefinition[NODE] {

  def toQuery = s"($name: `${factory.label}`)"
}

case class AnyNodeDefinition[+NODE <: Node]() extends NodeDefinition[NODE] {
  override def toQuery: String = s"($name)"
}

trait RelationDefinitionBase[
  START <: Node,
  RELATION <: AbstractRelation[START,END],
  END <: Node,
  +STARTDEF <: NodeDefinition[START],
  +ENDDEF <: NodeDefinition[END],
  +FACTORY <: AbstractRelationFactory[_ <: Node,_ <: AbstractRelation[_,_], _ <: Node]
] extends GraphDefinition {
  val startDefinition: STARTDEF
  val factory: AbstractRelationFactory[START,RELATION,END] with FACTORY
  val endDefinition: ENDDEF

  private def relationMatcher = factory match {
    case r: RelationFactory[_, RELATION, _]            => s"[$name :`${ r.relationType }`]"
    case r: HyperRelationFactory[_, _, RELATION, _, _] => s"[`${ r.startRelationType }`]->($name :`${ r.label }`)-[`${ r.endRelationType }`]"
  }

  private def nodeMatcher(nodeDefinition: NodeDefinition[_]) = nodeDefinition match {
    case r: HyperNodeDefinition[_,_,_] => (Some(r.toQuery), s"(${r.name})")
    case r => (None, r.toQuery)
  }

  override def parameterMap = startDefinition.parameterMap ++ endDefinition.parameterMap

  def toQuery: String = {
    val (startPre, startNode) = nodeMatcher(startDefinition)
    val (endPre, endNode) = nodeMatcher(endDefinition)
    val preMatcher = List(startPre, endPre).flatten.map(_ + ",").mkString

    s"$preMatcher$startNode-$relationMatcher->$endNode"
  }
}

case class HyperNodeDefinition[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END] with Node,
  END <: UuidNode
](
  startDefinition: FixedNodeDefinition[START],
  factory: AbstractRelationFactory[START, RELATION, END] with NodeFactory[RELATION],
  endDefinition: FixedNodeDefinition[END]) extends HyperNodeDefinitionBase[RELATION] with RelationDefinitionBase[START,RELATION,END,FixedNodeDefinition[START], FixedNodeDefinition[END],AbstractRelationFactory[START,RELATION,END]]

case class RelationDefinition[
  START <: Node,
  RELATION <: AbstractRelation[START,END],
  END <: Node,
  STARTDEF <: NodeDefinition[START],
  ENDDEF <: NodeDefinition[END],
  FACTORY <: AbstractRelationFactory[_ <: Node, _ <: AbstractRelation[_ <: Node, _ <: Node], _ <: Node]
](
  startDefinition: STARTDEF,
  factory: AbstractRelationFactory[START,RELATION,END] with FACTORY,
  endDefinition: ENDDEF) extends RelationDefinitionBase[START,RELATION,END,STARTDEF,ENDDEF,AbstractRelationFactory[START,RELATION,END] with FACTORY]
