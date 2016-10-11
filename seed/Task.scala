package tasks

import model.WustSchema._
import renesca._
import renesca.parameter._

trait TaskQueryHandler extends QueryHandler {
  def serverName: String
  def shutdown(): Unit
  def newTransaction(): QueryHandler
  def transaction[T](code: QueryHandler => T, dummy: Int = 0): T
}

class FileWriterQueryHandler(fileName: String) extends TaskQueryHandler {
  val writer = new java.io.PrintWriter(fileName)
  val parameterRegex = "\\{[^{}: ]+\\}".r

  private def formatParameterValue(value: ParameterValue): String = value match {
    case StringPropertyValue(str) => s""""$str""""
    case v: PrimitivePropertyValue => v.toString
    case v: ArrayPropertyValue => s"""[ ${v.elements.map(formatParameterValue _).mkString(",")} ]"""
    case MapParameterValue(values) =>
      val formatted = values.mapValues(formatParameterValue _)
      val mapped = formatted.map { case (k,v) => s"`$k`: $v" }.mkString(",")
      s"{ $mapped }"
  }

  private def formatQuery(query: Query): String = {
    val r = parameterRegex.replaceAllIn(query.statement, m => {
      // println(m)
      val matched = m.group(0)
      val key = matched.drop(1).dropRight(1)
      val value = query.parameters(PropertyKey(key))
      formatParameterValue(value).replace("$", "\\$")
    }) + ";\n"
    println(r)
    r
  }

  private def logQueries(queries: Seq[Query]) {
    val formatted = queries.map(formatQuery)
    writer.write(formatted.mkString)
  }

  val serverName = "file://" + fileName
  def shutdown() {
    writer.close()
  }

  override protected def handleError(exceptions: Option[Exception]) = ???
  override protected def queryService(jsonRequest: json.Request): json.Response = ???
  override protected def executeQueries(queries: Seq[Query], resultDataContents: List[String]): List[json.Result] = {
    logQueries(queries)
    List.empty
  }

  override def newTransaction() = this
  override def transaction[T](code: QueryHandler => T, dummy: Int = 0): T = {
    code(newTransaction)
  }
}

class TaskDbService extends DbService with TaskQueryHandler {
  def serverName = restService.server

  override def shutdown() {
    restService.actorSystem.shutdown()
  }

  override def transaction[T](code: QueryHandler => T, dummy: Int = 0): T = {
    transaction((c: Transaction) => code(c.asInstanceOf[Transaction]))
  }
}

trait Task extends DbUtil with App
trait DbUtil {
  lazy val db = sys.env.get("SEED_QUERY_FILE").map { file =>
    new FileWriterQueryHandler(file)
  }.getOrElse {
    val db = new TaskDbService
    db.restService = new RestService(
      server = "http://localhost:7474",
      credentials = Some(spray.http.BasicHttpCredentials(sys.env.getOrElse("NEO4J_USER", "neo4j"), sys.env.getOrElse("NEO4J_PASS", "neo4j")))
    )
    db
  }

  def dbContext[T](code: TaskQueryHandler => T): T = {
    val res = code(db)
    db.shutdown()
    res
  }

  def modifyDiscourse(code: Discourse => Any)(implicit db: TaskQueryHandler): Unit = {
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

