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

trait StartRelationReader[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode] extends RelationAccess[START, END] with FormattingNode[END] {
  protected def pageAwareRead(context: RequestContext, relDefs: Seq[NodeAndFixedRelationDef[START, RELATION, END]])(implicit ctx: QueryContext) = {
    context.page.map { page =>
      val skip = page * context.sizeWithDefault
      Ok(Json.toJson(limitedStartConnectedDiscourseNodes(skip, context.sizeWithDefault, relDefs: _*)))
    }.getOrElse(Ok(Json.toJson((startConnectedDiscourseNodes(relDefs: _*)))))
  }
}

trait EndRelationReader[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode] extends RelationAccess[END, START] with FormattingNode[START] {
  protected def pageAwareRead(context: RequestContext, relDefs: Seq[FixedAndNodeRelationDef[START, RELATION, END]])(implicit ctx: QueryContext) = {
    context.page.map { page =>
      val skip = page * context.sizeWithDefault
      Ok(Json.toJson(limitedEndConnectedDiscourseNodes(skip, context.sizeWithDefault, relDefs: _*)))
    }.getOrElse(Ok(Json.toJson(endConnectedDiscourseNodes(relDefs: _*))))
  }
}

trait StartRelationAccess[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode] extends RelationAccess[START, END] {

  def toNodeDef(implicit ctx: QueryContext) = FactoryNodeDef(nodeFactory)
  def toNodeDef(uuid: String)(implicit ctx: QueryContext) = FactoryUuidNodeDef(nodeFactory, uuid)
  def toBaseNodeDef(param: ConnectParameter[START])(implicit ctx: QueryContext) = FactoryUuidNodeDef(param.factory, param.baseUuid)
  def toBaseNodeDef[E <: UuidNode, S <: UuidNode](param: HyperConnectParameter[S, START with AbstractRelation[S, E], E])(implicit ctx: QueryContext) = {
    val start = FactoryUuidNodeDef(param.startFactory, param.startUuid)
    val end = FactoryUuidNodeDef(param.endFactory, param.endUuid)
    HyperNodeDef(start, param.factory, end)
  }
}

trait EndRelationAccess[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode] extends RelationAccess[END, START] {

  def toNodeDef(implicit ctx: QueryContext) = FactoryNodeDef(nodeFactory)
  def toNodeDef(uuid: String)(implicit ctx: QueryContext) = FactoryUuidNodeDef(nodeFactory, uuid)
  def toBaseNodeDef(param: ConnectParameter[END])(implicit ctx: QueryContext) = FactoryUuidNodeDef(param.factory, param.baseUuid)
  def toBaseNodeDef[E <: UuidNode, S <: UuidNode](param: HyperConnectParameter[S, END with AbstractRelation[S, E], E])(implicit ctx: QueryContext) = {
    val start = FactoryUuidNodeDef(param.startFactory, param.startUuid)
    val end = FactoryUuidNodeDef(param.endFactory, param.endUuid)
    HyperNodeDef(start, param.factory, end)
  }
}

trait StartRelationAccessDefault[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode] extends StartRelationAccess[START, RELATION, END] with RelationAccessDefault[START, END]

trait EndRelationAccessDefault[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode] extends EndRelationAccess[START, RELATION, END] with RelationAccessDefault[END, START]

trait StartRelationReadBase[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode] extends StartRelationAccessDefault[START, RELATION, END] with StartRelationReader[START, RELATION, END] {
  val factory: AbstractRelationFactory[START, RELATION, END]

  override def read(context: RequestContext, param: ConnectParameter[START]) = {
    implicit val ctx = new QueryContext
    pageAwareRead(context, Seq(RelationDef(toBaseNodeDef(param), factory, toNodeDef)))
  }

  override def read[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, START with AbstractRelation[S, E], E]) = {
    implicit val ctx = new QueryContext
    pageAwareRead(context, Seq(RelationDef(toBaseNodeDef(param), factory, toNodeDef)))
  }
}

trait EndRelationReadBase[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode] extends EndRelationAccessDefault[START, RELATION, END] with EndRelationReader[START, RELATION, END] {
  val factory: AbstractRelationFactory[START, RELATION, END]

  override def read(context: RequestContext, param: ConnectParameter[END]) = {
    implicit val ctx = new QueryContext
    pageAwareRead(context, Seq(RelationDef(toNodeDef, factory, toBaseNodeDef(param))))
  }

  override def read[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, END with AbstractRelation[S, E], E]) = {
    implicit val ctx = new QueryContext
    pageAwareRead(context, Seq(RelationDef(toNodeDef, factory, toBaseNodeDef(param))))
  }
}

trait StartMultiRelationReadBase[START <: UuidNode, END <: UuidNode] extends StartRelationAccessDefault[START, AbstractRelation[START, END], END] with StartRelationReader[START, AbstractRelation[START, END], END] {
  val factories: Seq[AbstractRelationFactory[START, AbstractRelation[START, END], END]]

  override def read(context: RequestContext, param: ConnectParameter[START]) = {
    implicit val ctx = new QueryContext
    pageAwareRead(context, factories.map(RelationDef(toBaseNodeDef(param), _, toNodeDef)))
  }

  override def read[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, START with AbstractRelation[S, E], E]) = {
    implicit val ctx = new QueryContext
    pageAwareRead(context, factories.map(RelationDef(toBaseNodeDef(param), _, toNodeDef)))
  }
}

trait EndMultiRelationReadBase[START <: UuidNode, END <: UuidNode] extends EndRelationAccessDefault[START, AbstractRelation[START, END], END] with EndRelationReader[START, AbstractRelation[START, END], END] {
  val factories: Seq[AbstractRelationFactory[START, AbstractRelation[START, END], END]]

  override def read(context: RequestContext, param: ConnectParameter[END]) = {
    implicit val ctx = new QueryContext
    pageAwareRead(context, factories.map(RelationDef(toNodeDef, _, toBaseNodeDef(param))))
  }

  override def read[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, END with AbstractRelation[S, E], E]) = {
    implicit val ctx = new QueryContext
    pageAwareRead(context, factories.map(RelationDef(toNodeDef, _, toBaseNodeDef(param))))
  }
}

trait StartRelationDeleteBase[START <: UuidNode, RELATION <: MatchableRelation[START, END], END <: UuidNode] extends StartRelationAccessDefault[START, RELATION, END] {
  val factory: MatchableRelationFactory[START, RELATION, END]

  override def delete(context: RequestContext, param: ConnectParameter[START], otherUuid: String) = context.withUser {
    val base = param.factory.matchesOnUuid(param.baseUuid)
    val node = nodeFactory.matchesOnUuid(otherUuid)
    val relation = factory.matchesMatchableRelation(base, node)
    val discourse = Discourse(base, node)
    discourse.remove(relation)
    db.transaction(_.persistChanges(discourse)).map(err =>
      BadRequest(s"Cannot delete Relation: $err'")).getOrElse(NoContent)
  }

  override def delete[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, START with AbstractRelation[S, E], E], uuid: String) = context.withUser {
    val start = param.startFactory.matchesOnUuid(param.startUuid)
    val end = param.endFactory.matchesOnUuid(param.endUuid)
    val base = param.factory.matchesMatchableRelation(start, end)
    val node = nodeFactory.matchesOnUuid(uuid)
    val relation = factory.matchesMatchableRelation(base, node)
    val discourse = Discourse(base)
    discourse.remove(relation)
    db.transaction(_.persistChanges(discourse)).map(err =>
      BadRequest(s"Cannot delete Relation: $err'")).getOrElse(NoContent)
  }
}

trait EndRelationDeleteBase[START <: UuidNode, RELATION <: MatchableRelation[START, END], END <: UuidNode] extends EndRelationAccessDefault[START, RELATION, END] {
  val factory: MatchableRelationFactory[START, RELATION, END]

  override def delete(context: RequestContext, param: ConnectParameter[END], otherUuid: String) = context.withUser {
    val base = param.factory.matchesOnUuid(param.baseUuid)
    val node = nodeFactory.matchesOnUuid(otherUuid)
    val relation = factory.matchesMatchableRelation(node, base)
    val discourse = Discourse(base, node)
    discourse.remove(relation)
    db.transaction(_.persistChanges(discourse)).map(err =>
      BadRequest(s"Cannot delete Relation: $err'")).getOrElse(NoContent)
  }

  override def delete[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, END with AbstractRelation[S, E], E], uuid: String) = context.withUser {
    val start = param.startFactory.matchesOnUuid(param.startUuid)
    val end = param.endFactory.matchesOnUuid(param.endUuid)
    val base = param.factory.matchesMatchableRelation(start, end)
    val node = nodeFactory.matchesOnUuid(uuid)
    val relation = factory.matchesMatchableRelation(node, base)
    val discourse = Discourse(base, node)
    discourse.remove(relation)
    db.transaction(_.persistChanges(discourse)).map(err =>
      BadRequest(s"Cannot delete Relation: $err'")).getOrElse(NoContent)
  }
}

trait StartRelationWriteBase[START <: UuidNode, RELATION <: ConstructRelation[START, END], END <: UuidNode] extends StartRelationAccessDefault[START, RELATION, END] with FormattingNode[END] {
  val factory: ConstructRelationFactory[START, RELATION, END]
  val nodeFactory: UuidNodeMatchesFactory[END]

  override def create(context: RequestContext, param: ConnectParameter[START], otherUuid: String) = context.withUser {
    val base = param.factory.matchesOnUuid(param.baseUuid)
    val node = nodeFactory.matchesOnUuid(otherUuid)
    val relation = factory.mergeConstructRelation(base, node)
    db.transaction(_.persistChanges(relation)).map(err =>
      BadRequest(s"Cannot create Relation: $err'")).getOrElse(Ok(Json.toJson(node)))
  }

  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, START with AbstractRelation[S, E], E], uuid: String) = context.withUser {
    val start = param.startFactory.matchesOnUuid(param.startUuid)
    val end = param.endFactory.matchesOnUuid(param.endUuid)
    val base = param.factory.matchesMatchableRelation(start, end)
    val node = nodeFactory.matchesOnUuid(uuid)
    val relation = factory.mergeConstructRelation(base, node)
    db.transaction(_.persistChanges(relation)).map(err =>
      BadRequest(s"Cannot create Relation: $err'")).getOrElse(Ok(Json.toJson(node)))
  }
}

trait EndRelationWriteBase[START <: UuidNode, RELATION <: ConstructRelation[START, END], END <: UuidNode] extends EndRelationAccessDefault[START, RELATION, END] with FormattingNode[START] {
  val factory: ConstructRelationFactory[START, RELATION, END]
  val nodeFactory: UuidNodeMatchesFactory[START]

  override def create(context: RequestContext, param: ConnectParameter[END], otherUuid: String) = context.withUser {
    val base = param.factory.matchesOnUuid(param.baseUuid)
    val node = nodeFactory.matchesOnUuid(otherUuid)
    val relation = factory.mergeConstructRelation(node, base)
    db.transaction(_.persistChanges(relation)).map(err =>
      BadRequest(s"Cannot create Relation: $err'")).getOrElse(Ok(Json.toJson(node)))
  }

  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, END with AbstractRelation[S, E], E], uuid: String) = context.withUser {
    val start = param.startFactory.matchesOnUuid(param.startUuid)
    val end = param.endFactory.matchesOnUuid(param.endUuid)
    val base = param.factory.matchesMatchableRelation(start, end)
    val node = nodeFactory.matchesOnUuid(uuid)
    val relation = factory.mergeConstructRelation(node, base)
    db.transaction(_.persistChanges(relation)).map(err =>
      BadRequest(s"Cannot create Relation: $err'")).getOrElse(Ok(Json.toJson(node)))
  }
}

case class StartRelationRead[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](factory: AbstractRelationFactory[START, RELATION, END], nodeFactory: UuidNodeMatchesFactory[END])(implicit val format: Format[END]) extends StartRelationReadBase[START, RELATION, END]
case class EndRelationRead[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](factory: AbstractRelationFactory[START, RELATION, END], nodeFactory: UuidNodeMatchesFactory[START])(implicit val format: Format[START]) extends EndRelationReadBase[START, RELATION, END]
case class StartMultiRelationRead[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](factories: AbstractRelationFactory[START, RELATION, END]*)(val nodeFactory: UuidNodeMatchesFactory[END])(implicit val format: Format[END]) extends StartMultiRelationReadBase[START, END]
case class EndMultiRelationRead[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](factories: AbstractRelationFactory[START, RELATION, END]*)(val nodeFactory: UuidNodeMatchesFactory[START])(implicit val format: Format[START]) extends EndMultiRelationReadBase[START, END]
case class StartRelationDelete[START <: UuidNode, RELATION <: MatchableRelation[START, END], END <: UuidNode](factory: MatchableRelationFactory[START, RELATION, END], nodeFactory: UuidNodeMatchesFactory[END]) extends StartRelationDeleteBase[START, RELATION, END]
case class EndRelationDelete[START <: UuidNode, RELATION <: MatchableRelation[START, END], END <: UuidNode](factory: MatchableRelationFactory[START, RELATION, END], nodeFactory: UuidNodeMatchesFactory[START]) extends EndRelationDeleteBase[START, RELATION, END]
case class StartRelationReadDelete[START <: UuidNode, RELATION <: MatchableRelation[START, END], END <: UuidNode](factory: MatchableRelationFactory[START, RELATION, END], nodeFactory: UuidNodeMatchesFactory[END])(implicit val format: Format[END]) extends StartRelationDeleteBase[START, RELATION, END] with StartRelationReadBase[START, RELATION, END]
case class EndRelationReadDelete[START <: UuidNode, RELATION <: MatchableRelation[START, END], END <: UuidNode](factory: MatchableRelationFactory[START, RELATION, END], nodeFactory: UuidNodeMatchesFactory[START])(implicit val format: Format[START]) extends EndRelationDeleteBase[START, RELATION, END] with EndRelationReadBase[START, RELATION, END]
case class StartRelationNothing[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](factory: AbstractRelationFactory[START, RELATION, END], nodeFactory: UuidNodeMatchesFactory[END]) extends StartRelationAccessDefault[START, RELATION, END]
case class EndRelationNothing[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](factory: AbstractRelationFactory[START, RELATION, END], nodeFactory: UuidNodeMatchesFactory[START]) extends EndRelationAccessDefault[START, RELATION, END]
case class StartRelationWrite[START <: UuidNode, RELATION <: ConstructRelation[START, END], END <: UuidNode](factory: ConstructRelationFactory[START, RELATION, END], nodeFactory: UuidNodeMatchesFactory[END])(implicit val format: Format[END]) extends StartRelationWriteBase[START, RELATION, END] with StartRelationDeleteBase[START, RELATION, END] with StartRelationReadBase[START, RELATION, END]
case class EndRelationWrite[START <: UuidNode, RELATION <: ConstructRelation[START, END], END <: UuidNode](factory: ConstructRelationFactory[START, RELATION, END], nodeFactory: UuidNodeMatchesFactory[START])(implicit val format: Format[START]) extends EndRelationWriteBase[START, RELATION, END] with EndRelationDeleteBase[START, RELATION, END] with EndRelationReadBase[START, RELATION, END]

trait RelationAccessDecorator[NODE <: UuidNode, OTHER <: UuidNode] extends RelationAccess[NODE, OTHER] with AccessDecoratorControlDefault {
  val self: RelationAccess[NODE, OTHER]

  override def read(context: RequestContext, param: ConnectParameter[NODE]) = {
    acceptRequestRead(context, Some(param.baseUuid)).getOrElse(self.read(context, param))
  }
  override def read[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S, E], E]) = {
    acceptRequestRead(context, Some(param.baseUuid)).getOrElse(self.read(context, param))
  }
  override def delete(context: RequestContext, param: ConnectParameter[NODE], uuid: String) = {
    acceptRequestWrite(context, Some(param.baseUuid)).getOrElse(self.delete(context, param, uuid))
  }
  override def delete[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S, E], E], uuid: String) = {
    acceptRequestWrite(context, Some(param.baseUuid)).getOrElse(self.delete(context, param, uuid))
  }
  override def create(context: RequestContext, param: ConnectParameter[NODE]) = {
    acceptRequestWrite(context, Some(param.baseUuid)).getOrElse(self.create(context, param))
  }
  override def create(context: RequestContext, param: ConnectParameter[NODE], uuid: String) = {
    acceptRequestWrite(context, Some(param.baseUuid)).getOrElse(self.create(context, param, uuid))
  }
  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S, E], E]) = {
    acceptRequestWrite(context, Some(param.baseUuid)).getOrElse(self.create(context, param))
  }
  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S, E], E], uuid: String) = {
    acceptRequestWrite(context, Some(param.baseUuid)).getOrElse(self.create(context, param, uuid))
  }

  val nodeFactory = self.nodeFactory
}

trait RelatedAccess extends RelationAccessDefault[UuidNode, UuidNode] {
  override val nodeFactory = UuidNodeMatches
}

case class StartRelationAccessDecoration[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](self: StartRelationAccess[START, RELATION, END], control: AccessDecoratorControl) extends RelationAccessDecorator[START, END] with StartRelationAccess[START, RELATION, END] with AccessDecoratorControlForward
case class EndRelationAccessDecoration[START <: UuidNode, RELATION <: AbstractRelation[START, END], END <: UuidNode](self: EndRelationAccess[START, RELATION, END], control: AccessDecoratorControl) extends RelationAccessDecorator[END, START] with EndRelationAccess[START, RELATION, END] with AccessDecoratorControlForward
