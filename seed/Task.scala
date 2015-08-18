package tasks

import model.WustSchema._
import renesca._

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

object AllTasks extends App {
  SeedInit.main(Array())
  ImportReddit.main(Array())
  ImportHackerNews.main(Array())
}

