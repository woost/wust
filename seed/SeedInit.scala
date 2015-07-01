package tasks

import common.ConfigString._
import model.WustSchema._
import modules.db.Database
import play.core.StaticApplication
import renesca._
import renesca.parameter.implicits._
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

  setupDbConstraints(db)

  discourse.add(Tag.merge(title = Some("Problem"), description = "...a problem", isType = true, merge = Set("title", "isType")))
  discourse.add(Tag.merge(title = Some("Goal"), description = "...a goal", isType = true, merge = Set("title", "isType")))
  discourse.add(Tag.merge(title = Some("Idea"), description = "...a idea", isType = true, merge = Set("title", "isType")))
  discourse.add(Tag.merge(title = Some("Pro"), description = "...a pro", isType = true, merge = Set("title", "isType")))
  discourse.add(Tag.merge(title = Some("Con"), description = "...a con", isType = true, merge = Set("title", "isType")))
  discourse.add(UserGroup.merge(name = "everyone", merge = Set("name")))

  db.persistChanges(discourse.graph)
  db.restService.actorSystem.shutdown()
}
