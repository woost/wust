import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

import renesca._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class Neo4jSpec extends Specification {

  "Neo4j" should {
    "write isolate transactions" >> {
      val db = new DbService
      db.restService = new RestService(
        server = "http://localhost:7474",
        credentials = None
      )

      db.query("""create (n {uuuid: "penis", counter: 0})""")

      val tx1 = db.newTransaction
      val tx2 = db.newTransaction
      tx1.query("""match (n {uuuid: "penis"}) set n.tx1counter = n.counter""");
      tx2.query("""match (n {uuuid: "penis"}) set n.tx2counter = n.counter""");
      tx1.query("""match (n {uuuid: "penis"}) set n.counter = n.tx1counter + 1""");
      tx2.query("""match (n {uuuid: "penis"}) set n.counter = n.tx2counter + 1""");
      tx1.commit()
      tx2.commit()

      val table = db.queryTable("""match (n {uuuid: "penis"}) return n.counter""")
      table.rows.head("n.counter").asLong mustEqual 2L
    }
  }
}
