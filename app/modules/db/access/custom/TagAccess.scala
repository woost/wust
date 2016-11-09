package modules.db.access.custom

import controllers.api.nodes.RequestContext
import formatters.json.RequestFormat._
import formatters.json.TagFormat
import wust.Shared.tagTitleColor
import model.WustSchema.{Created => SchemaCreated, _}
import modules.db.Database.db
import modules.db._
import modules.db.access.NodeReadBase
import modules.requests._
import play.api.libs.json._
import play.api.mvc.Results._
import renesca.Query
import renesca.parameter.implicits._

case class TagAccess() extends NodeReadBase[Scope] {
  val factory = Scope
  implicit val format = TagFormat.ScopeFormat

  //TODO: should override read for multiple tags, too. so it includes inherits
  override def read(context: RequestContext, uuid: String) = {
    implicit val ctx = new QueryContext
    val node = FactoryUuidNodeDef(factory, uuid)
    val base = FactoryNodeDef(factory)
    val impl = FactoryNodeDef(factory)
    val baseInherit = RelationDef(base, Inherits, node)
    val implInherit = RelationDef(node, Inherits, impl)

    val query = s"""
    match ${node.toPattern}
    optional match ${baseInherit.toPattern(true, false)}
    optional match ${implInherit.toPattern(false, true)}
    return *
    """

    val discourse = Discourse(db.queryGraph(query, ctx.params))
    discourse.scopes.find(_.uuid == uuid).map(s => Ok(Json.toJson(s))).getOrElse(NotFound(s"Cannot find node with uuid '$uuid'"))
  }

  override def create(context: RequestContext) = context.withUser {
    context.withJson { (request: TagAddRequest) =>
      //TODO: should accept description, too.
      //currently only handled by update.
      //TODO: what about scopes here? we are only merging tags not taglike, but
      //it is not possible to merge on taglike, as they are traits
      val node = Scope.merge(
        title = request.title,
        color = tagTitleColor(request.title),
        merge = Set("title")
      )

      db.transaction(_.persistChanges(node)) match {
        case Some(err) => BadRequest(s"Cannot create Tag: $err'")
        case _ => Ok(Json.toJson(node))
      }
    }
  }

  override def update(context: RequestContext, uuid: String) = context.withUser {
    context.withJson { (request: TagUpdateRequest) =>
      val node = Scope.matchesOnUuid(uuid)
      //TODO: normally we would want to set it back to None instead of ""
      if (request.description.isDefined) {
        node.description = request.description
      }

      db.transaction(_.persistChanges(node)) match {
        case Some(err) => BadRequest(s"Cannot update Tag with uuid '$uuid': $err'")
        case _ => Ok(Json.toJson(node))
      }
    }
  }
}
