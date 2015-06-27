package controllers.api

import controllers.api.nodes.Nodes
import model.{WustSchema => schema}
import modules.db.access.{NodeRead, StartAnyRelation, StartRelationRead}
import modules.requests.dsl._

object Users extends Nodes[schema.User] {
  implicit val restFormat = formatters.json.UserFormats.RestFormat

  val node = N(schema.User, NodeRead(schema.User),
    ("created", N --> StartAnyRelation(schema.Created)),
    ("updated", N --> StartAnyRelation(schema.Updated)),
    ("deleted", N --> StartAnyRelation(schema.Deleted))
  )
}
