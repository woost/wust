package controllers.api

import controllers.api.nodes.Nodes
import model.{WustSchema => schema}
import modules.db.access.{NodeRead, StartAnyRelation, StartRelationRead}
import modules.requests.{NodeSchema, StartConnectSchema}

object Users extends Nodes[schema.User] {
  implicit val restFormat = formatters.json.UserFormats.RestFormat

  lazy val nodeSchema = NodeSchema(routePath, new NodeRead(schema.User), Map(
    "created"  -> StartConnectSchema(new StartAnyRelation(schema.Created)),
    "updated"  -> StartConnectSchema(new StartAnyRelation(schema.Updated)),
    "deleted"  -> StartConnectSchema(new StartAnyRelation(schema.Deleted))
  ))
}
