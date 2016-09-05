package tasks

import model.WustSchema._
import renesca.parameter.implicits._

object SeedDatabase extends Task with SeedTools {
  println(s"Seeding database at ${db.serverName}...")

  dbContext { implicit db =>
    setupDbConstraints(db)

    //TODO: not only unique constraints in renesca, also support normal indices
    db.query(s"CREATE INDEX ON :`${Timestamp.label}`(timestamp)")
    db.query(s"CREATE INDEX ON :`${LoginInfo.label}`(providerID)")
    db.query(s"CREATE INDEX ON :`${ChangeRequest.label}`(status)")

    modifyDiscourse { implicit discourse =>
      discourse.add(
        mergeClassification("Done", color = Some(150), symbol = "fa fa-check", precedence = 100),

        mergeClassification("Problem", color = Some(90), symbol = "fa fa-flask", precedence = 20),
        mergeClassification("Goal", color = Some(169), symbol = "fa fa-crosshairs", precedence = 20),
        mergeClassification("Idea", color = Some(260), symbol = "fa fa-lightbulb-o", precedence = 20),

        mergeClassification("Question", color = Some(301), symbol = "fa fa-question", precedence = 10),
        mergeClassification("Task", color = Some(280), symbol = "fa fa-list-ul", precedence = 10),
        mergeClassification("Cause", color = Some(70), symbol = "fa fa-arrow-left", precedence = 10),

        mergeClassification("Pro", color = Some(135), symbol = "fa fa-thumbs-o-up", precedence = 6),
        mergeClassification("Contra", color = Some(21), symbol = "fa fa-thumbs-o-down", precedence = 5)

        // mergeClassification("Bug", color = Some(50)),
        // mergeClassification("Answer", color = Some(280)),
        // mergeClassification("Dependency", color = Some(110)),
        // mergeClassification("Hypothesis", color = Some(110)),
      )

      discourse.add(UserGroup.merge(name = "everyone", merge = Set("name")))
    }
  }
}
