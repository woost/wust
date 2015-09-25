package modules.db.access.custom

import controllers.api.nodes.RequestContext
import formatters.json.RequestFormat._
import play.api.libs.json._
import model.WustSchema.{Created => SchemaCreated, _}
import modules.db.Database._
import modules.db.access._
import modules.db._
import modules.requests._
import renesca.parameter.implicits._
import renesca.QueryHandler
import renesca.Query
import play.api.mvc.Results._
import model.Helpers.tagTitleColor
import moderation.Moderation

trait TagAccessHelper {
  protected def tagConnectRequestToClassification(tag: ClassificationConnectRequest) = {
    if (tag.id.isDefined)
      Some(Classification.matchesOnUuid(tag.id.get))
    else if (tag.title.isDefined)
      Some(Classification.merge(
        title = tag.title.get,
        color = tagTitleColor(tag.title.get),
        merge = Set("title")))
    else
      None
  }
}

case class ReferenceAccess() extends NodeAccessDefault[Reference] with TagAccessHelper {
  val factory = Reference

  private def deleteClassificationsFromGraph(discourse: Discourse, request: RemoveTagRequestBase, node: Reference) {
    request.removedTags.foreach { tagUuid =>
      val tag = Classification.matchesOnUuid(tagUuid)
      discourse.add(tag)
      val tagging = Classifies.matches(tag, node)
      discourse.remove(tagging)
    }
  }

  private def addClassifcationsToGraph(discourse: Discourse, request: AddClassificationRequestBase, node: Reference) {
    request.addedTags.flatMap(tagConnectRequestToClassification(_)).foreach { tag =>
      val tags = Classifies.merge(tag, node)
      discourse.add(tags)
    }
  }


  override def update(context: RequestContext, uuid: String) = context.withUser { user =>
    import formatters.json.EditNodeFormat._

    context.withJson { (request: ReferenceUpdateRequest) =>
      db.transaction { tx =>
        val node = Reference.matchesOnUuid(uuid)
        val discourse = Discourse(node)
        deleteClassificationsFromGraph(discourse, request, node)
        addClassifcationsToGraph(discourse, request, node)

        tx.persistChanges(discourse) match {
          case Some(err) => BadRequest(s"Cannot update Reference with uuid '$uuid': $err'")
          case _         => Ok(Json.toJson(ClassifiedReferences.shapeResponse(Reference.wrap(node.rawItem))))
        }
      }
    }
  }
}
