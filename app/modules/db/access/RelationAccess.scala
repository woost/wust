package modules.db.access

import controllers.api.nodes.{HyperConnectParameter, ConnectParameter, RequestContext}
import model.WustSchema._
import modules.db.Database._
import modules.db._
import modules.db.types._
import modules.requests.ConnectResponse
import play.api.libs.json.JsValue
import renesca.schema._

trait RelationAccess[NODE <: UuidNode, +OTHER <: UuidNode] {
  //TODO: switch left/right...left should be errors
  def read(context: RequestContext, param: ConnectParameter[NODE]): Either[String, Iterable[OTHER]] = Left("No read access on Relation")
  def read[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,NODE with AbstractRelation[S,E], E]): Either[String, Iterable[OTHER]] = Left("No read access on HyperRelation")
  def delete(context: RequestContext, param: ConnectParameter[NODE], uuid: String): Either[String, Boolean] = Left("No delete access on Relation")
  def delete[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,NODE with AbstractRelation[S,E],E], uuid: String): Either[String, Boolean] = Left("No delete access on HyperRelation")
  def create(context: RequestContext, param: ConnectParameter[NODE]): Either[String, ConnectResponse[OTHER]]
  def create(context: RequestContext, param: ConnectParameter[NODE], uuid: String): Either[String, ConnectResponse[OTHER]] = Left("No create access on Relation")
  def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S,E], E]): Either[String, ConnectResponse[OTHER]]
  def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S,E], E], uuid: String): Either[String, ConnectResponse[OTHER]] = Left("No create access on HyperRelation")

  val nodeFactory: UuidNodeMatchesFactory[OTHER]
}

trait NodeAwareRelationAccess[NODE <: UuidNode, OTHER <: UuidNode] extends RelationAccess[NODE, OTHER] {
  private var nodeAccess: Option[NodeAccess[OTHER]] = None
  def withCreate(access: NodeAccess[OTHER]) = {
    nodeAccess = Some(access)
  }

  private def createNode(context: RequestContext): Either[String, OTHER] = {
    nodeAccess.map(_.create(context)).getOrElse(Left("No factory defined on connect path"))
  }

  def create(context: RequestContext, param: ConnectParameter[NODE]): Either[String, ConnectResponse[OTHER]] = {
    createNode(context).right.toOption.map(n => create(context, param, n.uuid)).getOrElse(Left("Cannot create node on connect path"))
  }
  def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,NODE with AbstractRelation[S,E], E]): Either[String, ConnectResponse[OTHER]] = {
    createNode(context).right.toOption.map(n => create(context, param, n.uuid)).getOrElse(Left("Cannot create node on connect path"))
  }
}

trait StartRelationAccess[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends NodeAwareRelationAccess[START,END] {

  def toNodeDefinition = ConcreteFactoryNodeDefinition(nodeFactory)
  def toNodeDefinition(uuid: String) = FactoryUuidNodeDefinition(nodeFactory, uuid)
  def toBaseNodeDefinition(param: ConnectParameter[START]) = FactoryUuidNodeDefinition(param.baseFactory, param.baseUuid)
  def toBaseNodeDefinition[E <: UuidNode, S <: UuidNode](param: HyperConnectParameter[S, START with AbstractRelation[S,E], E]) = {
    val start = FactoryUuidNodeDefinition(param.startFactory, param.startUuid)
    val end = FactoryUuidNodeDefinition(param.endFactory, param.endUuid)
    HyperNodeDefinition(start, param.baseFactory, end)
  }

  protected def pageAwareRead(context: RequestContext, relDefs: Seq[NodeAndFixedRelationDefinition[START,RELATION,END]]) = {
    context.page.map { page =>
      val skip = page * context.limit
      Right(limitedStartConnectedDiscourseNodes(skip, context.limit, relDefs: _*))
    }.getOrElse(Right(startConnectedDiscourseNodes(relDefs: _*)))
  }
}

trait EndRelationAccess[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends NodeAwareRelationAccess[END,START] {

  def toNodeDefinition = ConcreteFactoryNodeDefinition(nodeFactory)
  def toNodeDefinition(uuid: String) = FactoryUuidNodeDefinition(nodeFactory, uuid)
  def toBaseNodeDefinition(param: ConnectParameter[END]) = FactoryUuidNodeDefinition(param.baseFactory, param.baseUuid)
  def toHyperBaseNodeDefinition[E <: UuidNode, S <: UuidNode](param: HyperConnectParameter[S, END with AbstractRelation[S,E], E]) = {
    val start = FactoryUuidNodeDefinition(param.startFactory, param.startUuid)
    val end = FactoryUuidNodeDefinition(param.endFactory, param.endUuid)
    HyperNodeDefinition(start, param.baseFactory, end)
  }

  protected def pageAwareRead(context: RequestContext, relDefs: Seq[FixedAndNodeRelationDefinition[START,RELATION,END]]) = {
    context.page.map { page =>
      val skip = page * context.limit
      Right(limitedEndConnectedDiscourseNodes(skip, context.limit, relDefs: _*))
    }.getOrElse(Right(endConnectedDiscourseNodes(relDefs: _*)))
  }
}

trait StartRelationReadBase[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends StartRelationAccess[START, RELATION, END] {
  val factory: AbstractRelationFactory[START,RELATION,END]

  override def read(context: RequestContext, param: ConnectParameter[START]) = {
    pageAwareRead(context, Seq(RelationDefinition(toBaseNodeDefinition(param), factory, toNodeDefinition)))
  }

  override def read[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,START with AbstractRelation[S,E], E]) = {
    pageAwareRead(context, Seq(RelationDefinition(toBaseNodeDefinition(param), factory, toNodeDefinition)))
  }
}

trait EndRelationReadBase[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends EndRelationAccess[START, RELATION, END] {
  val factory: AbstractRelationFactory[START,RELATION,END]

  override def read(context: RequestContext, param: ConnectParameter[END]) = {
    pageAwareRead(context, Seq(RelationDefinition(toNodeDefinition, factory, toBaseNodeDefinition(param))))
  }

  override def read[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,END with AbstractRelation[S,E], E]) = {
    pageAwareRead(context, Seq(RelationDefinition(toNodeDefinition, factory, toHyperBaseNodeDefinition(param))))
  }
}

trait StartMultiRelationReadBase[
START <: UuidNode,
END <: UuidNode
] extends StartRelationAccess[START,AbstractRelation[START,END],END] {
  val factories: Seq[AbstractRelationFactory[START,AbstractRelation[START,END],END]]

  override def read(context: RequestContext, param: ConnectParameter[START]) = {
    pageAwareRead(context, factories.map(RelationDefinition(toBaseNodeDefinition(param), _, toNodeDefinition)))
  }

  override def read[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,START with AbstractRelation[S,E], E]) = {
    pageAwareRead(context, factories.map(RelationDefinition(toBaseNodeDefinition(param), _, toNodeDefinition)))
  }
}

trait EndMultiRelationReadBase[
START <: UuidNode,
END <: UuidNode
] extends EndRelationAccess[START,AbstractRelation[START,END],END] {
  val factories: Seq[AbstractRelationFactory[START,AbstractRelation[START,END],END]]

  override def read(context: RequestContext, param: ConnectParameter[END]) = {
    pageAwareRead(context, factories.map(RelationDefinition(toNodeDefinition, _, toBaseNodeDefinition(param))))
  }

  override def read[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,END with AbstractRelation[S,E], E]) = {
    pageAwareRead(context, factories.map(RelationDefinition(toNodeDefinition, _, toHyperBaseNodeDefinition(param))))
  }
}

//TODO: correct results for delete....
trait StartRelationDeleteBase[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends StartRelationAccess[START,RELATION,END] {
  val factory: AbstractRelationFactory[START,RELATION,END]

  override def delete(context: RequestContext, param: ConnectParameter[START], uuid: String) = {
    val relationDefinition = RelationDefinition(toBaseNodeDefinition(param), factory, toNodeDefinition(uuid))
    disconnectNodes(relationDefinition)
    Right(true)
  }

  override def delete[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,START with AbstractRelation[S,E],E], uuid: String) = {
    val relationDefinition = RelationDefinition(toBaseNodeDefinition(param), factory, toNodeDefinition(uuid))
    disconnectNodes(relationDefinition)
    Right(true)
  }
}

trait EndRelationDeleteBase[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends EndRelationAccess[START,RELATION,END] {
  val factory: AbstractRelationFactory[START,RELATION,END]

  override def delete(context: RequestContext, param: ConnectParameter[END], uuid: String) = {
    val relationDefinition = RelationDefinition(toNodeDefinition(uuid), factory, toBaseNodeDefinition(param))
    disconnectNodes(relationDefinition)
    Right(true)
  }

  override def delete[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,END with AbstractRelation[S,E],E], uuid: String) = {
    val relationDefinition = RelationDefinition(toNodeDefinition(uuid), factory, toHyperBaseNodeDefinition(param))
    disconnectNodes(relationDefinition)
    Right(true)
  }
}

case class StartRelationRead[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](factory: AbstractRelationFactory[START,RELATION,END], nodeFactory: UuidNodeMatchesFactory[END]) extends StartRelationReadBase[START,RELATION,END]
case class EndRelationRead[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](factory: AbstractRelationFactory[START,RELATION,END], nodeFactory: UuidNodeMatchesFactory[START]) extends EndRelationReadBase[START,RELATION,END]
case class StartMultiRelationRead[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](factories: AbstractRelationFactory[START,RELATION,END]*)(val nodeFactory: UuidNodeMatchesFactory[END]) extends StartMultiRelationReadBase[START,END]
case class EndMultiRelationRead[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](factories: AbstractRelationFactory[START,RELATION,END]*)(val nodeFactory: UuidNodeMatchesFactory[START]) extends EndMultiRelationReadBase[START,END]
case class StartRelationDelete[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](factory: AbstractRelationFactory[START,RELATION,END], nodeFactory: UuidNodeMatchesFactory[END]) extends StartRelationDeleteBase[START,RELATION,END]
case class EndRelationDelete[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](factory: AbstractRelationFactory[START,RELATION,END], nodeFactory: UuidNodeMatchesFactory[START]) extends EndRelationDeleteBase[START,RELATION,END]
case class StartRelationReadDelete[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](factory: AbstractRelationFactory[START,RELATION,END], nodeFactory: UuidNodeMatchesFactory[END]) extends StartRelationDeleteBase[START,RELATION,END] with StartRelationReadBase[START,RELATION,END]
case class EndRelationReadDelete[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](factory: AbstractRelationFactory[START,RELATION,END], nodeFactory: UuidNodeMatchesFactory[START]) extends EndRelationDeleteBase[START,RELATION,END] with EndRelationReadBase[START,RELATION,END]
