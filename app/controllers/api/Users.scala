package controllers.api

import controllers.api.nodes.Nodes
import model.WustSchema._
import modules.db.access.{NodeRead, StartAnyRelation, StartRelationRead}
import modules.requests.{NodeSchema, StartConnectSchema}

object Users extends Nodes[User] {
  implicit val restFormat = formatters.json.UserFormats.RestFormat

  lazy val nodeSchema = NodeSchema(routePath, new NodeRead(User), Map(
    "goals"  -> StartConnectSchema(new StartRelationRead(Contributes, Goal)),
    "problems"  -> StartConnectSchema(new StartRelationRead(Contributes, Problem)),
    "ideas"  -> StartConnectSchema(new StartRelationRead(Contributes, Idea)),
    "contributes"  -> StartConnectSchema(new StartAnyRelation(Contributes))
  ))
}
