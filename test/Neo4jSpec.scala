import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

import renesca._
import renesca.parameter.PropertyKey

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class Neo4jSpec extends Specification {

  args(skipAll = true)

  "Neo4j" should {

    "write isolate transactions" >> {
      import scala.concurrent._
      import scala.concurrent.duration._
      import ExecutionContext.Implicits.global

      val db = new DbService
      db.restService = new RestService(
        server = "http://localhost:7474",
        credentials = None
      )

      db.query("""create (n {uuuid: "penis", counter: 0})""")

      val tx1 = db.newTransaction
      val tx2 = db.newTransaction

      val f = Future {
        val g2 = tx2.queryGraph("""match (n {uuuid: "penis"}) set n.__lock=true return n""");
        tx2.query(s"""match (n {uuuid: "penis"}) set n.counter = ${g2.nodes.head.properties(PropertyKey("counter"))} + 1 remove n.__lock""");
        tx2.commit()
      }

      val g1 = tx1.queryGraph("""match (n {uuuid: "penis"}) set n.__lock=true return n""");
      tx1.query(s"""match (n {uuuid: "penis"}) set n.counter = ${g1.nodes.head.properties(PropertyKey("counter"))} + 1 remove n.__lock""");
      tx1.commit()

      Await.ready(f, Duration(10, SECONDS))
      val table = db.queryTable("""match (n {uuuid: "penis"}) return n.counter""")
      db.query("""match (n {uuuid: "penis"}) delete n""")

      table.rows.head("n.counter").asLong mustEqual 2L
    }
  }
}
