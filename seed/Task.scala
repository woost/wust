package tasks

import common.ConfigString._
import model.WustSchema._
import modules.db.Database
import play.core.StaticApplication
import renesca._
import renesca.parameter.implicits._
import java.io.File

trait Task extends App {
  private val db = new DbService
  db.restService = new RestService(
    server = "http://localhost:7474",
    credentials = Some(spray.http.BasicHttpCredentials("neo4j", "neo4j"))
  )

  def dbContext(code: DbService => Any): Unit = {
    code(db)
    db.restService.actorSystem.shutdown()
  }

  def modifyDiscourse(code: Discourse => Any)(implicit db: DbService): Unit = {
    val discourse = Discourse.empty
    code(discourse)
    db.transaction(_.persistChanges(discourse)).foreach(println)
  }
}

