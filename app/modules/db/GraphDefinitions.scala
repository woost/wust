package modules

import renesca.parameter.{PropertyKey, ParameterValue}

package object db {

  import model.WustSchema.{UuidNode, ContentRelation, ContentRelationFactory}
  import modules.requests.{HyperConnectSchema, SimpleConnectSchema}
  import renesca.schema._
  import renesca.parameter.ParameterMap
  import renesca.parameter.implicits._

  type NodeRelationDefinition[START <: Node, RELATION <: ContentRelation[START,END], END <: Node] = RelationDefinition[START,RELATION,END,_ <: NodeDefinition[START], _ <: NodeDefinition[END]]
  type UuidNodeRelationDefinition[START <: UuidNode, RELATION <: ContentRelation[START,END], END <: UuidNode] = RelationDefinition[START,RELATION,END,_ <: UuidNodeDefinition[START], _ <: UuidNodeDefinition[END]]
  type StartUuidNodeRelationDefinition[START <: UuidNode, RELATION <: ContentRelation[START,END], END <: Node] = RelationDefinition[START,RELATION,END,_ <: UuidNodeDefinition[START], _ <: NodeDefinition[END]]
  type EndUuidNodeRelationDefinition[START <: Node, RELATION <: ContentRelation[START,END], END <: UuidNode] = RelationDefinition[START,RELATION,END,_ <: NodeDefinition[START], _ <: UuidNodeDefinition[END]]
  type FixedNodeRelationDefinition[START <: Node, RELATION <: ContentRelation[START,END], END <: Node] = RelationDefinition[START,RELATION,END,_ <: FixedNodeDefinition[START], _ <: FixedNodeDefinition[END]]
  type StartFixedNodeRelationDefinition[START <: Node, RELATION <: ContentRelation[START,END], END <: Node] = RelationDefinition[START,RELATION,END,_ <: FixedNodeDefinition[START], _ <: NodeDefinition[END]]
  type EndFixedNodeRelationDefinition[START <: Node, RELATION <: ContentRelation[START,END], END <: Node] = RelationDefinition[START,RELATION,END,_ <: NodeDefinition[START], _ <: FixedNodeDefinition[END]]
  type HyperNodeRelationDefinition[START <: Node with ContentRelation[_,_], RELATION <: ContentRelation[START,END], END <: Node with ContentRelation[_,_]] = RelationDefinition[START,RELATION,END,_ <: HyperNodeDefinition[_,START,_], _ <: HyperNodeDefinition[_,END,_]]
  type StartHyperNodeRelationDefinition[START <: Node with ContentRelation[_,_], RELATION <: ContentRelation[START,END], END <: Node] = RelationDefinition[START,RELATION,END,_ <: HyperNodeDefinition[_,START,_], _ <: NodeDefinition[END]]
  type EndHyperNodeRelationDefinition[START <: Node, RELATION <: ContentRelation[START,END], END <: Node with ContentRelation[_,_]] = RelationDefinition[START,RELATION,END,_ <: NodeDefinition[START], _ <: HyperNodeDefinition[_,END,_]]
  type HyperUuidNodeRelationDefinition[START <: Node with ContentRelation[_,_], RELATION <: ContentRelation[START,END], END <: UuidNode] = RelationDefinition[START,RELATION,END,_ <: HyperNodeDefinition[_,START,_], _ <: UuidNodeDefinition[END]]
  type UuidHyperNodeRelationDefinition[START <: UuidNode, RELATION <: ContentRelation[START,END], END <: ContentRelation[_,_] with Node] = RelationDefinition[START,RELATION,END,_ <: UuidNodeDefinition[START], _ <: HyperNodeDefinition[_,END,_]]

  // A neo4j variable needs to start with a letter an may not contain dashes
  private def randomVariable = "V" + java.util.UUID.randomUUID.toString.replace("-", "")

  trait GraphDefinition {
    def toQuery: String
    final val name = randomVariable
    def parameterMap: ParameterMap = Map.empty
  }

  trait NodeDefinition[+NODE <: Node] extends GraphDefinition {
    val factory: NodeFactory[NODE]
  }

  trait FixedNodeDefinition[+NODE <: Node] extends NodeDefinition[NODE]

  case class UuidNodeDefinition[+NODE <: UuidNode](
     factory: NodeFactory[NODE],
     uuid: String
     ) extends FixedNodeDefinition[NODE] {

    val uuidVariable = randomVariable

    override def parameterMap = Map(uuidVariable -> uuid)
    def toQuery = s"($name: `${factory.label}` {uuid: {$uuidVariable}})"
  }

  case class LabelNodeDefinition[+NODE <: Node](
    factory: NodeFactory[NODE]) extends NodeDefinition[NODE] {

    def toQuery = s"($name: `${factory.label}`)"
  }

  trait RelationDefinitionBase[START <: Node, RELATION <: ContentRelation[START,END], END <: Node, +STARTDEF <: NodeDefinition[START], +ENDDEF <: NodeDefinition[END]] extends GraphDefinition {
    val startDefinition: STARTDEF
    val factory: ContentRelationFactory[START, RELATION, END]
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

  case class HyperNodeDefinition[START <: UuidNode, RELATION <: ContentRelation[START,END] with Node, END <: UuidNode](
    startDefinition: UuidNodeDefinition[START],
    factory: ContentRelationFactory[START, RELATION, END] with NodeFactory[RELATION],
    endDefinition: UuidNodeDefinition[END]) extends FixedNodeDefinition[RELATION] with RelationDefinitionBase[START,RELATION,END,UuidNodeDefinition[START], UuidNodeDefinition[END]]

  case class RelationDefinition[START <: Node, RELATION <: ContentRelation[START,END], END <: Node, +STARTDEF <: NodeDefinition[START], +ENDDEF <: NodeDefinition[END]](
    startDefinition: STARTDEF,
    factory: ContentRelationFactory[START, RELATION, END],
    endDefinition: ENDDEF) extends RelationDefinitionBase[START,RELATION,END,STARTDEF,ENDDEF]
}
