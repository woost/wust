package renesca.js

import utest._

object GraphSpec extends TestSuite {
  val tests = TestSuite {
    'RawGraph {
      implicit def n(id: String) = new RawNode(id, "", "", None, hyperEdge = false, None, None)
      def r(startId: String, endId: String) = new RawRelation(startId, endId)
      implicit def rel(t: (String, String)) = new RawRelation(t._1, t._2)
      def h(id: String, startId: String, endId: String) = new RawNode(id, "", "", None, hyperEdge = true, Some(startId), Some(endId))
      def graph(nodes: Set[RawNode], relations: Set[RawRelation]) = new RawGraph(nodes, relations, null)
      def nodeIds(g: RawGraph) = g.nodes.map(_.id)
      def relIds(g: RawGraph) = g.relations.map(r => (r.startId -> r.endId))

      'deleteRelation {
        val r = rel("A" -> "B")
        val g = graph(Set("A", "B"), Set(r))
        g.remove(r)
        assert(nodeIds(g) == Set("A", "B"))
        assert(relIds(g) == Set())
      }
    }
  }
}
