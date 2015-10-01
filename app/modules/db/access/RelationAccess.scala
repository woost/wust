package modules.db.access

import controllers.api.nodes.{ConnectParameter, HyperConnectParameter, RequestContext}
import model.WustSchema._
import modules.db.Database._
import modules.db._
import modules.db.types._
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._
import renesca.schema._

trait RelationAccess[-NODE <: UuidNode, +OTHER <: UuidNode] {
  def read(context: RequestContext, param: ConnectParameter[NODE]): Result
  def read[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S, E], E]): Result
  def delete(context: RequestContext, param: ConnectParameter[NODE], uuid: String): Result
  def delete[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S, E], E], uuid: String): Result
  def create(context: RequestContext, param: ConnectParameter[NODE]): Result
  def create(context: RequestContext, param: ConnectParameter[NODE], uuid: String): Result
  def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S, E], E]): Result
  def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S, E], E], uuid: String): Result

  val nodeFactory: UuidNodeMatchesFactory[OTHER]
}

trait RelationAccessDefault[NODE <: UuidNode, OTHER <: UuidNode] extends RelationAccess[NODE, OTHER] {
  def read(context: RequestContext, param: ConnectParameter[NODE]) = NotFound("No read access on Relation")
  def read[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S, E], E]) = NotFound("No read access on HyperRelation")
  def delete(context: RequestContext, param: ConnectParameter[NODE], uuid: String) = NotFound("No delete access on Relation")
  def delete[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S, E], E], uuid: String) = NotFound("No delete access on HyperRelation")
  def create(context: RequestContext, param: ConnectParameter[NODE]) = NotFound("No json create access on Relation")
  def create(context: RequestContext, param: ConnectParameter[NODE], uuid: String) = NotFound("No create access on Relation")
  def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S, E], E]) = NotFound("No json create access on HyperRelation")
  def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S, E], E], uuid: String) = NotFound("No create access on HyperRelation")
}

trait StartRelationReader[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends RelationAccess[START, END] with FormattingNode[END] {
  protected def pageAwareRead(context: RequestContext, relDefs: Seq[NodeAndFixedRelationDefinition[START, RELATION, END]]) = {
    context.page.map { page =>
      val skip = page * context.limit
      Ok(Json.toJson(limitedStartConnectedDiscourseNodes(skip, context.limit, relDefs: _*)))
    }.getOrElse(Ok(Json.toJson((startConnectedDiscourseNodes(relDefs: _*)))))
  }
}

trait EndRelationReader[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends RelationAccess[END, START] with FormattingNode[START] {
  protected def pageAwareRead(context: RequestContext, relDefs: Seq[FixedAndNodeRelationDefinition[START, RELATION, END]]) = {
    context.page.map { page =>
      val skip = page * context.limit
      Ok(Json.toJson(limitedEndConnectedDiscourseNodes(skip, context.limit, relDefs: _*)))
    }.getOrElse(Ok(Json.toJson(endConnectedDiscourseNodes(relDefs: _*))))
  }
}

trait StartRelationAccess[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends RelationAccess[START, END] {

  def toNodeDefinition(implicit ctx: QueryContext) = FactoryNodeDefinition(nodeFactory)
  def toNodeDefinition(uuid: String)(implicit ctx: QueryContext) = FactoryUuidNodeDefinition(nodeFactory, uuid)
  def toBaseNodeDefinition(param: ConnectParameter[START])(implicit ctx: QueryContext) = FactoryUuidNodeDefinition(param.baseFactory, param.baseUuid)
  def toBaseNodeDefinition[E <: UuidNode, S <: UuidNode](param: HyperConnectParameter[S, START with AbstractRelation[S, E], E])(implicit ctx: QueryContext) = {
    val start = FactoryUuidNodeDefinition(param.startFactory, param.startUuid)
    val end = FactoryUuidNodeDefinition(param.endFactory, param.endUuid)
    HyperNodeDefinition(start, param.baseFactory, end)
  }
}

trait EndRelationAccess[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends RelationAccess[END, START] {

  def toNodeDefinition(implicit ctx: QueryContext) = FactoryNodeDefinition(nodeFactory)
  def toNodeDefinition(uuid: String)(implicit ctx: QueryContext) = FactoryUuidNodeDefinition(nodeFactory, uuid)
  def toBaseNodeDefinition(param: ConnectParameter[END])(implicit ctx: QueryContext) = FactoryUuidNodeDefinition(param.baseFactory, param.baseUuid)
  def toBaseNodeDefinition[E <: UuidNode, S <: UuidNode](param: HyperConnectParameter[S, END with AbstractRelation[S, E], E])(implicit ctx: QueryContext) = {
    val start = FactoryUuidNodeDefinition(param.startFactory, param.startUuid)
    val end = FactoryUuidNodeDefinition(param.endFactory, param.endUuid)
    HyperNodeDefinition(start, param.baseFactory, end)
  }
}

trait StartRelationAccessDefault[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends StartRelationAccess[START, RELATION, END] with RelationAccessDefault[START, END]

trait EndRelationAccessDefault[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends EndRelationAccess[START, RELATION, END] with RelationAccessDefault[END, START]

trait StartRelationReadBase[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends StartRelationAccessDefault[START, RELATION, END] with StartRelationReader[START, RELATION, END] {
  val factory: AbstractRelationFactory[START, RELATION, END]

  override def read(context: RequestContext, param: ConnectParameter[START]) = {
    implicit val ctx = new QueryContext
    pageAwareRead(context, Seq(RelationDefinition(toBaseNodeDefinition(param), factory, toNodeDefinition)))
  }

  override def read[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, START with AbstractRelation[S, E], E]) = {
    implicit val ctx = new QueryContext
    pageAwareRead(context, Seq(RelationDefinition(toBaseNodeDefinition(param), factory, toNodeDefinition)))
  }
}

trait EndRelationReadBase[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends EndRelationAccessDefault[START, RELATION, END] with EndRelationReader[START, RELATION, END] {
  val factory: AbstractRelationFactory[START, RELATION, END]

  override def read(context: RequestContext, param: ConnectParameter[END]) = {
    implicit val ctx = new QueryContext
    pageAwareRead(context, Seq(RelationDefinition(toNodeDefinition, factory, toBaseNodeDefinition(param))))
  }

  override def read[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, END with AbstractRelation[S, E], E]) = {
    implicit val ctx = new QueryContext
    pageAwareRead(context, Seq(RelationDefinition(toNodeDefinition, factory, toBaseNodeDefinition(param))))
  }
}

trait StartMultiRelationReadBase[
START <: UuidNode,
END <: UuidNode
] extends StartRelationAccessDefault[START, AbstractRelation[START, END], END] with StartRelationReader[START, AbstractRelation[START, END], END] {
  val factories: Seq[AbstractRelationFactory[START, AbstractRelation[START, END], END]]

  override def read(context: RequestContext, param: ConnectParameter[START]) = {
    implicit val ctx = new QueryContext
    pageAwareRead(context, factories.map(RelationDefinition(toBaseNodeDefinition(param), _, toNodeDefinition)))
  }

  override def read[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, START with AbstractRelation[S, E], E]) = {
    implicit val ctx = new QueryContext
    pageAwareRead(context, factories.map(RelationDefinition(toBaseNodeDefinition(param), _, toNodeDefinition)))
  }
}

trait EndMultiRelationReadBase[
START <: UuidNode,
END <: UuidNode
] extends EndRelationAccessDefault[START, AbstractRelation[START, END], END] with EndRelationReader[START, AbstractRelation[START, END], END] {
  val factories: Seq[AbstractRelationFactory[START, AbstractRelation[START, END], END]]

  override def read(context: RequestContext, param: ConnectParameter[END]) = {
    implicit val ctx = new QueryContext
    pageAwareRead(context, factories.map(RelationDefinition(toNodeDefinition, _, toBaseNodeDefinition(param))))
  }

  override def read[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, END with AbstractRelation[S, E], E]) = {
    implicit val ctx = new QueryContext
    pageAwareRead(context, factories.map(RelationDefinition(toNodeDefinition, _, toBaseNodeDefinition(param))))
  }
}

trait StartRelationDeleteBase[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends StartRelationAccessDefault[START, RELATION, END] {
  val factory: AbstractRelationFactory[START, RELATION, END]

  override def delete(context: RequestContext, param: ConnectParameter[START], uuid: String) = context.withUser {
    implicit val ctx = new QueryContext
    val relationDefinition = RelationDefinition(toBaseNodeDefinition(param), factory, toNodeDefinition(uuid))
    if(disconnectNodes(relationDefinition))
      NoContent
    else
      BadRequest("Cannot delete relation")
  }

  override def delete[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, START with AbstractRelation[S, E], E], uuid: String) = context.withUser {
    implicit val ctx = new QueryContext
    val relationDefinition = RelationDefinition(toBaseNodeDefinition(param), factory, toNodeDefinition(uuid))
    if(disconnectNodes(relationDefinition))
      NoContent
    else
      BadRequest("Cannot delete relation")
  }
}

trait EndRelationDeleteBase[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends EndRelationAccessDefault[START, RELATION, END] {
  val factory: AbstractRelationFactory[START, RELATION, END]

  override def delete(context: RequestContext, param: ConnectParameter[END], uuid: String) = context.withUser {
    implicit val ctx = new QueryContext
    val relationDefinition = RelationDefinition(toNodeDefinition(uuid), factory, toBaseNodeDefinition(param))
    if(disconnectNodes(relationDefinition))
      NoContent
    else
      BadRequest("Cannot delete relation")
  }

  override def delete[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, END with AbstractRelation[S, E], E], uuid: String) = context.withUser {
    implicit val ctx = new QueryContext
    val relationDefinition = RelationDefinition(toNodeDefinition(uuid), factory, toBaseNodeDefinition(param))
    if(disconnectNodes(relationDefinition))
      NoContent
    else
      BadRequest("Cannot delete relation")
  }
}

case class StartRelationRead[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](factory: AbstractRelationFactory[START, RELATION, END], nodeFactory: UuidNodeMatchesFactory[END])(implicit val format: Format[END]) extends StartRelationReadBase[START, RELATION, END]
case class EndRelationRead[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](factory: AbstractRelationFactory[START, RELATION, END], nodeFactory: UuidNodeMatchesFactory[START])(implicit val format: Format[START]) extends EndRelationReadBase[START, RELATION, END]
case class StartMultiRelationRead[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](factories: AbstractRelationFactory[START, RELATION, END]*)(val nodeFactory: UuidNodeMatchesFactory[END])(implicit val format: Format[END]) extends StartMultiRelationReadBase[START, END]
case class EndMultiRelationRead[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](factories: AbstractRelationFactory[START, RELATION, END]*)(val nodeFactory: UuidNodeMatchesFactory[START])(implicit val format: Format[START]) extends EndMultiRelationReadBase[START, END]
case class StartRelationDelete[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](factory: AbstractRelationFactory[START, RELATION, END], nodeFactory: UuidNodeMatchesFactory[END]) extends StartRelationDeleteBase[START, RELATION, END]
case class EndRelationDelete[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](factory: AbstractRelationFactory[START, RELATION, END], nodeFactory: UuidNodeMatchesFactory[START]) extends EndRelationDeleteBase[START, RELATION, END]
case class StartRelationReadDelete[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](factory: AbstractRelationFactory[START, RELATION, END], nodeFactory: UuidNodeMatchesFactory[END])(implicit val format: Format[END]) extends StartRelationDeleteBase[START, RELATION, END] with StartRelationReadBase[START, RELATION, END]
case class EndRelationReadDelete[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](factory: AbstractRelationFactory[START, RELATION, END], nodeFactory: UuidNodeMatchesFactory[START])(implicit val format: Format[START]) extends EndRelationDeleteBase[START, RELATION, END] with EndRelationReadBase[START, RELATION, END]
case class StartRelationNothing[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](factory: AbstractRelationFactory[START, RELATION, END], nodeFactory: UuidNodeMatchesFactory[END]) extends StartRelationAccessDefault[START, RELATION, END]
case class EndRelationNothing[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](factory: AbstractRelationFactory[START, RELATION, END], nodeFactory: UuidNodeMatchesFactory[START]) extends EndRelationAccessDefault[START, RELATION, END]

trait RelationAccessDecorator[NODE <: UuidNode, OTHER <: UuidNode] extends RelationAccess[NODE, OTHER] with AccessDecoratorControlDefault {
  val self: RelationAccess[NODE, OTHER]

  override def read(context: RequestContext, param: ConnectParameter[NODE]) = {
    acceptRequestRead(context).getOrElse(self.read(context, param))
  }
  override def read[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S, E], E]) = {
    acceptRequestRead(context).getOrElse(self.read(context, param))
  }
  override def delete(context: RequestContext, param: ConnectParameter[NODE], uuid: String) = {
    acceptRequestWrite(context).getOrElse(self.delete(context, param, uuid))
  }
  override def delete[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S, E], E], uuid: String) = {
    acceptRequestWrite(context).getOrElse(self.delete(context, param, uuid))
  }
  override def create(context: RequestContext, param: ConnectParameter[NODE]) = {
    acceptRequestWrite(context).getOrElse(self.create(context, param))
  }
  override def create(context: RequestContext, param: ConnectParameter[NODE], uuid: String) = {
    acceptRequestWrite(context).getOrElse(self.create(context, param, uuid))
  }
  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S, E], E]) = {
    acceptRequestWrite(context).getOrElse(self.create(context, param))
  }
  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S, E], E], uuid: String) = {
    acceptRequestWrite(context).getOrElse(self.create(context, param, uuid))
  }

  val nodeFactory = self.nodeFactory
}

trait RelatedAccess extends RelationAccessDefault[UuidNode, UuidNode] {
  override val nodeFactory = UuidNodeMatches
}

case class StartRelationAccessDecoration[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](self: StartRelationAccess[START, RELATION, END], control: AccessDecoratorControl) extends RelationAccessDecorator[START, END] with StartRelationAccess[START, RELATION, END] with AccessDecoratorControlForward
case class EndRelationAccessDecoration[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](self: EndRelationAccess[START, RELATION, END], control: AccessDecoratorControl) extends RelationAccessDecorator[END, START] with EndRelationAccess[START, RELATION, END] with AccessDecoratorControlForward
