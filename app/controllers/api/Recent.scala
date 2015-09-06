package controllers.api

import model.WustSchema.{ Created => CreatedAction, _}
import modules.db.Database._
import modules.db._
import formatters.json.ApiNodeFormat._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import renesca.Query
import renesca.graph.Label
import renesca.parameter.PropertyKey
import modules.db.access.custom.TaggedTaggable

object Recent extends TaggedTaggable[UuidNode] with Controller {
  def index(label: Option[String]) = Action {
    // white list, so only exposed nodes can be searched
    val labels = ExposedNode.labels ++ label.map(Label(_))
    val nodeDef = LabelNodeDefinition(labels)

   val returnStatement = s"return ${nodeDef.name} order by ${nodeDef.name}.timestamp desc limit 20"
   val query = s"match ${ nodeDef.toQuery } $returnStatement"
   val discourse = Discourse(db.queryGraph(Query(query)))

    Ok(Json.toJson(shapeResponse(discourse.uuidNodes)))
  }
}
