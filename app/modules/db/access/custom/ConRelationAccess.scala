package modules.db.access.custom

import controllers.api.nodes.{ConnectParameter, HyperConnectParameter, RequestContext}
import formatters.json.ApiNodeFormat._
import model.WustSchema._
import modules.db.Database.db
import modules.db.access._
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._
import renesca.schema._

trait ConstructRelationHelper {
  protected def persistRelation[T <: UuidNode](discourse: Discourse, result: T): Result = {
    db.transaction(_.persistChanges(discourse)).map(err =>
      BadRequest(s"Cannot create ConstructRelation: $err'")
    ).getOrElse({
      Ok(Json.toJson(result))
    })
  }
}

case class StartConRelationAccess[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](
  factory: ConstructRelationFactory[START, RELATION, END],
  nodeFactory: UuidNodeMatchesFactory[END]) extends StartRelationReadBase[START, RELATION, END] with StartRelationDeleteBase[START, RELATION, END] with ConstructRelationHelper {

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
    val base = param.baseFactory.matchesHyperConnection(start, end)
    val node = nodeFactory.matchesOnUuid(uuid)
    val relation = factory.mergeConstructRelation(base, node)
    discourse.add(base, relation)
    persistRelation(discourse, node)
  }
}

case class EndConRelationAccess[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](
  factory: ConstructRelationFactory[START, RELATION, END],
  nodeFactory: UuidNodeMatchesFactory[START]) extends EndRelationReadBase[START, RELATION, END] with EndRelationDeleteBase[START, RELATION, END] with ConstructRelationHelper {

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
    val base = param.baseFactory.matchesHyperConnection(start, end)
    val node = nodeFactory.matchesOnUuid(uuid)
    val relation = factory.mergeConstructRelation(node, base)
    discourse.add(base, relation)
    persistRelation(discourse, node)
  }
}
