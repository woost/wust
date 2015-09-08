package modules.db.access.custom

import controllers.api.nodes.RequestContext
import formatters.json.RequestFormat._
import model.WustSchema.{Created => SchemaCreated, _}
import modules.db.Database.db
import modules.db.access.NodeReadBase
import modules.db._;
import modules.requests._
import play.api.libs.json.JsValue
import renesca.parameter.implicits._
import renesca.Query
import play.api.mvc.Results._
import model.Helpers.tagTitleColor

class TagAccess extends NodeReadBase[TagLike] {
  val factory = TagLike

  //TODO: should override read for multiple tags, too. so it includes inherits
  override def read(context: RequestContext, uuid: String) = {
    println(context + uuid)
    val node = FactoryUuidNodeDefinition(factory, uuid)
    val base = ConcreteFactoryNodeDefinition(factory)
    val impl = ConcreteFactoryNodeDefinition(factory)
    val baseInherit = RelationDefinition(base, Inherits, node)
    val implInherit = RelationDefinition(node, Inherits, impl)
    val query = s"match ${node.toQuery} optional match ${baseInherit.toQuery(true, false)} optional match ${implInherit.toQuery(false, true)} return *"
    println(query)
    val discourse = Discourse(db.queryGraph(Query(query, baseInherit.parameterMap ++ implInherit.parameterMap)))
    println(discourse)
    discourse.tagLikes.find(_.uuid == uuid).map(Right(_)).getOrElse(Left(NotFound(s"Cannot find node with uuid '$uuid'")))
  }

  override def create(context: RequestContext) = {
    context.withJson { (request: TagAddRequest) =>
      //TODO: should accept description, too.
      //currently only handled by update.
      //TODO: what about scopes here? we are only merging tags not taglike, but
      //it is not possible to merge on taglike, as they are traits
      val node = Scope.merge(
        title = request.title,
        color = tagTitleColor(request.title),
        merge = Set("title"))
      // val contribution = SchemaCreated.create(context.user, node)

      db.transaction(_.persistChanges(node)) match {
        case Some(err) => Left(BadRequest(s"Cannot create Tag: $err'"))
        case _         => Right(node)
      }
    }
  }

  override def update(context: RequestContext, uuid: String) = {
    context.withJson { (request: TagUpdateRequest) =>
      val node = TagLike.matchesOnUuid(uuid)
      //TODO: normally we would want to set it back to None instead of ""
      if (request.description.isDefined) {
        node.description = request.description
      }

      // val contribution = Updated.create(context.user, node)

      db.transaction(_.persistChanges(node)) match {
      // db.transaction(_.persistChanges(contribution)) match {
        case Some(err) => Left(BadRequest(s"Cannot update Tag with uuid '$uuid': $err'"))
        case _         => Right(node)
      }
    }
  }
}

object TagAccess {
  def apply = new TagAccess
}

