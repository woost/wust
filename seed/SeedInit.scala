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
        mergeClassification("Problem", color = Some(90)),
        mergeClassification("Goal", color = Some(169)),
        mergeClassification("Idea", color = Some(260)),
        mergeClassification("Pro", color = Some(135)),
        mergeClassification("Con", color = Some(21)),
        mergeClassification("Bug", color = Some(50)),
        mergeClassification("Question", color = Some(301)),
        mergeClassification("Answer", color = Some(280)),
        mergeClassification("Cause", color = Some(70)),
        mergeClassification("Dependency", color = Some(110)),
        mergeClassification("Done", color = Some(150))
      )

      discourse.add(UserGroup.merge(name = "everyone", merge = Set("name")))
    }
  }
}
