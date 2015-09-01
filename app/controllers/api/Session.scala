package controllers.api

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import model.WustSchema._
import modules.auth.HeaderEnvironmentModule
import modules.db.Database._
import modules.db._
import play.api.libs.json._
import formatters.json.SessionFormat._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import renesca.Query
import renesca.graph.Label
import renesca.parameter.PropertyKey
import modules.db.access.custom.TaggedTaggable

object Session extends TaggedTaggable[UuidNode] with Controller with Silhouette[RealUser, JWTAuthenticator] with HeaderEnvironmentModule {
  def votes() = UserAwareAction { request =>
    request.identity.map { user =>
      Ok(JsArray(Seq()))
      // val userDef = ConcreteNodeDefinition(user)
      // val dimDef = ConcreteFactoryNodeDefinition(VoteDimension)
      // val votableDef = ConcreteFactoryNodeDefinition(Votable)
      // val catDef = HyperNodeDefinition(dimDef, Dimensionizes, votableDef)
      // val relDef = RelationDefinition(userDef, Votes, catDef)

      // val query = s"match ${ relDef.toQuery } return *"
      // val params = userDef.parameterMap ++ dimDef.parameterMap ++ votableDef.parameterMap ++ catDef.parameterMap ++ relDef.parameterMap
      // val discourse = Discourse(db.queryGraph(Query(query, params)))

      // //FIXME: renesca does not properly wrap hyperrelation start nodes and end nodes, so we do it manually
      // //--> should use SessionFormat
      // // Ok(Json.toJson(discourse.votes))
      // Ok(JsArray(discourse.votes.map { vote =>
      //   val node = vote.endNode.rawItem
      //   val startRelation = discourse.graph.relations.find(relation => relation.relationType == Dimensionizes.startRelationType && relation.endNode == node)
      //   val endRelation = discourse.graph.relations.find(relation => relation.relationType == Dimensionizes.endRelationType && relation.startNode == node)
      //   val wrappedNode = Dimensionizes.wrap(startRelation.get, node, endRelation.get)
      //   JsObject(Seq(
      //     ("startId", JsString(wrappedNode.startNodeOpt.map(_.uuid).getOrElse(""))),
      //     ("endId", JsString(wrappedNode.endNodeOpt.map(_.uuid).getOrElse(""))),
      //     ("weight", JsNumber(vote.weight))
      //   ))
      // }))
    }.getOrElse(Unauthorized("Only users can have a session"))
  }
}
