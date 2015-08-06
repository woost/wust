package modules.requests

import model.WustSchema._
import modules.db._
import modules.db.access._
import renesca.schema._

case class ApiDefinition(restRoot: String, websocketRoot: String)

trait NodeSchemaBase[NODE <: UuidNode] {
  val connectSchemas: Map[String, ConnectSchema[NODE]]
}

case class NodeSchema[
NODE <: UuidNode
](path: String, op: NodeAccess[NODE], connectSchemas: Map[String, ConnectSchema[NODE]]) extends NodeSchemaBase[NODE]

sealed trait ConnectSchema[-NODE <: UuidNode] {
  val reverse: Boolean
  val cardinality = "hasMany"
  val op: RelationAccess[NODE, UuidNode]
}

sealed trait StartConnection {
  val reverse = false
}
sealed trait EndConnection {
  val reverse = true
}
sealed trait PlainConnectSchema[BASE <: UuidNode] extends ConnectSchema[BASE]
sealed trait HyperConnectSchema[BASE <: UuidNode] extends ConnectSchema[BASE] {
  val connectSchemas: Map[String, ConnectSchema[_]]
}

case class StartConnectSchema[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](op: StartRelationAccess[START, RELATION, END]) extends PlainConnectSchema[START] with StartConnection

case class EndConnectSchema[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](op: EndRelationAccess[START, RELATION, END]) extends PlainConnectSchema[END] with EndConnection

case class StartHyperConnectSchema[
START <: UuidNode,
RELATION <: AbstractRelation[START, END] with UuidNode,
END <: UuidNode
](factory: AbstractRelationFactory[START, RELATION, END] with NodeFactory[RELATION], op: StartRelationAccess[START, RELATION, END], connectSchemas: Map[String, ConnectSchema[RELATION]]) extends NodeSchemaBase[RELATION] with HyperConnectSchema[START] with StartConnection {
  def toNodeDefinition(baseDef: FixedNodeDefinition[START], uuid: String) = {
    HyperNodeDefinition(baseDef, factory, op.toNodeDefinition(uuid))
  }
}

case class EndHyperConnectSchema[
START <: UuidNode,
RELATION <: AbstractRelation[START, END] with UuidNode,
END <: UuidNode
](factory: AbstractRelationFactory[START, RELATION, END] with NodeFactory[RELATION], op: EndRelationAccess[START, RELATION, END], connectSchemas: Map[String, ConnectSchema[RELATION]]) extends NodeSchemaBase[RELATION] with HyperConnectSchema[END] with EndConnection {
  def toNodeDefinition(baseDef: FixedNodeDefinition[END], uuid: String) = {
    HyperNodeDefinition(op.toNodeDefinition(uuid), factory, baseDef)
  }
}

object dsl {

  class RightHyperFactoryHolder[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END] with UuidNode,
  END <: UuidNode
  ](factory: HyperConnectionFactory[START,RELATION,END] with UuidNodeMatchesFactory[RELATION]) {
    def >(op: UuidNodeMatchesFactory[START] => StartRelationAccess[START, RELATION, END], connectSchemas: (String, (HyperConnectionFactory[START,RELATION,END] with UuidNodeMatchesFactory[RELATION]) => ConnectSchema[RELATION])*): UuidNodeMatchesFactory[START] => StartHyperConnectSchema[START, RELATION, END] = {
      baseFactory => StartHyperConnectSchema(factory, op(baseFactory), connectSchemas.toMap.mapValues(_(factory)))
    }

    def >(op: StartRelationAccess[START, RELATION, END], connectSchemas: (String, (HyperConnectionFactory[START,RELATION,END] with UuidNodeMatchesFactory[RELATION]) => ConnectSchema[RELATION])*): UuidNodeMatchesFactory[START] => StartHyperConnectSchema[START, RELATION, END] = {
      baseFactory => StartHyperConnectSchema(factory, op, connectSchemas.toMap.mapValues(_(factory)))
    }
  }

  class LeftHyperFactoryHolder[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END] with UuidNode,
  END <: UuidNode
  ](factory: HyperConnectionFactory[START,RELATION,END] with UuidNodeMatchesFactory[RELATION]) {
    def <(op: UuidNodeMatchesFactory[END] => EndRelationAccess[START, RELATION, END], connectSchemas: (String, (HyperConnectionFactory[START,RELATION,END] with UuidNodeMatchesFactory[RELATION]) => ConnectSchema[RELATION])*): UuidNodeMatchesFactory[END] => EndHyperConnectSchema[START, RELATION, END] = {
      baseFactory => EndHyperConnectSchema(factory, op(baseFactory), connectSchemas.toMap.mapValues(_(factory)))
    }

    def <(op: EndRelationAccess[START, RELATION, END], connectSchemas: (String, (HyperConnectionFactory[START,RELATION,END] with UuidNodeMatchesFactory[RELATION]) => ConnectSchema[RELATION])*): UuidNodeMatchesFactory[END] => EndHyperConnectSchema[START, RELATION, END] = {
      baseFactory => EndHyperConnectSchema(factory, op, connectSchemas.toMap.mapValues(_(factory)))
    }
  }

  class StartRelationAccessHolder[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](val accessFunc: UuidNodeMatchesFactory[START] => StartRelationAccess[START,RELATION,END]) {
    def +(nodeAccess: NodeAccess[END]): UuidNodeMatchesFactory[START] => StartRelationAccess[START,RELATION,END] = {
      factory => {
        val access = accessFunc(factory)
        access.withCreate(nodeAccess)
        access
      }
    }
  }

  implicit def StartRelationAccessToCreatable[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](access: UuidNodeMatchesFactory[START] => StartRelationAccess[START,RELATION,END]): StartRelationAccessHolder[START,RELATION,END] = new StartRelationAccessHolder(access)

  class EndRelationAccessHolder[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](val accessFunc: UuidNodeMatchesFactory[END] => EndRelationAccess[START,RELATION,END]) {
    def +(nodeAccess: NodeAccess[START]): UuidNodeMatchesFactory[END] => EndRelationAccess[START,RELATION,END] = {
      factory => {
        val access = accessFunc(factory)
        access.withCreate(nodeAccess)
        access
      }
    }
  }

  implicit def EndRelationAccessToCreatable[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](access: UuidNodeMatchesFactory[END] => EndRelationAccess[START,RELATION,END]): EndRelationAccessHolder[START,RELATION,END] = new EndRelationAccessHolder(access)

  class StartHyperRelationAccessHolder[
  ISTART <: UuidNode,
  IEND <: UuidNode,
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](val accessFunc: (HyperConnectionFactory[ISTART,START with AbstractRelation[ISTART,IEND],IEND] with UuidNodeMatchesFactory[START]) => StartRelationAccess[START,RELATION,END]) {
    def +(nodeAccess: NodeAccess[END]): (HyperConnectionFactory[ISTART,START with AbstractRelation[ISTART,IEND],IEND] with UuidNodeMatchesFactory[START]) => StartRelationAccess[START,RELATION,END] = {
      factory => {
        val access = accessFunc(factory)
        access.withCreate(nodeAccess)
        access
      }
    }
  }

  implicit def StartHyperRelationAccessToCreatable[
  ISTART <: UuidNode,
  IEND <: UuidNode,
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](access: (HyperConnectionFactory[ISTART,START with AbstractRelation[ISTART,IEND],IEND] with UuidNodeMatchesFactory[START]) => StartRelationAccess[START,RELATION,END]): StartHyperRelationAccessHolder[ISTART,IEND,START,RELATION,END] = new StartHyperRelationAccessHolder(access)

  class EndHyperRelationAccessHolder[
  ISTART <: UuidNode,
  IEND <: UuidNode,
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](val accessFunc: (HyperConnectionFactory[ISTART,END with AbstractRelation[ISTART,IEND],IEND] with UuidNodeMatchesFactory[END]) => EndRelationAccess[START,RELATION,END]) {
    def +(nodeAccess: NodeAccess[START]): (HyperConnectionFactory[ISTART,END with AbstractRelation[ISTART,IEND],IEND] with UuidNodeMatchesFactory[END]) => EndRelationAccess[START,RELATION,END] = {
      factory => {
        val access = accessFunc(factory)
        access.withCreate(nodeAccess)
        access
      }
    }
  }

  implicit def EndHyperRelationAccessToCreatable[
  ISTART <: UuidNode,
  IEND <: UuidNode,
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](access: (HyperConnectionFactory[ISTART,END with AbstractRelation[ISTART,IEND],IEND] with UuidNodeMatchesFactory[END]) => EndRelationAccess[START,RELATION,END]): EndHyperRelationAccessHolder[ISTART,IEND,START,RELATION,END] = new EndHyperRelationAccessHolder(access)

  object NodeDef {
    def apply[NODE <: UuidNode](factory: UuidNodeMatchesFactory[NODE], op: NodeAccess[NODE], connectSchemas: (String, UuidNodeMatchesFactory[NODE] => ConnectSchema[NODE])*): (String) => NodeSchema[NODE] = {
      path => NodeSchema(path, op, connectSchemas.toMap.mapValues(_(factory)))
    }
  }

  object N {
    def >[
    START <: UuidNode,
    RELATION <: AbstractRelation[START, END],
    END <: UuidNode
    ](op: UuidNodeMatchesFactory[START] => StartRelationAccess[START, RELATION, END]): UuidNodeMatchesFactory[START] => StartConnectSchema[START, RELATION, END] = {
      baseFactory => StartConnectSchema(op(baseFactory))
    }

    def <[
    START <: UuidNode,
    RELATION <: AbstractRelation[START, END],
    END <: UuidNode
    ](op: UuidNodeMatchesFactory[END] => EndRelationAccess[START, RELATION, END]): UuidNodeMatchesFactory[END] => EndConnectSchema[START, RELATION, END] = {
      baseFactory => EndConnectSchema(op(baseFactory))
    }

    def >[
    START <: UuidNode,
    RELATION <: AbstractRelation[START, END],
    END <: UuidNode
    ](op: StartRelationAccess[START, RELATION, END]): UuidNodeMatchesFactory[START] => StartConnectSchema[START, RELATION, END] = {
      baseFactory => StartConnectSchema(op)
    }

    def <[
    START <: UuidNode,
    RELATION <: AbstractRelation[START, END],
    END <: UuidNode
    ](op: EndRelationAccess[START, RELATION, END]): UuidNodeMatchesFactory[END] => EndConnectSchema[START, RELATION, END] = {
      baseFactory => EndConnectSchema(op)
    }

    def >[
    START <: UuidNode,
    RELATION <: AbstractRelation[START, END] with UuidNode,
    END <: UuidNode
    ](factory: HyperConnectionFactory[START, RELATION, END] with UuidNodeMatchesFactory[RELATION]) = {
      new RightHyperFactoryHolder(factory)
    }

    def <[
    START <: UuidNode,
    RELATION <: AbstractRelation[START, END] with UuidNode,
    END <: UuidNode
    ](factory: HyperConnectionFactory[START, RELATION, END] with UuidNodeMatchesFactory[RELATION]) = {
      new LeftHyperFactoryHolder(factory)
    }
  }

  object HR {
    def >[
    ISTART <: UuidNode,
    IEND <: UuidNode,
    START <: UuidNode,
    RELATION <: AbstractRelation[START, END],
    END <: UuidNode
    ](op: (HyperConnectionFactory[ISTART,START with AbstractRelation[ISTART,IEND],IEND] with UuidNodeMatchesFactory[START]) => StartRelationAccess[START, RELATION, END]): (HyperConnectionFactory[ISTART,START with AbstractRelation[ISTART,IEND],IEND] with UuidNodeMatchesFactory[START]) => StartConnectSchema[START, RELATION, END] = {
      baseFactory => StartConnectSchema(op(baseFactory))
    }

    def <[
    ISTART <: UuidNode,
    IEND <: UuidNode,
    START <: UuidNode,
    RELATION <: AbstractRelation[START, END],
    END <: UuidNode
    ](op: (HyperConnectionFactory[ISTART,END with AbstractRelation[ISTART,IEND],IEND] with UuidNodeMatchesFactory[END]) => EndRelationAccess[START, RELATION, END]): (HyperConnectionFactory[ISTART,END with AbstractRelation[ISTART,IEND],IEND] with UuidNodeMatchesFactory[END]) => EndConnectSchema[START, RELATION, END] = {
      baseFactory => EndConnectSchema(op(baseFactory))
    }
  }
}
