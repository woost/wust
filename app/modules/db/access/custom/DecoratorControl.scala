package modules.db.access.custom

import controllers.api.nodes.{HyperConnectParameter, RequestContext}
import modules.db.Database._
import modules.db.access._
import modules.db._
import modules.requests.ConnectResponse
import play.api.mvc.Results._
import model.WustSchema._
import renesca.Query
import renesca.parameter._
import renesca.parameter.implicits._

// does not allow dummy users for any request
class CheckUser extends AccessDecoratorControl with AccessDecoratorControlDefault {
  override def acceptRequest(context: RequestContext) = {
    if (context.user.isDummy)
      Some(Unauthorized("Not Authorized"))
    else
      None
  }
}

object CheckUser {
  def apply = new CheckUser
}

// only allows read requests
class CheckUserWrite extends CheckUser {
  override def acceptRequestRead(context: RequestContext) = None
}

object CheckUserWrite {
  def apply = new CheckUserWrite
}

// adds tags to the results by querying for them and adding them to the graph
// it might be better to implement this in the nodeaccess/relationaccess directly
// because shapeResponse takes the original response and queries for its tags.
// so this costs an extra query. instead we could implement this in a node- or
// relationaccess and include the tags in the read queries.
// Still, this solution is very flexible as we can add decorators to other
// accesses easily: PostAccess.apply + TaggedTaggable(Post)
class TaggedTaggable[NODE <: UuidNode] extends AccessNodeDecoratorControl[NODE] with AccessNodeDecoratorControlDefault[NODE] {
  override def shapeResponse(response: NODE) = {
    shapeResponse(List(response)).head
  }

  override def shapeResponse(response: Iterable[NODE]) = {
    //TODO: share code with component query
    if (!response.isEmpty) {
      val tagDef = LabelNodeDefinition[VoteDimension with TagLike](TagLike.labels)
      val nodeDef = LabelNodeDefinition[Votable with Taggable](Taggable.labels)
      val tagsDef = HyperNodeDefinition(tagDef, Tags, nodeDef)

      val query = s"""
      match ${tagsDef.toQuery} where ${nodeDef.name}.uuid in {nodeUuids}
      return *
      """
      val params = nodeDef.parameterMap ++ tagDef.parameterMap ++ tagsDef.parameterMap ++ Map("nodeUuids" -> response.map(_.uuid).toSeq)

      val dimDef = HyperNodeDefinition(tagDef, Dimensionizes, nodeDef)
      val userUpDef = ConcreteFactoryNodeDefinition(User)
      val votesUpDef = RelationDefinition(userUpDef, Votes, dimDef)
      val userDownDef = ConcreteFactoryNodeDefinition(User)
      val votesDownDef = RelationDefinition(userDownDef, Votes, dimDef)
      //TODO: only get the tags with votes, the rest will get default values anyways
      val tagWeightQuery = s"""
      match ${tagsDef.toQuery} where ${nodeDef.name}.uuid in {nodeUuids}
      optional match ${dimDef.toQuery(false, false)}
      optional match ${votesDownDef.toQuery(true, false)} where ${votesDownDef.name}.weight = -1
      optional match ${votesUpDef.toQuery(true, false)} where ${votesUpDef.name}.weight = 1
      with ${tagDef.name}, count(${votesUpDef.name}.weight) as up, count(${votesDownDef.name}.weight) as down
      return ${tagDef.name}.uuid, ${tagweight("up","down")} as weight
      """

      val tagWeightParams = params ++ dimDef.parameterMap ++ userDownDef.parameterMap ++ userUpDef.parameterMap ++ votesDownDef.parameterMap ++ votesUpDef.parameterMap

      // val discourse = Discourse(db.queryGraph(Query(query, params.toMap)))

      // val params = focusNode.parameterMap
      implicit val discourseRawGraph = db.queryGraph(Query(query, params))
      val discourse = Discourse(discourseRawGraph)
      val tagweights = db.queryTable(Query(tagWeightQuery, tagWeightParams))

      // build hashmap weights: Map tags-hyperrelation.uuid -> weight
      val weights = tagweights.rows.map { tagweight =>
        tagweight(s"${tagDef.name}.uuid").asString -> tagweight("weight").asInstanceOf[DoublePropertyValue]
      }.toMap

      // write weights in to tags-hyperrelations
      for(tags <- discourse.tags) {
        tags.rawItem.properties("weight") = weights.getOrElse(tags.uuid, tagweight_p)
      }

      discourse.add(response.toSeq: _*)
    }

    response
  }
}

object TaggedTaggable {
  def apply[NODE <: UuidNode] = new TaggedTaggable[NODE]
}
