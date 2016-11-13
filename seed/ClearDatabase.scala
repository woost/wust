package tasks

import model.WustSchema._
import renesca.parameter.implicits._

object ClearDatabase extends Task with SeedTools {
  println(s"Clearing database at ${db.serverName}")
  scala.io.StdIn.readLine("Press enter to continue")
  println("Deleting all nodes and relations...")

  dbContext { implicit db =>
    db.query("match (n) optional match (n)-[r]-() delete n,r")
  }
}
