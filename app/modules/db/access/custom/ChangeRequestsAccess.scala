package modules.db.access.custom

import controllers.api.nodes.{ConnectParameter, RequestContext}
import model.WustSchema.{Created => SchemaCreated, _}
import modules.db.Database.db
import modules.db.GraphHelper._
import modules.db.access.{EndRelationAccessDefault, NodeAccessDefault}
import modules.db.{FactoryUuidNodeDefinition, ConcreteFactoryNodeDefinition, ConcreteNodeDefinition, RelationDefinition}
import renesca.Query

case class PostUpdatedAccess() extends EndRelationAccessDefault[Updated, UpdatedToPost, Post] {
  val nodeFactory = Updated
  override def read(context: RequestContext, param: ConnectParameter[Post]) = {
    Right(context.user.map { user =>
      val userDef = ConcreteNodeDefinition(user)
      val updatedDef = ConcreteFactoryNodeDefinition(Updated)
      val postDef = FactoryUuidNodeDefinition(Post, param.baseUuid)
      val relDef = RelationDefinition(updatedDef, UpdatedToPost, postDef)
      val votesDef = RelationDefinition(userDef, Votes, updatedDef)
      //TODO: graphdefinition with arbitrary properties, not only uuid
      val query = s"match ${relDef.toQuery} where ${updatedDef.name}.applied = false optional match ${votesDef.toQuery(true, false)} return ${votesDef.name}, ${updatedDef.name}"
      val discourse = Discourse(db.queryGraph(Query(query, relDef.parameterMap ++ votesDef.parameterMap)))
      discourse.updateds
    }.getOrElse(Seq.empty))
  }
}

case class PostUpdatedTagsAccess() extends EndRelationAccessDefault[UpdatedTags, UpdatedTagsToPost, Post] {
  val nodeFactory = UpdatedTags
  override def read(context: RequestContext, param: ConnectParameter[Post]) = {
    Right(context.user.map { user =>
      val userDef = ConcreteNodeDefinition(user)
      val updatedDef = ConcreteFactoryNodeDefinition(UpdatedTags)
      val postDef = FactoryUuidNodeDefinition(Post, param.baseUuid)
      val relDef = RelationDefinition(updatedDef, UpdatedTagsToPost, postDef)
      val votesDef = RelationDefinition(userDef, Votes, updatedDef)
      val query = s"match ${relDef.toQuery} where ${updatedDef.name}.applied = false optional match ${votesDef.toQuery(true, false)} return ${votesDef.name}, ${updatedDef.name}"
      val discourse = Discourse(db.queryGraph(Query(query, relDef.parameterMap ++ votesDef.parameterMap)))
      discourse.updatedTags
    }.getOrElse(Seq.empty))
  }
}
