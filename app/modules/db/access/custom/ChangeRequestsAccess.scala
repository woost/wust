package modules.db.access.custom

import controllers.api.nodes.{ConnectParameter, RequestContext}
import model.WustSchema.{Created => SchemaCreated, _}
import modules.db.Database.db
import modules.db.GraphHelper._
import modules.db.access._
import modules.db._
import renesca.Query
import play.api.mvc.Results._
import formatters.json.ChangeRequestFormat._
import play.api.libs.json._

case class InstantChangeRequestAccess() extends NodeAccessDefault[ChangeRequest] {
  val factory = ChangeRequest

  override def read(context: RequestContext) = {
    val page = context.page.getOrElse(0)
    val limit = context.limit
    val skip = page * limit

    val crDef = LabelNodeDefinition[TagChangeRequest](ChangeRequest.labels)
    val crTagsDef = RelationDefinition(crDef, ProposesTag, ConcreteFactoryNodeDefinition(Scope))
    val postDef = ConcreteFactoryNodeDefinition(Post)
    val tagsDef = RelationDefinition(ConcreteFactoryNodeDefinition(Scope), Tags, postDef)
    val connectsDef = ConcreteFactoryNodeDefinition(Connects)
    val connDef = RelationDefinition(postDef, PostToConnects, connectsDef)
    val classifiesDef = RelationDefinition(ConcreteFactoryNodeDefinition(Classification), Classifies, connectsDef)

    val query = s"""
    match ${crDef.toQuery}-[relation:`${UpdatedToPost.relationType}`|`${AddTagsToPost.relationType}`|`${RemoveTagsToPost.relationType}`]->${postDef.toQuery} where ${crDef.name}.applied = ${INSTANT}
    with ${postDef.name}, relation, ${crDef.name} order by ${crDef.name}.timestamp skip ${skip} limit ${limit}
    optional match ${crTagsDef.toQuery(false, true)}
    optional match ${tagsDef.toQuery(true, false)}
    optional match ${connDef.toQuery(false, true)}, ${classifiesDef.toQuery(true, false)}
    return *
    """

    val params = crDef.parameterMap ++ tagsDef.parameterMap ++ connDef.parameterMap ++ classifiesDef.parameterMap

    val discourse = Discourse(db.queryGraph(query, params))

    Left(Ok(Json.toJson(discourse.changeRequests)))
  }
}
