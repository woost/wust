package modules.db.access

import model.WustSchema._
import modules.db.Database._
import modules.db._
import modules.db.types._
import modules.requests.ConnectResponse
import play.api.libs.json.JsValue
import renesca.schema._

trait RelationAccess[-NODE <: UuidNode, +OTHER <: UuidNode] {
  //TODO: switch left/right...left should be errors
  //TODO: no nodedefinitions as args
  def read(baseDef: FixedNodeDefinition[NODE], page: Option[Int]): Either[Iterable[OTHER], String] = Right("No read access on Relation")
  def delete(baseDef: FixedNodeDefinition[NODE], uuid: String): Either[Boolean, String] = Right("No delete access on Relation")
  def deleteHyper(baseDef: HyperNodeDefinitionBase[NODE with AbstractRelation[_, _]], uuid: String): Either[Boolean, String] = Right("No delete access on HyperRelation")
  def create(uuid: String, user: User, json: JsValue): Either[ConnectResponse[OTHER], String]
  def create(uuid: String, user: User, otherUuid: String): Either[ConnectResponse[OTHER], String] = Right("No create access on Relation")
  def createHyper(startUuid: String, endUuid: String, user: User, json: JsValue): Either[ConnectResponse[OTHER], String]
  def createHyper(startUuid: String, endUuid: String, user: User, nestedUuid: String): Either[ConnectResponse[OTHER], String] = Right("No create access on HyperRelation")
}

trait NodeAwareRelationAccess[NODE <: UuidNode, OTHER <: UuidNode] extends RelationAccess[NODE, OTHER] {
  private var nodeAccess: Option[NodeAccess[OTHER]] = None
  def withCreate(access: NodeAccess[OTHER]) = {
    nodeAccess = Some(access)
  }

  private def createNode(user: User, js: JsValue): Either[OTHER, String] = {
    nodeAccess.map(_.create(user, js)).getOrElse(Right("No factory defined on connect path"))
  }

  def create(uuid: String, user: User, json: JsValue): Either[ConnectResponse[OTHER], String] = {
    createNode(user, json).left.toOption.map(n => create(uuid, user, n.uuid)).getOrElse(Right("Cannot create node on connect path"))
  }
  def createHyper(startUuid: String, endUuid: String, user: User, json: JsValue): Either[ConnectResponse[OTHER], String] = {
    createNode(user, json).left.toOption.map(n => createHyper(startUuid, endUuid, user, n.uuid)).getOrElse(Right("Cannot create node on connect path"))
  }
}

trait StartRelationAccess[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends NodeAwareRelationAccess[START,END] {
  val nodeFactory: NodeFactory[END]

  def toNodeDefinition = ConcreteFactoryNodeDefinition(nodeFactory)
  def toNodeDefinition(uuid: String) = FactoryUuidNodeDefinition(nodeFactory, uuid)

  protected def pageAwareRead(relDefs: Seq[NodeAndFixedRelationDefinition[START,RELATION,END]], pageOpt: Option[Int]) = {
    pageOpt.map { page =>
      val limit = 30
      val skip = page * limit;
      Left(limitedStartConnectedDiscourseNodes(skip, limit, relDefs: _*))
    }.getOrElse(Left(startConnectedDiscourseNodes(relDefs: _*)))
  }
}

trait EndRelationAccess[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends NodeAwareRelationAccess[END,START] {
  val nodeFactory: NodeFactory[START]

  def toNodeDefinition = ConcreteFactoryNodeDefinition(nodeFactory)
  def toNodeDefinition(uuid: String) = FactoryUuidNodeDefinition(nodeFactory, uuid)

  protected def pageAwareRead(relDefs: Seq[FixedAndNodeRelationDefinition[START,RELATION,END]], pageOpt: Option[Int]) = {
    pageOpt.map { page =>
      val limit = 30
      val skip = page * limit;
      Left(limitedEndConnectedDiscourseNodes(skip, limit, relDefs: _*))
    }.getOrElse(Left(endConnectedDiscourseNodes(relDefs: _*)))
  }
}

case class StartMultiRelationRead[
START <: UuidNode,
END <: UuidNode
](
  factories: AbstractRelationFactory[START,AbstractRelation[START,END],END]*
)(
  val nodeFactory: NodeFactory[END]
) extends StartRelationAccess[START,AbstractRelation[START,END],END] {
  override def read(baseDef: FixedNodeDefinition[START], pageOpt: Option[Int]) = {
    pageAwareRead(factories.map(RelationDefinition(baseDef, _, toNodeDefinition)), pageOpt)
  }
}

case class EndMultiRelationRead[
START <: UuidNode,
END <: UuidNode
](
  factories: AbstractRelationFactory[START,AbstractRelation[START,END],END]*
)(
  val nodeFactory: NodeFactory[START]
) extends EndRelationAccess[START,AbstractRelation[START,END],END] {
  override def read(baseDef: FixedNodeDefinition[END], pageOpt: Option[Int]) = {
    pageAwareRead(factories.map(RelationDefinition(toNodeDefinition, _, baseDef)), pageOpt)
  }
}


class StartRelationRead[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](
  val factory: AbstractRelationFactory[START, RELATION, END],
  val nodeFactory: NodeFactory[END]
  ) extends StartRelationAccess[START, RELATION, END] {

  override def read(baseDef: FixedNodeDefinition[START], pageOpt: Option[Int]) = {
    pageAwareRead(Seq(RelationDefinition(baseDef, factory, toNodeDefinition)), pageOpt)
  }
}

object StartRelationRead {
  def apply[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](
    factory: AbstractRelationFactory[START, RELATION, END],
    nodeFactory: NodeFactory[END]
    ): UuidNodeMatchesFactory[START] => StartRelationRead[START, RELATION, END] = {
    _ => new StartRelationRead(factory, nodeFactory)
  }
}

class EndRelationRead[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](
  val factory: AbstractRelationFactory[START, RELATION, END],
  val nodeFactory: NodeFactory[START]
  ) extends EndRelationAccess[START, RELATION, END] {

  override def read(baseDef: FixedNodeDefinition[END], pageOpt: Option[Int]) = {
    pageAwareRead(Seq(RelationDefinition(toNodeDefinition, factory, baseDef)), pageOpt)
  }
}

object EndRelationRead {
  def apply[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](
    factory: AbstractRelationFactory[START, RELATION, END],
    nodeFactory: NodeFactory[START]
    ): UuidNodeMatchesFactory[END] => EndRelationRead[START, RELATION, END] = {
    _ => new EndRelationRead(factory, nodeFactory)
  }
}

//TODO: correct results for delete....
class StartRelationReadDelete[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](
  factory: AbstractRelationFactory[START, RELATION, END],
  nodeFactory: NodeFactory[END]
  ) extends StartRelationRead(factory, nodeFactory) {

  override def delete(baseDef: FixedNodeDefinition[START], uuid: String) = {
    val relationDefinition = RelationDefinition(baseDef, factory, toNodeDefinition(uuid))
    disconnectNodes(relationDefinition)
    Left(true)
  }

  override def deleteHyper(baseDef: HyperNodeDefinitionBase[START with AbstractRelation[_, _]], uuid: String) = {
    val relationDefinition = RelationDefinition(baseDef, factory, toNodeDefinition(uuid))
    disconnectNodes(relationDefinition)
    Left(true)
  }
}

object StartRelationReadDelete {
  def apply[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](
    factory: AbstractRelationFactory[START, RELATION, END],
    nodeFactory: NodeFactory[END]
    ): UuidNodeDefinition[START] => StartRelationReadDelete[START, RELATION, END] = {
    _ => new StartRelationReadDelete(factory, nodeFactory)
  }
}

class EndRelationReadDelete[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](
  factory: AbstractRelationFactory[START, RELATION, END],
  nodeFactory: NodeFactory[START]
  ) extends EndRelationRead(factory, nodeFactory) {

  override def delete(baseDef: FixedNodeDefinition[END], uuid: String) = {
    val relationDefinition = RelationDefinition(toNodeDefinition(uuid), factory, baseDef)
    disconnectNodes(relationDefinition)
    Left(true)
  }

  override def deleteHyper(baseDef: HyperNodeDefinitionBase[END with AbstractRelation[_, _]], uuid: String) = {
    val relationDefinition = RelationDefinition(toNodeDefinition(uuid), factory, baseDef)
    disconnectNodes(relationDefinition)
    Left(true)
  }
}

object EndRelationReadDelete {
  def apply[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](
    factory: AbstractRelationFactory[START, RELATION, END],
    nodeFactory: NodeFactory[START]
    ): UuidNodeMatchesFactory[END] => EndRelationReadDelete[START, RELATION, END] = {
    _ => new EndRelationReadDelete(factory, nodeFactory)
  }
}
