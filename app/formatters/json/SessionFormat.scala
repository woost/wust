package formatters.json

import model.WustSchema._
import play.api.libs.json._
import renesca.graph.{Label, RelationType}
import renesca.schema._

object SessionFormat {
  // implicit object NodeFormat extends Format[Votes] {
  //   def reads(json: JsValue) = ???

  //   def writes(votes: Votes) = {
  //     JsObject(Seq(
  //       ("startId", JsString(votes.endNode.startNodeOpt.map(_.uuid).getOrElse(""))),
  //       ("endId", JsString(votes.endNode.endNodeOpt.map(_.uuid).getOrElse(""))),
  //       ("weight", JsNumber(votes.weight))
  //     ))
  //   }
  // }
}
