package tasks

import model.WustSchema._
import renesca.parameter.implicits._

object SeedDatabase extends Task with SeedTools {
  println(s"Seeding database at ${db.restService.server}...")

  dbContext { implicit db =>
    setupDbConstraints(db)

    //TODO: not only unique constraints in renesca, also support normal indices
    db.query("CREATE INDEX ON :TIMESTAMP(timestamp)")
    db.query("CREATE INDEX ON :LOGININFO(providerID)")
    db.query("CREATE INDEX ON :CREATEREQUEST(applied)")

    modifyDiscourse { implicit discourse =>
      discourse.add(
        mergeClassification("Problem", color = Some(90)),
        mergeClassification("Goal", color = Some(169)),
        mergeClassification("Idea", color = Some(255)),
        mergeClassification("Pro", color = Some(135)),
        mergeClassification("Con", color = Some(21)),
        mergeClassification("Bug", color = Some(57)),
        mergeClassification("Question", color = Some(301)),
        mergeClassification("Answer", color = Some(216)),
        mergeClassification("repliesTo")
      )

      discourse.add(UserGroup.merge(name = "everyone", merge = Set("name")))
    }
  }
}
