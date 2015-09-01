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
      val tagDef = ConcreteFactoryNodeDefinition(TagLike)
      val nodeDef = ConcreteFactoryNodeDefinition(Taggable)
      val tagsDef = HyperNodeDefinition(tagDef, Tags, nodeDef)

      val query = s"""
      match ${tagsDef.toQuery} where ${nodeDef.name}.uuid in {nodeUuids}
      return *
      """
      val params = nodeDef.parameterMap ++ tagDef.parameterMap ++ tagsDef.parameterMap ++ Map("nodeUuids" -> response.map(_.uuid).toSeq)

      val discourse = Discourse(db.queryGraph(Query(query, params.toMap)))

      discourse.add(response.toSeq: _*)
    }

    response
  }
}

object TaggedTaggable {
  def apply[NODE <: UuidNode] = new TaggedTaggable[NODE]
}
