package modules.db.access.custom

import controllers.api.nodes.{ConnectParameter, HyperConnectParameter, RequestContext}
import formatters.json.ResponseFormat._
import formatters.json.RequestFormat._
import renesca.parameter.implicits._
import formatters.json.GraphFormat
import modules.requests._
import model.WustSchema._
import modules.db.Database.db
import modules.db.access._
import modules.db.helpers.{PostHelper,ClassifiedReferences}
import modules.db._
import modules.requests.ConnectResponse
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._
import renesca.schema._
import renesca.Query
import wust.Shared.tagTitleColor


trait ConnectsRelationHelper[NODE <: Connectable] {
  implicit def format: Format[NODE]

  protected def persistRelation(discourse: Discourse, result: NODE): Result = {
    db.transaction(_.persistChanges(discourse)).map(err =>
      BadRequest(s"Cannot connect: $err'")
    ).getOrElse({
      //TODO: Only send partial graph: the created relations!
      //TODO: should only send node if the node was newly created
      Ok(Json.toJson(ConnectResponse[NODE](discourse, Some(result))))
    })
  }

  protected def createPost(context: RequestContext): Either[Result, Post] = context.user.map { user =>
    import formatters.json.EditNodeFormat._

    context.jsonAs[PostAddRequest].map { request =>
      PostHelper.createPost(request, user) match {
        case Left(err) => Left(BadRequest(s"Cannot create Post: $err"))
        case Right(node) => Right(node)
      }
    }.getOrElse(Left(BadRequest("Cannot parse create request")))
  }.getOrElse(Left(context.onlyUsersError))
}

case class StartConnectsAccess() extends StartRelationReadBase[Post, Connects, Connectable] with StartRelationDeleteBase[Post, Connects, Connectable] with ConnectsRelationHelper[Connectable] {
  val factory = Connects
  val nodeFactory = Connectable
  implicit val format = GraphFormat.ConnectableFormat

  private def createRelation(context: RequestContext, param: ConnectParameter[Post], node: Connectable) = {
    val discourse = Discourse(node.graph)
    val base = param.baseFactory.matchesOnUuid(param.baseUuid)
    discourse.add(base, node, factory.merge(base, node))
    persistRelation(discourse, node)
  }

  override def create(context: RequestContext, param: ConnectParameter[Post]) = createPost(context) match {
    case Left(err) => err
    case Right(node) => createRelation(context, param, node)
  }

  override def create(context: RequestContext, param: ConnectParameter[Post], otherUuid: String) = context.withUser {
    createRelation(context, param, nodeFactory.matchesOnUuid(otherUuid))
  }
}

case class EndConnectsAccess() extends EndRelationReadBase[Post, Connects, Connectable] with EndRelationDeleteBase[Post, Connects, Connectable] with ConnectsRelationHelper[Post] {
  val factory = Connects
  val nodeFactory = Post
  implicit val format = GraphFormat.PostFormat

  val postaccess = PostAccess.apply

  private def createRelation(context: RequestContext, param: ConnectParameter[Connectable], node: Post) = {
    val discourse = Discourse(node.graph)
    val base = param.baseFactory.matchesOnUuid(param.baseUuid)
    discourse.add(base, node, factory.merge(node, base))
    persistRelation(discourse, node)
  }

  private def createRelation[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, Connectable with AbstractRelation[S, E], E], node: Post) = {
    val discourse = Discourse(node.graph)
    val start = param.startFactory.matchesOnUuid(param.startUuid)
    val end = param.endFactory.matchesOnUuid(param.endUuid)
    val base = param.baseFactory.matchesMatchableRelation(start, end)
    val relation = factory.merge(node, base)
    discourse.add(base, node, relation)
    persistRelation(discourse, node)
  }

  override def create(context: RequestContext, param: ConnectParameter[Connectable]) = createPost(context) match {
    case Left(err) => err
    case Right(node) => createRelation(context, param, node)
  }

  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, Connectable with AbstractRelation[S, E], E]) = createPost(context) match {
    case Left(err) => err
    case Right(node) => createRelation(context, param, node)
  }

  override def create(context: RequestContext, param: ConnectParameter[Connectable], otherUuid: String) = context.withUser {
    createRelation(context, param, nodeFactory.matchesOnUuid(otherUuid))
  }


  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, Connectable with AbstractRelation[S, E], E], uuid: String) = context.withUser {
    createRelation(context, param, nodeFactory.matchesOnUuid(uuid))
  }
}

case class ConnectsAccess() extends NodeAccessDefault[Connects] {
  val factory = Connects

  private def deleteClassificationsFromGraph(discourse: Discourse, request: ConnectsUpdateRequest, node: Connects) {
    request.removedTags.foreach { remTag =>
      val tag = Classification.matchesOnUuid(remTag.id)
      discourse.add(tag)
      val tagging = Classifies.matches(tag, node)
      discourse.remove(tagging)
    }
  }

  private def addClassifcationsToGraph(discourse: Discourse, request: ConnectsUpdateRequest, node: Connects) {
    request.addedTags.map(c => Classification.matchesOnUuid(c.id)).foreach { tag =>
      val tags = Classifies.merge(tag, node)
      discourse.add(tags)
    }
  }

  override def update(context: RequestContext, uuid: String) = context.withUser { user =>
    import formatters.json.EditNodeFormat._

    context.withJson { (request: ConnectsUpdateRequest) =>
      db.transaction { tx =>
        val node = Connects.matchesOnUuid(uuid)
        val discourse = Discourse(node)
        deleteClassificationsFromGraph(discourse, request, node)
        addClassifcationsToGraph(discourse, request, node)

        tx.persistChanges(discourse) match {
          case Some(err) => BadRequest(s"Cannot update Connects with uuid '$uuid': $err'")
          case _         => Ok(Json.toJson(ClassifiedReferences.shapeResponse(Connects.wrap(node.rawItem))))
        }
      }
    }
  }
}
