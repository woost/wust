package tasks

import model.WustSchema._
import renesca.parameter.implicits._

object ClearDatabase extends Task with SeedTools {
  val time = 10
  println(s"Clearing database at ${db.restService.server} in ${time}s.")
  for(s <- Range.Int(time,0,-1)) {
    print(s" $s")
    Thread.sleep(1000)
  }
  println()
  println("Deleting all nodes and relations...")

  dbContext { implicit db =>
    db.query("match n optional match (n)-[r]-() delete n,r")
  }
}
