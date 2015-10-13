package tasks

import model.WustSchema._
import renesca.parameter.implicits._

object SeedDatabase extends Task with SeedTools {
  println(s"Seeding database at ${db.restService.server}...")

  dbContext { implicit db =>
    setupDbConstraints(db)

    //TODO: not only unique constraints in renesca, also support normal indices
    db.query(s"CREATE INDEX ON :`${Timestamp.label}`(timestamp)")
    db.query(s"CREATE INDEX ON :`${LoginInfo.label}`(providerID)")
    db.query(s"CREATE INDEX ON :`${ChangeRequest.label}`(status)")

    modifyDiscourse { implicit discourse =>
      discourse.add(
        mergeClassification("Problem", color = Some(90), symbol = "fa fa-flask"),
        mergeClassification("Goal", color = Some(169), symbol = "fa fa-crosshairs"),
        mergeClassification("Idea", color = Some(260), symbol = "fa fa-lightbulb-o"),
        mergeClassification("Pro", color = Some(135), symbol = "fa fa-thumbs-o-up"),
        mergeClassification("Contra", color = Some(21), symbol = "fa fa-thumbs-o-down"),
        // mergeClassification("Bug", color = Some(50)),
        mergeClassification("Question", color = Some(301), symbol = "fa fa-question"),
        // mergeClassification("Answer", color = Some(280)),
        mergeClassification("Task", color = Some(280), symbol = "fa fa-list-ul"),
        mergeClassification("Consequence", color = Some(70), symbol = "fa fa-arrow-right"),
        // mergeClassification("Dependency", color = Some(110)),
        // mergeClassification("Hypothesis", color = Some(110)),
        mergeClassification("Done", color = Some(150), symbol = "fa fa-check")
      )

      discourse.add(UserGroup.merge(name = "everyone", merge = Set("name")))
    }
  }
}
