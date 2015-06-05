package tasks

import common.ConfigString._
import model.WustSchema._
import modules.db.Database
import play.core.StaticApplication
import renesca._
import java.io.File

trait Task extends App {
  val db = new DbService
  db.restService = new RestService(
    server = "http://localhost:7474",
    credentials = Some(spray.http.BasicHttpCredentials("neo4j", "neo4j"))
  )
}

object SeedInit extends Task {
  println("Seeding Database")
  val discourse = Discourse.empty


  db.query("CREATE INDEX ON :POST(uuid)")

  discourse.add(Tag.local(title = Some("Problem"), description = "...a problem", isType = true))
  discourse.add(Tag.local(title = Some("Goal"), description = "...a goal", isType = true))
  discourse.add(Tag.local(title = Some("Idea"), description = "...an idea", isType = true))
  discourse.add(Tag.local(title = Some("Pro"), description = "...a pro", isType = true))
  discourse.add(Tag.local(title = Some("Con"), description = "...a con", isType = true))
  discourse.add(UserGroup.local("everyone"))

  db.persistChanges(discourse.graph)
  db.restService.actorSystem.shutdown()
}
