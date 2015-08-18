package tasks

import model.WustSchema._
import renesca.parameter.implicits._

object SeedInit extends Task with SeedTools {
  println("Seeding Database...")

  dbContext { implicit db =>
    setupDbConstraints(db)

    //TODO: not only unique constraints in renesca, also support normal indices
    db.query("CREATE INDEX ON :TIMESTAMP(timestamp)")

    modifyDiscourse { implicit discourse =>
      discourse.add(
        DummyUser.create(name = "anonymous"),

        mergeClassification("Problem", color = Some(46)),
        mergeClassification("Goal", color = Some(158)),
        mergeClassification("Idea", color = Some(210)),
        mergeClassification("Pro", color = Some(105)),
        mergeClassification("Con", color = Some(359)),
        mergeClassification("StartPost"),
        mergeClassification("repliesTo")
      )

      discourse.add(UserGroup.merge(name = "everyone", merge = Set("name")))
    }
  }
}
