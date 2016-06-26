package tasks

import model.WustSchema._
import renesca._

trait Task extends App {
  val db = new DbService
  db.restService = new RestService(
    server = "http://localhost:7474",
    credentials = Some(spray.http.BasicHttpCredentials(sys.env.getOrElse("NEO4J_USER", "neo4j"), sys.env.getOrElse("NEO4J_PASS", "neo4j")))
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

object A_ClearSeedRedditSOHN extends App {
  ClearDatabase.main(Array())
  SeedDatabase.main(Array())
  ImportReddit.main(Array())
  ImportStackOverflow.main(Array())
  ImportHackerNews.main(Array())
}

