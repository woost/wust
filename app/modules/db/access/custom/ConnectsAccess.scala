package modules.db.access.custom

import controllers.api.nodes.{ConnectParameter, HyperConnectParameter, RequestContext}
import controllers.live.LiveWebSocket
import formatters.json.ResponseFormat._
import formatters.json.RequestFormat._
import renesca.parameter.implicits._
import formatters.json.GraphFormat
import modules.requests._
import model.WustSchema.{Created => SchemaCreated, _}
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
import moderation.Moderation

trait ConnectsHelper {
  protected def canEditConnects(connects: Connects, scopes: Seq[Scope]) = {
    val startNode = connects.startNodeOpt.get
    val authorBoost = if (startNode.rev_createds.isEmpty) 0 else Moderation.authorKarmaBoost
    val voteWeight = Moderation.voteWeightFromScopes(scopes)
    val applyThreshold = Moderation.postChangeThreshold(startNode.viewCount)
    Moderation.canApply(voteWeight + authorBoost, applyThreshold)
  }

  protected def getConnectsWithKarma(user: User, uuid: String) = {
    implicit val ctx = new QueryContext
    val userDef = ConcreteNodeDef(user)
    val postDef = FactoryNodeDef(Post)
    val createdDef = RelationDef(userDef, SchemaCreated, postDef)
    val connectsDef = HyperNodeDef(postDef, Connects, FactoryNodeDef(Connectable), Some(uuid))
    val query = s"""
    match ${connectsDef.toPattern},
    (${connectsDef.startName})-[:`${Connects.startRelationType}`|`${Connects.endRelationType}` *0..20]->(connectable: `${Connectable.label}`)
    with distinct connectable, ${connectsDef.startName}, ${connectsDef.startRelationName}, ${connectsDef.name}, ${connectsDef.endRelationName}, ${connectsDef.endName}
    match ${userDef.toPattern}
    optional match ${createdDef.toPattern(false, false)}
    optional match (tag: `${Scope.label}`)-[:`${Tags.startRelationType}`]->(:`${Tags.label}`)-[:`${Tags.endRelationType}`]->(connectable: `${Post.label}`)
    optional match (${userDef.name})-[hasKarma:`${HasKarma.relationType}`]->(tag)
    return ${connectsDef.startName}, ${connectsDef.startRelationName}, ${connectsDef.name}, ${connectsDef.endRelationName}, ${connectsDef.endName}, ${userDef.name}, ${createdDef.startRelationName}, ${createdDef.name}, ${createdDef.endRelationName}, tag, hasKarma
    """

    val discourse = Discourse(db.queryGraph(query, ctx.params))
    (discourse, discourse.connects.find(_.uuid == uuid).get)
  }
}

trait ConnectsRelationHelper[NODE <: Connectable] extends ConnectsHelper {
  implicit def format: Format[NODE]

  protected def persistRelation(discourse: Discourse, result: NODE, base: Connectable): Result = {
    db.transaction(_.persistChanges(discourse)).map(err =>
      BadRequest(s"Cannot connect: $err'")
    ) getOrElse {
      //TODO: Only send partial graph: the created relations!
      //TODO: should only send node if the node was newly created
      val connResponse = ConnectResponse[NODE](discourse, Some(result))
      LiveWebSocket.sendConnectsAdd(base.uuid, connResponse)
      Ok(Json.toJson(connResponse))
    }
  }

  protected def deleteRelation(relation: Connects): Result = {
    val discourse = Discourse(relation)
    db.transaction { tx =>
      tx.persistChanges(discourse).map(_ => NoContent) orElse {
        discourse.remove(relation)
        tx.persistChanges(discourse)
      }.map(err => BadRequest(s"Cannot disconnect: $err"))
    } getOrElse {
      LiveWebSocket.sendConnectableDelete(relation.uuid)
      NoContent
    }
  }

  protected def createPost(context: RequestContext): Either[Result, Discourse] = context.user.map { user =>
    import formatters.json.EditNodeFormat._

    context.jsonAs[PostAddRequest].map { request =>
      Right(PostHelper.createPost(request, user))
    }.getOrElse(Left(BadRequest("Cannot parse create request")))
  }.getOrElse(Left(context.onlyUsersError))
}

case class StartConnectsAccess() extends StartRelationReadBase[Post, Connects, Connectable] with StartRelationDeleteBase[Post, Connects, Connectable] with ConnectsRelationHelper[Connectable] {
  val factory = Connects
  val nodeFactory = Connectable
  implicit val format = GraphFormat.ConnectableFormat

  private def createRelation(context: RequestContext, param: ConnectParameter[Post], discourse: Discourse) = {
    //TODO: magic accessor for traits should include matches nodes
    val node = discourse.connectables.headOption.getOrElse(discourse.nodesAs(ConnectableMatches).head)
    val base = param.factory.matchesOnUuid(param.baseUuid)
    discourse.add(base, node, factory.merge(base, node))
    persistRelation(discourse, node, base)
  }

  override def create(context: RequestContext, param: ConnectParameter[Post]) = createPost(context) match {
    case Left(err) => err
    case Right(discourse) => createRelation(context, param, discourse)
  }

  override def create(context: RequestContext, param: ConnectParameter[Post], otherUuid: String) = context.withUser {
    createRelation(context, param, Discourse(nodeFactory.matchesOnUuid(otherUuid)))
  }

  override def delete(context: RequestContext, param: ConnectParameter[Post], otherUuid: String) = context.withUser {
    val base = param.factory.matchesOnUuid(param.baseUuid)
    val node = nodeFactory.matchesOnUuid(otherUuid)
    val relation = factory.matches(base, node)
    val discourse = Discourse(relation)
    deleteRelation(relation)
  }
}

case class EndConnectsAccess() extends EndRelationReadBase[Post, Connects, Connectable] with EndRelationDeleteBase[Post, Connects, Connectable] with ConnectsRelationHelper[Post] {
  val factory = Connects
  val nodeFactory = Post
  implicit val format = GraphFormat.PostFormat

  val postaccess = PostAccess.apply

  private def createRelation(context: RequestContext, param: ConnectParameter[Connectable], discourse: Discourse) = {
    val node = discourse.posts.head
    val base = param.factory.matchesOnUuid(param.baseUuid)
    discourse.add(base, node, factory.merge(node, base))
    persistRelation(discourse, node, base)
  }

  private def createRelation[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, Connectable with AbstractRelation[S, E], E], discourse: Discourse) = {
    val start = param.startFactory.matchesOnUuid(param.startUuid)
    val end = param.endFactory.matchesOnUuid(param.endUuid)
    val node = discourse.posts.head
    val base = param.factory.matchesMatchableRelation(start, end)
    val relation = factory.merge(node, base)
    discourse.add(base, node, relation)
    persistRelation(discourse, node, base)
  }

  override def create(context: RequestContext, param: ConnectParameter[Connectable]) = createPost(context) match {
    case Left(err) => err
    case Right(discourse) => createRelation(context, param, discourse)
  }

  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, Connectable with AbstractRelation[S, E], E]) = createPost(context) match {
    case Left(err) => err
    case Right(discourse) => createRelation(context, param, discourse)
  }

  override def create(context: RequestContext, param: ConnectParameter[Connectable], otherUuid: String) = context.withUser {
    createRelation(context, param, Discourse(nodeFactory.matchesOnUuid(otherUuid)))
  }

  override def create[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, Connectable with AbstractRelation[S, E], E], uuid: String) = context.withUser {
    createRelation(context, param, Discourse(nodeFactory.matchesOnUuid(uuid)))
  }

  override def delete(context: RequestContext, param: ConnectParameter[Connectable], otherUuid: String) = context.withUser {
    val base = param.factory.matchesOnUuid(param.baseUuid)
    val node = nodeFactory.matchesOnUuid(otherUuid)
    val relation = factory.matches(node, base)
    deleteRelation(relation)
  }

  override def delete[S <: UuidNode, E <: UuidNode](context: RequestContext, param: HyperConnectParameter[S, Connectable with AbstractRelation[S, E], E], uuid: String) = context.withUser {
    val start = param.startFactory.matchesOnUuid(param.startUuid)
    val end = param.endFactory.matchesOnUuid(param.endUuid)
    val base = param.factory.matchesMatchableRelation(start, end)
    val node = nodeFactory.matchesOnUuid(uuid)
    val relation = factory.matches(node, base)
    deleteRelation(relation)
  }
}

case class ConnectsAccess() extends NodeAccessDefault[Connects] with ConnectsHelper {
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
      val (discourse, connects) = getConnectsWithKarma(user, uuid)

      if (canEditConnects(connects, discourse.scopes)) {
        deleteClassificationsFromGraph(discourse, request, connects)
        addClassifcationsToGraph(discourse, request, connects)

        db.transaction(_.persistChanges(discourse)) match {
          case Some(err) => BadRequest(s"Cannot update Connects with uuid '$uuid': $err'")
          case _         =>
            val connectsWithClass = ClassifiedReferences.shapeResponse(connects)
            LiveWebSocket.sendConnectableUpdate(connectsWithClass)
            Ok(Json.toJson(connectsWithClass))
        }
      } else {
        Unauthorized("Not enough karma to edit relation")
      }
    }
  }
}
