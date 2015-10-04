package modules.requests

import model.WustSchema._
import modules.db.access._
import renesca.schema._

case class ApiDefinition(restRoot: String, websocketRoot: String)

trait NodeSchemaBase[NODE <: UuidNode] {
  val connectSchemas: Map[String, ConnectSchema[NODE]]
}

case class NodeSchema[
NODE <: UuidNode
](path: String, name: String, op: NodeAccess[NODE], connectSchemas: Map[String, ConnectSchema[NODE]]) extends NodeSchemaBase[NODE]

sealed trait ConnectSchema[-NODE <: UuidNode] {
  val cardinality = "hasMany"
  val op: RelationAccess[NODE, UuidNode]
}

sealed trait PlainConnectSchema[BASE <: UuidNode] extends ConnectSchema[BASE]
sealed trait HyperConnectSchema[BASE <: UuidNode] extends ConnectSchema[BASE]

case class RelatedConnectSchema[BASE <: UuidNode, NODE <: UuidNode](op: RelationAccess[BASE, NODE]) extends PlainConnectSchema[BASE]

case class StartConnectSchema[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](op: StartRelationAccess[START, RELATION, END]) extends PlainConnectSchema[START]

case class EndConnectSchema[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](op: EndRelationAccess[START, RELATION, END]) extends PlainConnectSchema[END]

case class StartHyperConnectSchema[
START <: UuidNode,
RELATION <: HyperRelation[START, _ <: Relation[START,RELATION], RELATION, _ <: Relation[RELATION,END], END] with UuidNode,
END <: UuidNode
](factory: MatchableRelationFactory[START, RELATION, END] with UuidNodeMatchesFactory[RELATION] with HyperRelationFactory[START, _ <: Relation[START,RELATION], RELATION, _ <: Relation[RELATION,END], END], op: StartRelationAccess[START, RELATION, END], connectSchemas: Map[String, ConnectSchema[RELATION]]) extends NodeSchemaBase[RELATION] with HyperConnectSchema[START]

case class EndHyperConnectSchema[
START <: UuidNode,
RELATION <: HyperRelation[START, _ <: Relation[START,RELATION], RELATION, _ <: Relation[RELATION,END], END] with UuidNode,
END <: UuidNode
](factory: MatchableRelationFactory[START, RELATION, END] with UuidNodeMatchesFactory[RELATION] with HyperRelationFactory[START, _ <: Relation[START,RELATION], RELATION, _ <: Relation[RELATION,END], END], op: EndRelationAccess[START, RELATION, END], connectSchemas: Map[String, ConnectSchema[RELATION]]) extends NodeSchemaBase[RELATION] with HyperConnectSchema[END]

object dsl {

  class RightHyperFactoryHolder[
  START <: UuidNode,
  RELATION <: HyperRelation[START, _ <: Relation[START,RELATION], RELATION, _ <: Relation[RELATION,END], END] with UuidNode,
  END <: UuidNode
  ](factory: MatchableRelationFactory[START, RELATION, END] with UuidNodeMatchesFactory[RELATION] with HyperRelationFactory[START, _ <: Relation[START,RELATION], RELATION, _ <: Relation[RELATION,END], END]) {
    def >(op: StartRelationAccess[START, RELATION, END], connectSchemas: (String, ConnectSchema[RELATION])*) = {
      StartHyperConnectSchema(factory, op, connectSchemas.toMap)
    }
  }

  class LeftHyperFactoryHolder[
  START <: UuidNode,
  RELATION <: HyperRelation[START, _ <: Relation[START,RELATION], RELATION, _ <: Relation[RELATION,END], END] with UuidNode,
  END <: UuidNode
  ](factory: MatchableRelationFactory[START, RELATION, END] with UuidNodeMatchesFactory[RELATION] with HyperRelationFactory[START, _ <: Relation[START,RELATION], RELATION, _ <: Relation[RELATION,END], END]) {
    def <(op: EndRelationAccess[START, RELATION, END], connectSchemas: (String, ConnectSchema[RELATION])*) = {
      EndHyperConnectSchema(factory, op, connectSchemas.toMap)
    }
  }

  class StartRelationAccessHolder[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](val access: StartRelationAccess[START, RELATION, END]) {
    def +(decorate: StartRelationAccess[START, RELATION, END] => StartRelationAccess[START, RELATION, END]) = {
      decorate(access)
    }
    def +(control: AccessDecoratorControl) = {
      StartRelationAccessDecoration(access, control)
    }
  }

  implicit def StartRelationAccessPlus[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](access: StartRelationAccess[START, RELATION, END]): StartRelationAccessHolder[START, RELATION, END] = new StartRelationAccessHolder(access)

  class EndRelationAccessHolder[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](val access: EndRelationAccess[START, RELATION, END]) {
    def +(decorate: EndRelationAccess[START, RELATION, END] => EndRelationAccess[START, RELATION, END]) = {
      decorate(access)
    }
    def +(control: AccessDecoratorControl) = {
      EndRelationAccessDecoration(access, control)
    }
  }

  implicit def EndRelationAccessPlus[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](access: EndRelationAccess[START, RELATION, END]): EndRelationAccessHolder[START, RELATION, END] = new EndRelationAccessHolder(access)

  class NodeAccessHolder[NODE <: UuidNode](access: NodeAccess[NODE]) {
    def +(decorate: NodeAccess[NODE] => NodeAccess[NODE]) = {
      decorate(access)
    }
    def +(control: AccessDecoratorControl) = {
      NodeAccessDecoration(access, control)
    }
  }

  implicit def NodeAccessPlus[
  NODE <: UuidNode
  ](access: NodeAccess[NODE]): NodeAccessHolder[NODE] = new NodeAccessHolder(access)

  object NodeDef {
    def apply[NODE <: UuidNode](name: String, op: NodeAccess[NODE], connectSchemas: (String, ConnectSchema[NODE])*): (String) => NodeSchema[NODE] = {
      path => NodeSchema(path, name, op, connectSchemas.toMap)
    }
    def apply[NODE <: UuidNode](op: NodeAccess[NODE], connectSchemas: (String, ConnectSchema[NODE])*): (String) => NodeSchema[NODE] = {
      apply(op.factory.getClass.getSimpleName.dropRight(1), op, connectSchemas: _*)
    }
  }

  object N {
    def <>[BASE <: UuidNode, NODE <: UuidNode](op: RelationAccess[BASE, NODE]) = {
      RelatedConnectSchema(op)
    }

    def >[
    START <: UuidNode,
    RELATION <: AbstractRelation[START, END],
    END <: UuidNode
    ](op: StartRelationAccess[START, RELATION, END]) = {
      StartConnectSchema(op)
    }

    def <[
    START <: UuidNode,
    RELATION <: AbstractRelation[START, END],
    END <: UuidNode
    ](op: EndRelationAccess[START, RELATION, END]) = {
      EndConnectSchema(op)
    }

    def >[
    START <: UuidNode,
    RELATION <: HyperRelation[START, _ <: Relation[START,RELATION], RELATION, _ <: Relation[RELATION,END], END] with UuidNode,
    END <: UuidNode
    ](factory: MatchableRelationFactory[START, RELATION, END] with UuidNodeMatchesFactory[RELATION] with HyperRelationFactory[START, _ <: Relation[START,RELATION], RELATION, _ <: Relation[RELATION,END], END]) = {
      new RightHyperFactoryHolder(factory)
    }

    def <[
    START <: UuidNode,
    RELATION <: HyperRelation[START, _ <: Relation[START,RELATION], RELATION, _ <: Relation[RELATION,END], END] with UuidNode,
    END <: UuidNode
    ](factory: MatchableRelationFactory[START, RELATION, END] with UuidNodeMatchesFactory[RELATION] with HyperRelationFactory[START, _ <: Relation[START,RELATION], RELATION, _ <: Relation[RELATION,END], END]) = {
      new LeftHyperFactoryHolder(factory)
    }
  }
}
