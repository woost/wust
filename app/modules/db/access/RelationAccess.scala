package modules.db.access

import controllers.api.nodes.{HyperConnectParameter, ConnectParameter, RequestContext}
import model.WustSchema._
import modules.db.Database._
import modules.db._
import modules.db.types._
import modules.requests.ConnectResponse
import play.api.libs.json.JsValue
import renesca.schema._
import play.api.mvc.Results._
import play.api.mvc.Result

trait RelationAccess[NODE <: UuidNode, +OTHER <: UuidNode] {
  def read(context: RequestContext, param: ConnectParameter[NODE]): Either[Result, Iterable[OTHER]]
  def read[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,NODE with AbstractRelation[S,E], E]): Either[Result, Iterable[OTHER]]
  def delete(context: RequestContext, param: ConnectParameter[NODE], uuid: String): Either[Result, Boolean]
  def delete[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,NODE with AbstractRelation[S,E],E], uuid: String): Either[Result, Boolean]
  def create(context: RequestContext, param: ConnectParameter[NODE]): Either[Result, ConnectResponse[OTHER]]
  def create(context: RequestContext, param: ConnectParameter[NODE], uuid: String): Either[Result, ConnectResponse[OTHER]]
  def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S,E], E]): Either[Result, ConnectResponse[OTHER]]
  def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S,E], E], uuid: String): Either[Result, ConnectResponse[OTHER]]

  val nodeFactory: UuidNodeMatchesFactory[OTHER]
}

trait RelationAccessDefault[NODE <: UuidNode, OTHER <: UuidNode] extends RelationAccess[NODE, OTHER]{
  def read(context: RequestContext, param: ConnectParameter[NODE]): Either[Result, Iterable[OTHER]] = Left(NotFound("No read access on Relation"))
  def read[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,NODE with AbstractRelation[S,E], E]): Either[Result, Iterable[OTHER]] = Left(NotFound("No read access on HyperRelation"))
  def delete(context: RequestContext, param: ConnectParameter[NODE], uuid: String): Either[Result, Boolean] = Left(NotFound("No delete access on Relation"))
  def delete[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,NODE with AbstractRelation[S,E],E], uuid: String): Either[Result, Boolean] = Left(NotFound("No delete access on HyperRelation"))
  def create(context: RequestContext, param: ConnectParameter[NODE]): Either[Result, ConnectResponse[OTHER]] = Left(NotFound("No json create access on Relation"))
  def create(context: RequestContext, param: ConnectParameter[NODE], uuid: String): Either[Result, ConnectResponse[OTHER]] = Left(NotFound("No create access on Relation"))
  def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S,E], E]): Either[Result, ConnectResponse[OTHER]] = Left(NotFound("No json create access on HyperRelation"))
  def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S,E], E], uuid: String): Either[Result, ConnectResponse[OTHER]] = Left(NotFound("No create access on HyperRelation"))
}

trait StartRelationAccess[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends RelationAccess[START,END] {

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
] extends RelationAccess[END,START] {

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

trait StartRelationAccessDefault[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends StartRelationAccess[START, RELATION, END] with RelationAccessDefault[START,END]

trait EndRelationAccessDefault[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends EndRelationAccess[START, RELATION, END] with RelationAccessDefault[END,START]

trait StartRelationReadBase[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends StartRelationAccessDefault[START, RELATION, END] {
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
] extends EndRelationAccessDefault[START, RELATION, END] {
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
] extends StartRelationAccessDefault[START,AbstractRelation[START,END],END] {
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
] extends EndRelationAccessDefault[START,AbstractRelation[START,END],END] {
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
] extends StartRelationAccessDefault[START,RELATION,END] {
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
] extends EndRelationAccessDefault[START,RELATION,END] {
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

trait RelationAccessDecorator[NODE <: UuidNode, +OTHER <: UuidNode] extends RelationAccess[NODE, OTHER] with AccessDecoratorControlDefault {
  val self: RelationAccess[NODE, OTHER]

  override def read(context: RequestContext, param: ConnectParameter[NODE]) = {
    acceptRequestRead(context).map(Left(_)).getOrElse(self.read(context, param))
  }
  override def read[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,NODE with AbstractRelation[S,E], E]) = {
    acceptRequestRead(context).map(Left(_)).getOrElse(self.read(context, param))
  }
  override def delete(context: RequestContext, param: ConnectParameter[NODE], uuid: String) = {
    acceptRequestWrite(context).map(Left(_)).getOrElse(self.delete(context, param, uuid))
  }
  override def delete[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,NODE with AbstractRelation[S,E],E], uuid: String) = {
    acceptRequestWrite(context).map(Left(_)).getOrElse(self.delete(context, param, uuid))
  }
  override def create(context: RequestContext, param: ConnectParameter[NODE]) = {
    acceptRequestWrite(context).map(Left(_)).getOrElse(self.create(context, param))
  }
  override def create(context: RequestContext, param: ConnectParameter[NODE], uuid: String) = {
    acceptRequestWrite(context).map(Left(_)).getOrElse(self.create(context, param, uuid))
  }
  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S,E], E]) = {
    acceptRequestWrite(context).map(Left(_)).getOrElse(self.create(context, param))
  }
  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S,E], E], uuid: String) = {
    acceptRequestWrite(context).map(Left(_)).getOrElse(self.create(context, param, uuid))
  }

  val nodeFactory = self.nodeFactory
}

trait NodeAwareRelationAccess[NODE <: UuidNode, OTHER <: UuidNode] extends RelationAccessDecorator[NODE, OTHER] {
  val self: RelationAccess[NODE, OTHER]
  val nodeAccess: NodeAccess[OTHER]

  override def create(context: RequestContext, param: ConnectParameter[NODE]) = {
    nodeAccess.create(context) match {
      case Left(err) => Left(err)
      case Right(node) => self.create(context, param, node.uuid)
    }
  }
  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,NODE with AbstractRelation[S,E], E]) = {
    nodeAccess.create(context) match {
      case Left(err) => Left(err)
      case Right(node) => self.create(context, param, node.uuid)
    }
  }
}

case class StartNodeAwareRelationAccess[START <: UuidNode, RELATION <: AbstractRelation[START,END], END <: UuidNode](self: StartRelationAccess[START,RELATION,END], nodeAccess: NodeAccess[END]) extends NodeAwareRelationAccess[START,END] with StartRelationAccess[START,RELATION,END]
case class EndNodeAwareRelationAccess[START <: UuidNode, RELATION <: AbstractRelation[START,END], END <: UuidNode](self: EndRelationAccess[START,RELATION,END], nodeAccess: NodeAccess[START]) extends NodeAwareRelationAccess[END,START] with EndRelationAccess[START,RELATION,END]
case class StartRelationAccessDecoration[START <: UuidNode, RELATION <: AbstractRelation[START,END], END <: UuidNode](self: StartRelationAccess[START,RELATION,END], control: AccessDecoratorControl) extends RelationAccessDecorator[START,END] with StartRelationAccess[START,RELATION,END] with AccessDecoratorControlForward
case class EndRelationAccessDecoration[START <: UuidNode, RELATION <: AbstractRelation[START,END], END <: UuidNode](self: EndRelationAccess[START,RELATION,END], control: AccessDecoratorControl) extends RelationAccessDecorator[END,START] with EndRelationAccess[START,RELATION,END] with AccessDecoratorControlForward
