package modules.db.access.custom

import controllers.api.nodes.{ConnectParameter, HyperConnectParameter, RequestContext}
import model.WustSchema._
import modules.db.Database.db
import modules.db.access._
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._
import renesca.schema._

trait ConstructRelationHelper[NODE <: UuidNode] {
  implicit def format: Format[NODE]

  protected def persistRelation(discourse: Discourse, result: NODE): Result = {
    db.transaction(_.persistChanges(discourse)).map(err =>
      BadRequest(s"Cannot create ConstructRelation: $err'")
    ).getOrElse({
      Ok(Json.toJson(result))
    })
  }
}

trait StartConRelationAccessBase[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends StartRelationReadBase[START, RELATION, END] with StartRelationDeleteBase[START, RELATION, END] with ConstructRelationHelper[END] {
  val factory: ConstructRelationFactory[START, RELATION, END]
  val nodeFactory: UuidNodeMatchesFactory[END]

  override def create(context: RequestContext, param: ConnectParameter[START], otherUuid: String) = context.withUser {
    val discourse = Discourse.empty
    val base = param.baseFactory.matchesOnUuid(param.baseUuid)
    val node = nodeFactory.matchesOnUuid(otherUuid)
    discourse.add(factory.mergeConstructRelation(base, node))
    persistRelation(discourse, node)
  }

  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, START with AbstractRelation[S, E], E], uuid: String) = context.withUser {
    val discourse = Discourse.empty
    val start = param.startFactory.matchesOnUuid(param.startUuid)
    val end = param.endFactory.matchesOnUuid(param.endUuid)
    val base = param.baseFactory.matchesMatchableRelation(start, end)
    val node = nodeFactory.matchesOnUuid(uuid)
    val relation = factory.mergeConstructRelation(base, node)
    discourse.add(base, relation)
    persistRelation(discourse, node)
  }
}

trait EndConRelationAccessBase[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
] extends EndRelationReadBase[START, RELATION, END] with EndRelationDeleteBase[START, RELATION, END] with ConstructRelationHelper[START] {
  val factory: ConstructRelationFactory[START, RELATION, END]
  val nodeFactory: UuidNodeMatchesFactory[START]

  override def create(context: RequestContext, param: ConnectParameter[END], otherUuid: String) = context.withUser {
    val discourse = Discourse.empty
    val base = param.baseFactory.matchesOnUuid(param.baseUuid)
    val node = nodeFactory.matchesOnUuid(otherUuid)
    discourse.add(factory.mergeConstructRelation(node, base))
    persistRelation(discourse, node)
  }

  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, END with AbstractRelation[S, E], E], uuid: String) = context.withUser {
    val discourse = Discourse.empty
    val start = param.startFactory.matchesOnUuid(param.startUuid)
    val end = param.endFactory.matchesOnUuid(param.endUuid)
    val base = param.baseFactory.matchesMatchableRelation(start, end)
    val node = nodeFactory.matchesOnUuid(uuid)
    val relation = factory.mergeConstructRelation(node, base)
    discourse.add(base, relation)
    persistRelation(discourse, node)
  }
}

case class StartConRelationAccess[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](factory: ConstructRelationFactory[START, RELATION, END],
  nodeFactory: UuidNodeMatchesFactory[END])(implicit val format: Format[END]) extends StartConRelationAccessBase[START,RELATION,END]

case class EndConRelationAccess[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](factory: ConstructRelationFactory[START, RELATION, END],
  nodeFactory: UuidNodeMatchesFactory[START])(implicit val format: Format[START]) extends EndConRelationAccessBase[START,RELATION,END]
