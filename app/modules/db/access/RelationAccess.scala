package modules.db.access

import controllers.api.nodes.RequestContext
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
  def read(context: RequestContext, uuid: String): Either[Iterable[OTHER], String] = Right("No read access on Relation")
  def readHyper(context: RequestContext, baseDef: HyperNodeDefinitionBase[NODE with AbstractRelation[_, _]]): Either[Iterable[OTHER], String] = Right("No read access on HyperRelation")
  def delete(context: RequestContext, uuid: String, nestedUuid: String): Either[Boolean, String] = Right("No delete access on Relation")
  def deleteHyper(context: RequestContext, baseDef: HyperNodeDefinitionBase[NODE with AbstractRelation[_, _]], uuid: String): Either[Boolean, String] = Right("No delete access on HyperRelation")
  def create(context: RequestContext, uuid: String): Either[ConnectResponse[OTHER], String]
  def create(context: RequestContext, uuid: String, nestedUuid: String): Either[ConnectResponse[OTHER], String] = Right("No create access on Relation")
  def createHyper(context: RequestContext, startUuid: String, endUuid: String): Either[ConnectResponse[OTHER], String]
  def createHyper(context: RequestContext, startUuid: String, endUuid: String, nestedUuid: String): Either[ConnectResponse[OTHER], String] = Right("No create access on HyperRelation")
}

trait NodeAwareRelationAccess[NODE <: UuidNode, OTHER <: UuidNode] extends RelationAccess[NODE, OTHER] {
  private var nodeAccess: Option[NodeAccess[OTHER]] = None
  def withCreate(access: NodeAccess[OTHER]) = {
    nodeAccess = Some(access)
  }

  private def createNode(context: RequestContext): Either[OTHER, String] = {
    nodeAccess.map(_.create(context)).getOrElse(Right("No factory defined on connect path"))
  }

  def create(context: RequestContext, uuid: String): Either[ConnectResponse[OTHER], String] = {
    createNode(context).left.toOption.map(n => create(context, uuid, n.uuid)).getOrElse(Right("Cannot create node on connect path"))
  }
  def createHyper(context: RequestContext, startUuid: String, endUuid: String): Either[ConnectResponse[OTHER], String] = {
    createNode(context).left.toOption.map(n => createHyper(context, startUuid, endUuid, n.uuid)).getOrElse(Right("Cannot create node on connect path"))
  }
}

trait StartRelationAccess[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends NodeAwareRelationAccess[START,END] {
  val nodeFactory: UuidNodeMatchesFactory[END]
  val baseFactory: UuidNodeMatchesFactory[START]

  def toNodeDefinition = ConcreteFactoryNodeDefinition(nodeFactory)
  def toNodeDefinition(uuid: String) = FactoryUuidNodeDefinition(nodeFactory, uuid)
  def toBaseNodeDefinition(uuid: String) = FactoryUuidNodeDefinition(baseFactory, uuid)

  protected def pageAwareRead(context: RequestContext, relDefs: Seq[NodeAndFixedRelationDefinition[START,RELATION,END]]) = {
    context.page.map { page =>
      val skip = page * context.limit
      Left(limitedStartConnectedDiscourseNodes(skip, context.limit, relDefs: _*))
    }.getOrElse(Left(startConnectedDiscourseNodes(relDefs: _*)))
  }
}

trait EndRelationAccess[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends NodeAwareRelationAccess[END,START] {
  val nodeFactory: UuidNodeMatchesFactory[START]
  val baseFactory: UuidNodeMatchesFactory[END]

  def toNodeDefinition = ConcreteFactoryNodeDefinition(nodeFactory)
  def toNodeDefinition(uuid: String) = FactoryUuidNodeDefinition(nodeFactory, uuid)
  def toBaseNodeDefinition(uuid: String) = FactoryUuidNodeDefinition(baseFactory, uuid)

  protected def pageAwareRead(context: RequestContext, relDefs: Seq[FixedAndNodeRelationDefinition[START,RELATION,END]]) = {
    context.page.map { page =>
      val skip = page * context.limit
      Left(limitedEndConnectedDiscourseNodes(skip, context.limit, relDefs: _*))
    }.getOrElse(Left(endConnectedDiscourseNodes(relDefs: _*)))
  }
}

class StartMultiRelationRead[
START <: UuidNode,
END <: UuidNode
](
  factories: AbstractRelationFactory[START,AbstractRelation[START,END],END]*
)(
  val nodeFactory: UuidNodeMatchesFactory[END],
  val baseFactory: UuidNodeMatchesFactory[START]
) extends StartRelationAccess[START,AbstractRelation[START,END],END] {

  override def read(context: RequestContext, uuid: String) = {
    pageAwareRead(context, factories.map(RelationDefinition(toBaseNodeDefinition(uuid), _, toNodeDefinition)))
  }

  override def readHyper(context: RequestContext, baseDef: HyperNodeDefinitionBase[START with AbstractRelation[_, _]]) = {
    pageAwareRead(context, factories.map(RelationDefinition(baseDef, _, toNodeDefinition)))
  }
}

object StartMultiRelationRead {
  def apply[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](
    factories: AbstractRelationFactory[START, RELATION, END]*
    )(
    nodeFactory: UuidNodeMatchesFactory[END]
    ): UuidNodeMatchesFactory[START] => StartMultiRelationRead[START, END] = {
    baseFactory => new StartMultiRelationRead(factories: _*)(nodeFactory, baseFactory)
  }
}

class EndMultiRelationRead[
START <: UuidNode,
END <: UuidNode
](
  factories: AbstractRelationFactory[START,AbstractRelation[START,END],END]*
)(
  val nodeFactory: UuidNodeMatchesFactory[START],
  val baseFactory: UuidNodeMatchesFactory[END]
) extends EndRelationAccess[START,AbstractRelation[START,END],END] {

  override def read(context: RequestContext, uuid: String) = {
    pageAwareRead(context, factories.map(RelationDefinition(toNodeDefinition, _, toBaseNodeDefinition(uuid))))
  }

  override def readHyper(context: RequestContext, baseDef: HyperNodeDefinitionBase[END with AbstractRelation[_, _]]) = {
    pageAwareRead(context, factories.map(RelationDefinition(toNodeDefinition, _, baseDef)))
  }
}

object EndMultiRelationRead {
  def apply[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](
    factories: AbstractRelationFactory[START, RELATION, END]*
    )(
    nodeFactory: UuidNodeMatchesFactory[START]
    ): UuidNodeMatchesFactory[END] => EndMultiRelationRead[START, END] = {
    baseFactory => new EndMultiRelationRead(factories: _*)(nodeFactory, baseFactory)
  }
}

class StartRelationRead[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](
  val factory: AbstractRelationFactory[START, RELATION, END],
  val nodeFactory: UuidNodeMatchesFactory[END],
  val baseFactory: UuidNodeMatchesFactory[START]
  ) extends StartRelationAccess[START, RELATION, END] {

  override def read(context: RequestContext, uuid: String) = {
    pageAwareRead(context, Seq(RelationDefinition(toBaseNodeDefinition(uuid), factory, toNodeDefinition)))
  }

  override def readHyper(context: RequestContext, baseDef: HyperNodeDefinitionBase[START with AbstractRelation[_, _]]) = {
    pageAwareRead(context, Seq(RelationDefinition(baseDef, factory, toNodeDefinition)))
  }
}

object StartRelationRead {
  def apply[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](
    factory: AbstractRelationFactory[START, RELATION, END],
    nodeFactory: UuidNodeMatchesFactory[END]
    ): UuidNodeMatchesFactory[START] => StartRelationRead[START, RELATION, END] = {
    baseFactory => new StartRelationRead(factory, nodeFactory, baseFactory)
  }
}

class EndRelationRead[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](
  val factory: AbstractRelationFactory[START, RELATION, END],
  val nodeFactory: UuidNodeMatchesFactory[START],
  val baseFactory: UuidNodeMatchesFactory[END]
  ) extends EndRelationAccess[START, RELATION, END] {

  override def read(context: RequestContext, uuid: String) = {
    pageAwareRead(context, Seq(RelationDefinition(toNodeDefinition, factory, toBaseNodeDefinition(uuid))))
  }

  override def readHyper(context: RequestContext, baseDef: HyperNodeDefinitionBase[END with AbstractRelation[_, _]]) = {
    pageAwareRead(context, Seq(RelationDefinition(toNodeDefinition, factory, baseDef)))
  }
}

object EndRelationRead {
  def apply[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](
    factory: AbstractRelationFactory[START, RELATION, END],
    nodeFactory: UuidNodeMatchesFactory[START]
    ): UuidNodeMatchesFactory[END] => EndRelationRead[START, RELATION, END] = {
    baseFactory => new EndRelationRead(factory, nodeFactory, baseFactory)
  }
}

//TODO: correct results for delete....
class StartRelationReadDelete[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](
  factory: AbstractRelationFactory[START, RELATION, END],
  nodeFactory: UuidNodeMatchesFactory[END],
  baseFactory: UuidNodeMatchesFactory[START]
  ) extends StartRelationRead(factory, nodeFactory, baseFactory) {

  override def delete(context: RequestContext, uuid: String, nestedUuid: String) = {
    val relationDefinition = RelationDefinition(toBaseNodeDefinition(uuid), factory, toNodeDefinition(nestedUuid))
    disconnectNodes(relationDefinition)
    Left(true)
  }

  override def deleteHyper(context: RequestContext, baseDef: HyperNodeDefinitionBase[START with AbstractRelation[_, _]], uuid: String) = {
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
    nodeFactory: UuidNodeMatchesFactory[END]
    ): UuidNodeMatchesFactory[START] => StartRelationReadDelete[START, RELATION, END] = {
    baseFactory => new StartRelationReadDelete(factory, nodeFactory, baseFactory)
  }
}

class EndRelationReadDelete[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](
  factory: AbstractRelationFactory[START, RELATION, END],
  nodeFactory: UuidNodeMatchesFactory[START],
  baseFactory: UuidNodeMatchesFactory[END]
  ) extends EndRelationRead(factory, nodeFactory, baseFactory) {

  override def delete(context: RequestContext, uuid: String, nestedUuid: String) = {
    val relationDefinition = RelationDefinition(toNodeDefinition(nestedUuid), factory, toBaseNodeDefinition(uuid))
    disconnectNodes(relationDefinition)
    Left(true)
  }

  override def deleteHyper(context: RequestContext, baseDef: HyperNodeDefinitionBase[END with AbstractRelation[_, _]], uuid: String) = {
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
    nodeFactory: UuidNodeMatchesFactory[START]
    ): UuidNodeMatchesFactory[END] => EndRelationReadDelete[START, RELATION, END] = {
    baseFactory => new EndRelationReadDelete(factory, nodeFactory, baseFactory)
  }
}
