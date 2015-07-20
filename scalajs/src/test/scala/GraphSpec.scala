package renesca.js

import utest._

object GraphSpec extends TestSuite {
  val tests = TestSuite {
    'RawGraph {
      implicit def n(id: String) = new RawNode(id, "", "", None, hyperEdge = false, None, None)
      implicit def r(t: (String, String)) = new RawRelation(t._1, t._2)
      def h(startId: String, id: String, endId: String) = new RawNode(id, "", "", None, hyperEdge = true, Some(startId), Some(endId))
      def graph(nodes: Set[RawNode], relations: Set[RawRelation]) = new RawGraph(nodes, relations, null)

      val A = n("A")
      val B = n("B")
      val C = n("C")
      val D = n("D")
      val E = n("E")
      val ArB = r("A" -> "B")

      val AXB = h("A", "X", "B")
      val ArX = r("A", "X")
      val XrB = r("X", "B")

      val CrX = r("C", "X")

      val CYX = h("C", "Y", "X")
      val CrY = r("C", "Y")
      val YrX = r("Y", "X")

      val XZD = h("X", "Z", "D")
      val XrZ = r("X", "Z")
      val ZrD = r("Z", "D")

      val YrZ = r("Y", "Z")

      val APX = h("A", "P", "X")
      val ArP = r("A", "P")
      val PrX = r("P", "X")

      val BQX = h("B", "Q", "X")
      val BrQ = r("B", "Q")
      val QrX = r("Q", "X")

      val ASQ = h("A", "S", "Q")
      val ArS = r("A", "S")
      val SrQ = r("S", "Q")

      'deleteRelation {
        val g = graph(Set(A, B), Set(ArB))

        g.remove(ArB)

        assert(g.nodes == Set(A, B), g.relations == Set())
      }

      'deleteNodeWithOutRelation {
        val g = graph(Set(A, B), Set(ArB))

        g.remove(A)

        assert(g.nodes == Set(B), g.relations == Set())
      }

      'deleteNodeWithInRelation {
        val g = graph(Set(A, B), Set(ArB))

        g.remove(B)

        assert(g.nodes == Set(A), g.relations == Set())
      }

      'deleteNodeWithOutHyperRelation {
        val g = graph(Set(A, B, AXB), Set(ArX, XrB))

        g.remove(A)

        assert(g.nodes == Set(B), g.relations == Set())
      }

      'deleteNodeWithInHyperRelation {
        val g = graph(Set(A, B, AXB), Set(ArX, XrB))

        g.remove(B)

        assert(g.nodes == Set(A), g.relations == Set())
      }

      'deleteNodeWithRelationAndHyperRelation {
        val g = graph(Set(A, B, AXB), Set(ArX, XrB, ArB))

        g.remove(B)

        assert(g.nodes == Set(A), g.relations == Set())
      }

      'deleteHyperRelation {
        val g = graph(Set(A, B, AXB), Set(ArX, XrB))

        g.remove(AXB)

        assert(g.nodes == Set(A, B), g.relations == Set())
      }

      'deleteHyperRelationWithRelation {
        val g = graph(Set(A, B, AXB, C), Set(ArX, XrB, CrX))

        g.remove(AXB)

        assert(g.nodes == Set(A, B, C), g.relations == Set())
      }

      'deleteHyperRelationWithHyperRelation {
        val g = graph(Set(A, B, AXB, C, CYX), Set(ArX, XrB, CrY, YrX))

        g.remove(AXB)

        assert(g.nodes == Set(A, B, C), g.relations == Set())
      }
      'deleteHyperRelationWithInterconnectedHyperRelations {
        val g = graph(Set(A, B, AXB, C, CYX, D, XZD), Set(ArX, XrB, CrY, YrX, XrZ, ZrD, YrZ))

        'deleteHyperRelation {
          g.remove(AXB)

          assert(g.nodes == Set(A, B, C, D), g.relations == Set())
        }

        'deleteNode {
          g.remove(A)

          assert(g.nodes == Set(B, C, D), g.relations == Set())
        }
      }

      'hierarchyCollapse {
        val g = graph(Set(A, B, AXB, APX, BQX, ASQ), Set(ArX, XrB, ArP, PrX, BrQ, QrX, ArS, SrQ))

        g.remove(AXB)

        assert(g.nodes == Set(A, B), g.relations == Set())
      }
    }
  }
}
