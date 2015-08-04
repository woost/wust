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

object Recent extends Controller {
  //FIXME: white list labels!!
  def index(label: Option[String]) = Action {
    // white list, so only exposed nodes can be searched
    val labels = ContentNode.labels ++ label.map(Label(_))
    val nodeDef = LabelNodeDefinition(labels)

    val userDef = ConcreteFactoryNodeDefinition(User)
    val relationDef = RelationDefinition(userDef, CreatedAction, nodeDef)

   val returnStatement = s"with ${relationDef.name}, ${relationDef.endDefinition.name} return ${relationDef.endDefinition.name} order by ${relationDef.name}.timestamp desc"
   val query = s"match ${ relationDef.toQuery } $returnStatement"
   val discourse = Discourse(db.queryGraph(Query(query, relationDef.parameterMap)))

    Ok(Json.toJson(discourse.nodes))
  }
}
