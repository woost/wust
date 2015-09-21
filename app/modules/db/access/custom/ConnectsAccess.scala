package modules.db.access.custom

import controllers.api.nodes.{ConnectParameter, HyperConnectParameter, RequestContext}
import formatters.json.ResponseFormat._
import model.WustSchema._
import modules.db.Database.db
import modules.db.access._
import modules.requests.ConnectResponse
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._
import renesca.schema._

trait ConnectsRelationHelper {
  protected def persistRelation[T <: Connectable](discourse: Discourse, result: T): Result = {
    db.transaction(_.persistChanges(discourse)).map(err =>
      BadRequest(s"Cannot connect: $err'")
    ).getOrElse({
      //TODO: Only send partial graph: the created relations!
      //TODO: should only send node if the node was newly created
      Ok(Json.toJson(ConnectResponse[T](discourse, Some(result))))
    })
  }
}

case class StartConnectsAccess() extends StartRelationReadBase[Post, Connects, Connectable] with StartRelationDeleteBase[Post, Connects, Connectable] with ConnectsRelationHelper {
  val factory = Connects
  val nodeFactory = Connectable

  val postaccess = PostAccess.apply

  override def create(context: RequestContext, param: ConnectParameter[Post]) = context.withUser {
    postaccess.createNode(context).map(n => createRelation(context, param, n)).getOrElse(BadRequest("Cannot create post"))
  }

  private def createRelation(context: RequestContext, param: ConnectParameter[Post], node: Connectable) = {
    val discourse = Discourse(node.graph)
    val base = param.baseFactory.matchesOnUuid(param.baseUuid)
    discourse.add(base, node, factory.merge(base, node))
    persistRelation(discourse, node)
  }

  override def create(context: RequestContext, param: ConnectParameter[Post], otherUuid: String) = context.withUser {
    createRelation(context, param, nodeFactory.matchesOnUuid(otherUuid))
  }
}

case class EndConnectsAccess() extends EndRelationReadBase[Post, Connects, Connectable] with EndRelationDeleteBase[Post, Connects, Connectable] with ConnectsRelationHelper {
  val factory = Connects
  val nodeFactory = Post

  val postaccess = PostAccess.apply

  override def create(context: RequestContext, param: ConnectParameter[Connectable]) = context.withUser {
    postaccess.createNode(context).map(n => createRelation(context, param, n)).getOrElse(BadRequest("Cannot create post"))
  }

  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, Connectable with AbstractRelation[S, E], E]) = context.withUser {
    postaccess.createNode(context).map(n => createRelation(context, param, n)).getOrElse(BadRequest("Cannot create post"))
  }

  private def createRelation(context: RequestContext, param: ConnectParameter[Connectable], node: Post) = {
    val discourse = Discourse(node.graph)
    val base = param.baseFactory.matchesOnUuid(param.baseUuid)
    discourse.add(base, node, factory.merge(node, base))
    persistRelation(discourse, node)
  }

  override def create(context: RequestContext, param: ConnectParameter[Connectable], otherUuid: String) = context.withUser {
    createRelation(context, param, nodeFactory.matchesOnUuid(otherUuid))
  }

  private def createRelation[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, Connectable with AbstractRelation[S, E], E], node: Post) = {
    val discourse = Discourse(node.graph)
    val start = param.startFactory.matchesOnUuid(param.startUuid)
    val end = param.endFactory.matchesOnUuid(param.endUuid)
    val base = param.baseFactory.matchesHyperConnection(start, end)
    val relation = factory.merge(node, base)
    discourse.add(base, node, relation)
    persistRelation(discourse, node)
  }

  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, Connectable with AbstractRelation[S, E], E], uuid: String) = context.withUser {
    createRelation(context, param, nodeFactory.matchesOnUuid(uuid))
  }
}