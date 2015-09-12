package modules.db.access.custom

import controllers.api.nodes.{HyperConnectParameter, ConnectParameter, RequestContext}
import formatters.json.RequestFormat._
import model.WustSchema._
import modules.db.Database.db
import modules.db.HyperNodeDefinitionBase
import modules.db.access._
import modules.requests.{ConnectResponse}
import play.api.libs.json.JsValue
import play.api.mvc.Result
import play.api.mvc.Results._
import renesca.parameter.implicits._
import renesca.schema._

trait ConstructRelationHelper {
  protected def persistRelation[T <: UuidNode](discourse: Discourse, result: T): Either[Result, ConnectResponse[T]] = {
    db.transaction(_.persistChanges(discourse)).map(err =>
      Left(BadRequest(s"Cannot create ConstructRelation: $err'"))
    ).getOrElse({
      //TODO: Only send partial graph: the created relations!
      //TODO: should only send node if the node was newly created
      Right(ConnectResponse[T](discourse, Some(result)))
    })
  }

  protected def fail(uuid: String) = Right(s"Cannot connect Nodes with uuid '$uuid'")
}

//TODO: track what the user did here
//TODO: all relationaccess should accept this type signature, so we can have
//relations between traits and still use subclass node as start/end node
case class StartConRelationAccess[
RSTART <: UuidNode,
START <: RSTART,
RELATION <: AbstractRelation[RSTART, REND],
REND <: UuidNode,
END <: REND
](
  factory: ConstructRelationFactory[RSTART, RELATION, REND],
  nodeFactory: UuidNodeMatchesFactory[END]) extends StartRelationReadBase[RSTART,RELATION,REND] with StartRelationDeleteBase[RSTART,RELATION,REND] with ConstructRelationHelper {

  override def create(context: RequestContext, param: ConnectParameter[RSTART], otherUuid: String) = context.withUser {
      val discourse = Discourse.empty
      val base = param.baseFactory.matchesOnUuid(param.baseUuid)
      val node = nodeFactory.matchesOnUuid(otherUuid)
      discourse.add(factory.mergeConstructRelation(base, node))
      persistRelation(discourse, node)
  }

  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,RSTART with AbstractRelation[S,E], E], uuid: String) = context.withUser {
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
RSTART <: UuidNode,
START <: RSTART,
RELATION <: AbstractRelation[RSTART, REND],
REND <: UuidNode,
END <: REND
](
  factory: ConstructRelationFactory[RSTART, RELATION, REND],
  nodeFactory: UuidNodeMatchesFactory[START]) extends EndRelationReadBase[RSTART,RELATION,REND] with EndRelationDeleteBase[RSTART,RELATION,REND] with ConstructRelationHelper {

  override def create(context: RequestContext, param: ConnectParameter[REND], otherUuid: String) = context.withUser {
    val discourse = Discourse.empty
    val base = param.baseFactory.matchesOnUuid(param.baseUuid)
    val node = nodeFactory.matchesOnUuid(otherUuid)
    discourse.add(factory.mergeConstructRelation(node, base))
    persistRelation(discourse, node)
  }

  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,REND with AbstractRelation[S,E], E], uuid: String) = context.withUser {
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
