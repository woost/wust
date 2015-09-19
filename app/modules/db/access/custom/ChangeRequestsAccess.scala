package modules.db.access.custom

import controllers.api.nodes.{ConnectParameter, RequestContext}
import model.WustSchema.{Created => SchemaCreated, _}
import modules.db.Database.db
import modules.db.GraphHelper._
import modules.db.access._
import modules.db._
import renesca.Query
import play.api.mvc.Results._

case class InstantChangeRequestAccess() extends NodeAccessDefault[ChangeRequest] {
  val factory = ChangeRequest

  override def read(context: RequestContext) = {
    val page = context.page.getOrElse(0)
    val skip = page * context.limit

    val crDef = LabelNodeDefinition[TagChangeRequest](ChangeRequest.labels)
    val crTagsDef = RelationDefinition(crDef, ProposesTag, ConcreteFactoryNodeDefinition(Scope))
    val postDef = ConcreteFactoryNodeDefinition(Post)
    val tagsDef = RelationDefinition(ConcreteFactoryNodeDefinition(Scope), Tags, postDef)
    val connectsDef = ConcreteFactoryNodeDefinition(Connects)
    val connDef = RelationDefinition(postDef, PostToConnects, connectsDef)
    val classifiesDef = RelationDefinition(ConcreteFactoryNodeDefinition(Classification), Classifies, connectsDef)

    val query = s"""
    match ${crDef.toQuery}-[relation:`${UpdatedToPost.relationType}`|`${AddTagsToPost.relationType}`|`${RemoveTagsToPost.relationType}`]->${postDef.toQuery} where ${crDef.name}.instantChange = true
    optional match ${crTagsDef.toQuery(false, true)}
    optional match ${tagsDef.toQuery(true, false)}
    optional match ${connDef.toQuery(false, true)}, ${classifiesDef.toQuery(true, false)}
    return *
    order by ${crDef.name}.timestamp skip ${skip} limit ${context.limit}"""

    val params = crDef.parameterMap ++ tagsDef.parameterMap ++ connDef.parameterMap ++ classifiesDef.parameterMap

    val discourse = Discourse(db.queryGraph(query, params))

    Right(discourse.changeRequests)
  }
}

case class TagChangeRequestAccess[NODE <: TagChangeRequest](factory: TagChangeRequestMatchesFactory[NODE]) extends NodeAccessDefault[NODE] {
  //TODO: paging
  override def read(context: RequestContext) = {
    val tagDef = ConcreteFactoryNodeDefinition(Scope)
    val nodeDef = ConcreteFactoryNodeDefinition(factory)
    val tagsDef = RelationDefinition(nodeDef, ProposesTag, tagDef)

    val query = s""" match ${tagsDef.toQuery} return * """
    val params = tagsDef.parameterMap

    val discourse = Discourse(db.queryGraph(query, params))

    Right(discourse.nodesAs(factory))
  }

  override def read(context: RequestContext, uuid: String) = {
    val tagDef = ConcreteFactoryNodeDefinition(Scope)
    val nodeDef = FactoryUuidNodeDefinition(factory, uuid)
    val tagsDef = RelationDefinition(nodeDef, ProposesTag, tagDef)

    val query = s""" match ${tagsDef.toQuery} return * """
    val params = tagsDef.parameterMap

    val discourse = Discourse(db.queryGraph(query, params))

    discourse.nodesAs(factory).headOption match {
      case Some(node) => Right(node)
      case None => Left(NotFound(s"Cannot find node with uuid '$uuid'"))
    }
  }
}

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
      val query = s"match ${relDef.toQuery} where ${updatedDef.name}.applied = 0 optional match ${votesDef.toQuery(true, false)} return ${votesDef.name}, ${updatedDef.name}"
      val discourse = Discourse(db.queryGraph(query, relDef.parameterMap ++ votesDef.parameterMap))
      discourse.updateds
    }.getOrElse(Seq.empty))
  }
}

case class PostTagChangeRequestAccess() extends RelationAccessDefault[Post, TagChangeRequest] {
  val nodeFactory = TagChangeRequestMatches

  override def read(context: RequestContext, param: ConnectParameter[Post]) = {
    Right(context.user.map { user =>
      val userDef = ConcreteNodeDefinition(user)
      val updatedDef = ConcreteFactoryNodeDefinition(nodeFactory)
      val postDef = FactoryUuidNodeDefinition(Post, param.baseUuid)
      val votesDef = RelationDefinition(userDef, Votes, updatedDef)
      val scopeDef = ConcreteFactoryNodeDefinition(Scope)
      val proposes = RelationDefinition(updatedDef, ProposesTag, scopeDef)
      val query = s"match ${updatedDef.toQuery}-[:`${AddTags.endRelationType}`|`${RemoveTags.endRelationType}`]->${postDef.toQuery} where ${updatedDef.name}.applied = 0 optional match ${votesDef.toQuery(true, false)} optional match ${proposes.toQuery(false, true)} return ${votesDef.name}, ${proposes.name}, ${updatedDef.name}"
      val discourse = Discourse(db.queryGraph(query, postDef.parameterMap ++ votesDef.parameterMap))
      discourse.tagChangeRequests
    }.getOrElse(Seq.empty))
  }

}
