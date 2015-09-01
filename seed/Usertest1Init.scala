package tasks

import model.WustSchema._
import renesca.parameter.implicits._

object Usertest1Init extends Task with SeedTools {
  ClearDatabase.main(Array())
  SeedDatabase.main(Array())
  println(s"Preparing Database for Usertest1 on ${db.restService.server}...")

  dbContext { implicit db =>
    modifyDiscourse { discourse =>
      val replies = mergeClassification("repliesTo")
      val problem = mergeClassification("Problem")
      val question = mergeClassification("Question")
      val pro = mergeClassification("Pro")
      val startpost = mergeClassification("StartPost")

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
        tag(rootnode, startpost),
        tag(meinung, pro),
        tag(rootnode, question),
        tag(stinkenConnects, replies),
        tag(schmutzigConnects, replies),
        tag(wasserConnects, replies),
        tag(meinungConnects, replies),
        tag(stinken, problem),
        tag(schmutzig, problem)
      )
    }
  }
  
  ImportReddit.main(Array())
}
