package tasks

import model.WustSchema._
import renesca.parameter.implicits._

object SeedInit extends Task with TagTools {
  println("Seeding Database...")

  dbContext { implicit db =>
    setupDbConstraints(db)

    modifyDiscourse { implicit discourse =>
      discourse.add(
        mergeTag("Problem", isType = true),
        mergeTag("Goal", isType = true),
        mergeTag("Idea", isType = true),
        mergeTag("Pro", isType = true),
        mergeTag("Con", isType = true),
        mergeTag("Comment")
      )

      discourse.add(UserGroup.merge(name = "everyone", merge = Set("name")))

      discourse.add(Connects.create(Post.create("hello"), Post.create("moon")))
    }
  }
}
