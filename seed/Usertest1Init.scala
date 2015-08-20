package tasks

import model.WustSchema._
import renesca.parameter.implicits._

object Usertest1Init extends Task with SeedTools {
  ClearDatabase.main(Array())
  SeedInit.main(Array())
  println(s"Preparing Database for Usertest1 on ${db.restService.server}...")

  dbContext { implicit db =>
    modifyDiscourse { discourse =>
      val startPost = createPost("Das ist eine Frage?")
      val reply = createPost("Eine Antwort.")
      discourse.add(
        startPost,
        Connects.create(reply, startPost)
      )
    }
  }
}
