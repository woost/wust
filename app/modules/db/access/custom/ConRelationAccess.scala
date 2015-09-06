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
case class StartConRelationAccess[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](
  factory: ConstructRelationFactory[START, RELATION, END],
  nodeFactory: UuidNodeMatchesFactory[END]) extends StartRelationReadBase[START,RELATION,END] with StartRelationDeleteBase[START,RELATION,END] with ConstructRelationHelper {

  override def create(context: RequestContext, param: ConnectParameter[START], otherUuid: String) = {
      val discourse = Discourse.empty
      val base = param.baseFactory.matchesOnUuid(param.baseUuid)
      val node = nodeFactory.matchesOnUuid(otherUuid)
      discourse.add(factory.mergeConstructRelation(base, node))
      persistRelation(discourse, node)
  }

  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,START with AbstractRelation[S,E], E], uuid: String) = {
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
  nodeFactory: UuidNodeMatchesFactory[START]) extends EndRelationReadBase[START,RELATION,END] with EndRelationDeleteBase[START,RELATION,END] with ConstructRelationHelper {

  override def create(context: RequestContext, param: ConnectParameter[END], otherUuid: String) = {
    val discourse = Discourse.empty
    val base = param.baseFactory.matchesOnUuid(param.baseUuid)
    val node = nodeFactory.matchesOnUuid(otherUuid)
    discourse.add(factory.mergeConstructRelation(node, base))
    persistRelation(discourse, node)
  }

  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,END with AbstractRelation[S,E], E], uuid: String) = {
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
