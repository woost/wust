package tasks

import model.WustSchema._
import renesca.parameter.implicits._

object Usertest1Init extends Task with SeedTools {
  ClearDatabase.main(Array())
  SeedDatabase.main(Array())
  println(s"Preparing Database for Usertest1 on ${db.restService.server}...")

  dbContext { implicit db =>
    modifyDiscourse { discourse =>
      val problem = matchClassification("Problem")
      val question = matchClassification("Question")
      val pro = matchClassification("Pro")

      val rootnode = createPost("Was denkt ihr über Wäsche?")
      val stinken = createPost("Meine Wäsche stinkt.")
      val schmutzig = createPost("Meine Wäsche ist schmutzig.")
      val wasser = createPost("Ich esse gerne Wasser.")
      val meinung = createPost("Der Platz im Bus neben dir ist bestimmt immer frei.")

      val stinkenConnects = Connects.create(stinken, rootnode)
      val schmutzigConnects = Connects.create(schmutzig, rootnode)
      val wasserConnects = Connects.create(wasser, rootnode)
      val meinungConnects = Connects.create(meinung, stinken)

      discourse.add(
        stinken,
        schmutzig,
        wasser,
        meinung,
        stinkenConnects,
        schmutzigConnects,
        wasserConnects,
        meinungConnects,
        classify(meinungConnects, pro),
        // classify(rootnode, question), //TODO on relation between scope and post
        classify(stinkenConnects, problem),
        classify(schmutzigConnects, problem)
      )
    }
  }
  
  ImportReddit.main(Array())
}
