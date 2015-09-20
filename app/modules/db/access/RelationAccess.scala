package modules.db.access

import controllers.api.nodes.{HyperConnectParameter, ConnectParameter, RequestContext}
import model.WustSchema._
import modules.db.Database._
import modules.db._
import modules.db.types._
import modules.requests.ConnectResponse
import play.api.libs.json._
import renesca.schema._
import renesca.graph.Graph
import play.api.mvc.Results._
import play.api.mvc.Result
import formatters.json.ApiNodeFormat._

trait RelationAccess[-NODE <: UuidNode, +OTHER <: UuidNode] {
  def read(context: RequestContext, param: ConnectParameter[NODE]): Result
  def read[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,NODE with AbstractRelation[S,E], E]): Result
  def delete(context: RequestContext, param: ConnectParameter[NODE], uuid: String): Result
  def delete[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,NODE with AbstractRelation[S,E],E], uuid: String): Result
  def create(context: RequestContext, param: ConnectParameter[NODE]): Result
  def create(context: RequestContext, param: ConnectParameter[NODE], uuid: String): Result
  def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S,E], E]): Result
  def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S,E], E], uuid: String): Result

  val nodeFactory: UuidNodeMatchesFactory[OTHER]
}

trait RelationAccessDefault[NODE <: UuidNode, OTHER <: UuidNode] extends RelationAccess[NODE, OTHER]{
  def read(context: RequestContext, param: ConnectParameter[NODE]) = NotFound("No read access on Relation")
  def read[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,NODE with AbstractRelation[S,E], E]) = NotFound("No read access on HyperRelation")
  def delete(context: RequestContext, param: ConnectParameter[NODE], uuid: String) = NotFound("No delete access on Relation")
  def delete[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,NODE with AbstractRelation[S,E],E], uuid: String) = NotFound("No delete access on HyperRelation")
  def create(context: RequestContext, param: ConnectParameter[NODE]) = NotFound("No json create access on Relation")
  def create(context: RequestContext, param: ConnectParameter[NODE], uuid: String) = NotFound("No create access on Relation")
  def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S,E], E]) = NotFound("No json create access on HyperRelation")
  def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S,E], E], uuid: String) = NotFound("No create access on HyperRelation")
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
      Ok(Json.toJson(limitedStartConnectedDiscourseNodes(skip, context.limit, relDefs: _*)))
    }.getOrElse(Ok(Json.toJson((startConnectedDiscourseNodes(relDefs: _*)))))
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
      Ok(Json.toJson(limitedEndConnectedDiscourseNodes(skip, context.limit, relDefs: _*)))
    }.getOrElse(Ok(Json.toJson(endConnectedDiscourseNodes(relDefs: _*))))
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

trait StartRelationDeleteBase[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends StartRelationAccessDefault[START,RELATION,END] {
  val factory: AbstractRelationFactory[START,RELATION,END]

  override def delete(context: RequestContext, param: ConnectParameter[START], uuid: String) = context.withUser {
    val relationDefinition = RelationDefinition(toBaseNodeDefinition(param), factory, toNodeDefinition(uuid))
    if (disconnectNodes(relationDefinition))
      BadRequest("Cannot delete relation")
    else
      NoContent
  }

  override def delete[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,START with AbstractRelation[S,E],E], uuid: String) = context.withUser {
    val relationDefinition = RelationDefinition(toBaseNodeDefinition(param), factory, toNodeDefinition(uuid))
    if (disconnectNodes(relationDefinition))
      BadRequest("Cannot delete relation")
    else
      NoContent
  }
}

trait EndRelationDeleteBase[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends EndRelationAccessDefault[START,RELATION,END] {
  val factory: AbstractRelationFactory[START,RELATION,END]

  override def delete(context: RequestContext, param: ConnectParameter[END], uuid: String) = context.withUser {
    val relationDefinition = RelationDefinition(toNodeDefinition(uuid), factory, toBaseNodeDefinition(param))
    if (disconnectNodes(relationDefinition))
      BadRequest("Cannot delete relation")
    else
      NoContent
  }

  override def delete[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,END with AbstractRelation[S,E],E], uuid: String) = context.withUser {
    val relationDefinition = RelationDefinition(toNodeDefinition(uuid), factory, toHyperBaseNodeDefinition(param))
    if (disconnectNodes(relationDefinition))
      BadRequest("Cannot delete relation")
    else
      NoContent
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

trait RelationAccessDecorator[NODE <: UuidNode, OTHER <: UuidNode] extends RelationAccess[NODE, OTHER] with AccessDecoratorControlDefault {
  val self: RelationAccess[NODE, OTHER]

  override def read(context: RequestContext, param: ConnectParameter[NODE]) = {
    acceptRequestRead(context).getOrElse(self.read(context, param))
  }
  override def read[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,NODE with AbstractRelation[S,E], E]) = {
    acceptRequestRead(context).getOrElse(self.read(context, param))
  }
  override def delete(context: RequestContext, param: ConnectParameter[NODE], uuid: String) = {
    acceptRequestWrite(context).getOrElse(self.delete(context, param, uuid))
  }
  override def delete[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,NODE with AbstractRelation[S,E],E], uuid: String) = {
    acceptRequestWrite(context).getOrElse(self.delete(context, param, uuid))
  }
  override def create(context: RequestContext, param: ConnectParameter[NODE]) = {
    acceptRequestWrite(context).getOrElse(self.create(context, param))
  }
  override def create(context: RequestContext, param: ConnectParameter[NODE], uuid: String) = {
    acceptRequestWrite(context).getOrElse(self.create(context, param, uuid))
  }
  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S,E], E]) = {
    acceptRequestWrite(context).getOrElse(self.create(context, param))
  }
  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, NODE with AbstractRelation[S,E], E], uuid: String) = {
    acceptRequestWrite(context).getOrElse(self.create(context, param, uuid))
  }

  val nodeFactory = self.nodeFactory
}

trait RelatedAccess extends RelationAccessDefault[UuidNode, UuidNode] {
  override val nodeFactory = UuidNodeMatches
}

case class StartRelationAccessDecoration[START <: UuidNode, RELATION <: AbstractRelation[START,END], END <: UuidNode](self: StartRelationAccess[START,RELATION,END], control: AccessDecoratorControl) extends RelationAccessDecorator[START,END] with StartRelationAccess[START,RELATION,END] with AccessDecoratorControlForward
case class EndRelationAccessDecoration[START <: UuidNode, RELATION <: AbstractRelation[START,END], END <: UuidNode](self: EndRelationAccess[START,RELATION,END], control: AccessDecoratorControl) extends RelationAccessDecorator[END,START] with EndRelationAccess[START,RELATION,END] with AccessDecoratorControlForward
