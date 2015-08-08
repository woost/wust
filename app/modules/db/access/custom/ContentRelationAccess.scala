package modules.db.access.custom

import controllers.api.nodes.{HyperConnectParameter, ConnectParameter, RequestContext}
import formatters.json.RequestFormat._
import model.WustSchema._
import modules.db.Database.db
import modules.db.HyperNodeDefinitionBase
import modules.db.access._
import modules.requests.{ConnectResponse, PostAddRequest}
import play.api.libs.json.JsValue
import renesca.parameter.implicits._
import renesca.schema._

trait ContentRelationHelper {
  protected def persistRelation[T <: UuidNode](discourse: Discourse, result: T): Either[String, ConnectResponse[T]] = {
    db.transaction(_.persistChanges(discourse)).map(err =>
      Left(s"Cannot create ContentRelation: $err'")
    ).getOrElse({
      //TODO: Only send partial graph: the created relations!
      //TODO: should only send node if the node was newly created
      Right(ConnectResponse[T](discourse, Some(result)))
    })
  }

  protected def fail(uuid: String) = Right(s"Cannot connect Nodes with uuid '$uuid'")
}

//TODO: track what the user did here
case class StartContentRelationAccess[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](
  factory: ContentRelationFactory[START, RELATION, END],
  nodeFactory: UuidNodeMatchesFactory[END]) extends StartRelationReadBase[START,RELATION,END] with StartRelationDeleteBase[START,RELATION,END] with ContentRelationHelper {

  override def create(context: RequestContext, param: ConnectParameter[START], otherUuid: String) = {
      val discourse = Discourse.empty
      val base = param.baseFactory.matchesOnUuid(param.baseUuid)
      val node = nodeFactory.matchesOnUuid(otherUuid)
      discourse.add(factory.mergeContentRelation(base, node))
      persistRelation(discourse, node)
  }

  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,START with AbstractRelation[S,E], E], uuid: String) = {
      val discourse = Discourse.empty
      val start = param.startFactory.matchesOnUuid(param.startUuid)
      val end = param.endFactory.matchesOnUuid(param.endUuid)
      val base = param.baseFactory.matchesHyperConnection(start, end)
      val node = nodeFactory.matchesOnUuid(uuid)
      val relation = factory.mergeContentRelation(base, node)
      discourse.add(base, relation)
      persistRelation(discourse, node)
  }
}

case class EndContentRelationAccess[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](
  factory: ContentRelationFactory[START, RELATION, END],
  nodeFactory: UuidNodeMatchesFactory[START]) extends EndRelationReadBase[START,RELATION,END] with EndRelationDeleteBase[START,RELATION,END] with ContentRelationHelper {

  override def create(context: RequestContext, param: ConnectParameter[END], otherUuid: String) = {
    val discourse = Discourse.empty
    val base = param.baseFactory.matchesOnUuid(param.baseUuid)
    val node = nodeFactory.matchesOnUuid(otherUuid)
    discourse.add(factory.mergeContentRelation(node, base))
    persistRelation(discourse, node)
  }

  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S,END with AbstractRelation[S,E], E], uuid: String) = {
      val discourse = Discourse.empty
      val start = param.startFactory.matchesOnUuid(param.startUuid)
      val end = param.endFactory.matchesOnUuid(param.endUuid)
      val base = param.baseFactory.matchesHyperConnection(start, end)
      val node = nodeFactory.matchesOnUuid(uuid)
      val relation = factory.mergeContentRelation(node, base)
      discourse.add(base, relation)
      persistRelation(discourse, node)
  }
}
