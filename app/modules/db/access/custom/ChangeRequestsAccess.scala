package modules.db.access.custom

import controllers.api.nodes.{ConnectParameter, RequestContext}
import model.WustSchema.{Created => SchemaCreated, _}
import modules.db.Database.db
import modules.db.GraphHelper._
import modules.db.access.{EndRelationAccessDefault, NodeAccessDefault}
import modules.db.{FactoryUuidNodeDefinition, ConcreteFactoryNodeDefinition, ConcreteNodeDefinition, RelationDefinition}
import renesca.Query

//TODO: one api to get them all?
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

case class PostAddTagsAccess() extends EndRelationAccessDefault[AddTags, AddTagsToPost, Post] {
  val nodeFactory = AddTags
  override def read(context: RequestContext, param: ConnectParameter[Post]) = {
    Right(context.user.map { user =>
      val userDef = ConcreteNodeDefinition(user)
      val updatedDef = ConcreteFactoryNodeDefinition(AddTags)
      val postDef = FactoryUuidNodeDefinition(Post, param.baseUuid)
      val relDef = RelationDefinition(updatedDef, AddTagsToPost, postDef)
      val votesDef = RelationDefinition(userDef, Votes, updatedDef)
      val query = s"match ${relDef.toQuery} where ${updatedDef.name}.applied = false optional match ${votesDef.toQuery(true, false)} return ${votesDef.name}, ${updatedDef.name}"
      val discourse = Discourse(db.queryGraph(Query(query, relDef.parameterMap ++ votesDef.parameterMap)))
      discourse.addTags
    }.getOrElse(Seq.empty))
  }
}

case class PostRemoveTagsAccess() extends EndRelationAccessDefault[RemoveTags, RemoveTagsToPost, Post] {
  val nodeFactory = RemoveTags
  override def read(context: RequestContext, param: ConnectParameter[Post]) = {
    Right(context.user.map { user =>
      val userDef = ConcreteNodeDefinition(user)
      val updatedDef = ConcreteFactoryNodeDefinition(RemoveTags)
      val postDef = FactoryUuidNodeDefinition(Post, param.baseUuid)
      val relDef = RelationDefinition(updatedDef, RemoveTagsToPost, postDef)
      val votesDef = RelationDefinition(userDef, Votes, updatedDef)
      val query = s"match ${relDef.toQuery} where ${updatedDef.name}.applied = false optional match ${votesDef.toQuery(true, false)} return ${votesDef.name}, ${updatedDef.name}"
      val discourse = Discourse(db.queryGraph(Query(query, relDef.parameterMap ++ votesDef.parameterMap)))
      discourse.removeTags
    }.getOrElse(Seq.empty))
  }
}
