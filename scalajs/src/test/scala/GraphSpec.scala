package renesca.js

import utest._

import scala.scalajs.js

object GraphSpec extends TestSuite {
  // console.warn polyfill
  if(js.Dynamic.global.console.warn == js.undefined)
    js.Dynamic.global.console.updateDynamic("warn")((m: String) => println(m))

  val tests = TestSuite {
    import TestHelpers._

    'RawGraph {
      'deleteRelation {
        val g = graph(Set(A, B), Set(ArB))

        g.remove(ArB)

        assert(g.nodes == Set(A, B))
        assert(g.relations == Set())
      }

      'deleteNodeWith_OutRelation {
        val g = graph(Set(A, B), Set(ArB))

        g.remove(A)

        assert(g.nodes == Set(B))
        assert(g.relations == Set())
      }

      'deleteNodeWith_InRelation {
        val g = graph(Set(A, B), Set(ArB))

        g.remove(B)

        assert(g.nodes == Set(A))
        assert(g.relations == Set())
      }

      'deleteNodeWith_OutHyperRelation {
        val g = graph(Set(A, B, AXB), Set(ArX, XrB))

        g.remove(A)

        assert(g.nodes == Set(B))
        assert(g.relations == Set())
      }

      'deleteNodeWith_InHyperRelation {
        val g = graph(Set(A, B, AXB), Set(ArX, XrB))

        g.remove(B)

        assert(g.nodes == Set(A))
        assert(g.relations == Set())
      }

      'deleteNodeWithRelationAndHyperRelation {
        val g = graph(Set(A, B, AXB), Set(ArX, XrB, ArB))

        g.remove(B)

        assert(g.nodes == Set(A))
        assert(g.relations == Set())
      }

      'deleteHyperRelation {
        val g = graph(Set(A, B, AXB), Set(ArX, XrB))

        g.remove(AXB)

        assert(g.nodes == Set(A, B))
        assert(g.relations == Set())
      }

      'deleteHyperRelationWithRelation {
        val g = graph(Set(A, B, AXB, C), Set(ArX, XrB, CrX))

        g.remove(AXB)

        assert(g.nodes == Set(A, B, C))
        assert(g.relations == Set())
      }

      'deleteHyperRelationWithHyperRelation {
        val g = graph(Set(A, B, AXB, C, CYX), Set(ArX, XrB, CrY, YrX))

        g.remove(AXB)

        assert(g.nodes == Set(A, B, C))
        assert(g.relations == Set())
      }
      'deleteHyperRelationWithInterconnectedHyperRelations {
        val g = graph(Set(A, B, AXB, C, CYX, D, XZD), Set(ArX, XrB, CrY, YrX, XrZ, ZrD, YrZ))

        'deleteHyperRelation {
          g.remove(AXB)

          assert(g.nodes == Set(A, B, C, D))
          assert(g.relations == Set())
        }

        'deleteNode {
          g.remove(A)

          assert(g.nodes == Set(B, C, D))
          assert(g.relations == Set())
        }
      }

      'HyperSelfLoopHierarchyCollapse {
        // two nodes: A, B, hyperrelation X
        // both nodes have a hyperrelation (P, X) pointing to X (hyper half self loop)
        // A has a hyperrelation (S) to the self-loop of B
        val g = graph(Set(A, B, AXB, APX, BQX, ASQ), Set(ArX, XrB, ArP, PrX, BrQ, QrX, ArS, SrQ))

        g.remove(AXB)

        assert(g.nodes == Set(A, B))
        assert(g.relations == Set())
      }
    }

    'WrappedGraph {
      'WrapItems {
        val g = graph(Set(A, B, C, AXB), Set(ArX, XrB, ArC))
        'Graph {
          val wrapped = g.wrap("")
          assert(wrapped.nodes.map(_.id) == Set("A", "B", "C", "X"))
          assert(wrapped.relations.map(r => r.startId -> r.endId) == Set("A" -> "X", "X" -> "B", "A" -> "C"))
        }
        'HyperGraph {
          val wrapped = g.hyperWrap("")
          assert(wrapped.nodes.map(_.id) == Set("A", "B", "C", "X"))
          assert(wrapped.hyperRelations.map(r => r.startId -> r.endId) == Set("A" -> "B"))
        }
      }
      'nodeById {
        val g = graph(Set(A), Set())
        val wrapped = g.wrap("")
        assert(wrapped.nodeById("A") == wrapped.nodes.head)
      }
      'relationById {
        val g = graph(Set(A, B, C, AXB), Set(ArX, XrB, ArC))
        'Graph {
          val wrapped = g.wrap("")
          import wrapped.relationByIds
          assert(relationByIds.size == 3)

          val RArX = relationByIds("A" -> "X")
          assert(RArX.startId == "A", RArX.endId == "X")

          val RXrB = relationByIds("X" -> "B")
          assert(RXrB.startId == "X", RXrB.endId == "B")

          val RArC = relationByIds("A" -> "C")
          assert(RArC.startId == "A", RArC.endId == "C")
        }
        'HyperGraph {
          val wrapped = g.hyperWrap("")
          import wrapped.relationByIds
          assert(relationByIds.size == 1)

          val RArB = relationByIds("A" -> "B")
          assert(RArB.startId == "A", RArB.endId == "B")
        }
      }

      'WrapRawGraphChanges {
        // the new nodes/relations are already in the graph
        val g = graph(Set(A, B, C, AXB), Set(ArX, XrB, ArC))
        val rawChanges = new RawGraphChanges(Set(C, AXB), Set(ArX, XrB, ArC))

        'Graph {
          val wrapped = g.wrap("")
          import wrapped.{nodeById => nid}
          import wrapped.{relationByIds => rid}
          val changes = GraphChanges(
            Set(nid("C"), nid("X")),
            Set(rid("A" -> "X"), rid("X" -> "B"), rid("A" -> "C")))
          val wrappedChanges = wrapped.wrapRawChanges(rawChanges)
          assert(wrappedChanges == changes)
        }
        'HyperGraph {
          val wrapped = g.hyperWrap("")
          import wrapped.{nodeById => nid}
          import wrapped.{relationByIds => rid}
          val changes = GraphChanges(
            Set(nid("C"), nid("X")),
            Set(rid("A" -> "B")))
          val wrappedChanges = wrapped.wrapRawChanges(rawChanges)
          assert(wrappedChanges == changes)
        }
      }

      'CacheUpdates {
        val g = graph(Set(A, B, C, D, E, F, G, AXB), Set(ArX, XrB, ArC, DrA, ErF, CrG))
        'Graph {
          val wrapped = g.wrap("")
          // shadowing:
          val A = wrapped.nodeById("A")
          val B = wrapped.nodeById("B")
          val C = wrapped.nodeById("C")
          val D = wrapped.nodeById("D")
          val E = wrapped.nodeById("E")
          val F = wrapped.nodeById("F")
          val G = wrapped.nodeById("G")
          val X = wrapped.nodeById("X")
          val ArX = wrapped.relationByIds("A" -> "X")
          val XrB = wrapped.relationByIds("X" -> "B")
          val ArC = wrapped.relationByIds("A" -> "C")
          val DrA = wrapped.relationByIds("D" -> "A")
          val ErF = wrapped.relationByIds("E" -> "F")
          val CrG = wrapped.relationByIds("C" -> "G")
          assert(wrapped.nodes.map(_.id) == Set("A", "B", "C", "D", "E", "F", "G", "X"))
          val s = wrapped.relations.map(r => r.startId -> r.endId)
          assert(s == Set("A" -> "X", "X" -> "B", "A" -> "C", "C" -> "G", "D" -> "A", "E" -> "F"))

          assert(A.inRelations == Set(DrA))
          assert(A.outRelations == Set(ArX, ArC))
          assert(A.relations == Set(ArX, ArC, DrA))
          assert(A.predecessors == Set(D))
          assert(A.successors == Set(X, C))
          assert(A.neighbours == Set(X, C, D))
          assert(A.inDegree == 1)
          assert(A.outDegree == 2)
          assert(A.degree == 3)

          assert(A.component == Set(A, B, C, D, G, X))
          assert(F.component == Set(E, F))
          assert(A.deepSuccessors == Set(B, C, X, G))
          assert(G.deepPredecessors == Set(A, D, C))

          'AddNode {
            val newNode = n("new")
            wrapped.addNode(newNode)
            wrapped.commit()
            val wrappedNewNode = wrapped.nodeById(newNode.id)

            assert(wrapped.nodes contains wrappedNewNode)
            assert(g.nodes contains newNode)
            assert(wrappedNewNode.inRelations == Set.empty)
            assert(wrappedNewNode.outRelations == Set.empty)

            assert(A.inRelations == Set(DrA))
            assert(A.outRelations == Set(ArX, ArC))
            assert(A.relations == Set(ArX, ArC, DrA))
            assert(A.predecessors == Set(D))
            assert(A.successors == Set(X, C))
            assert(A.neighbours == Set(X, C, D))
            assert(A.inDegree == 1)
            assert(A.outDegree == 2)
            assert(A.degree == 3)

            assert(A.component == Set(A, B, C, D, G, X))
            assert(F.component == Set(E, F))
            assert(A.deepSuccessors == Set(B, C, X, G))
            assert(G.deepPredecessors == Set(A, D, C))
          }

          'AddRelation {
            wrapped.addRelation(CrE)
            wrapped.commit()
            val WCrE = wrapped.relationByIds(CrE.startId -> CrE.endId)

            assert(wrapped.relations contains WCrE)
            assert(g.relations contains CrE)
            assert(E.inRelations contains WCrE)
            assert(C.outRelations contains WCrE)

            assert(A.inRelations == Set(DrA))
            assert(A.outRelations == Set(ArX, ArC))
            assert(A.relations == Set(ArX, ArC, DrA))
            assert(A.predecessors == Set(D))
            assert(A.successors == Set(X, C))
            assert(A.neighbours == Set(X, C, D))
            assert(A.inDegree == 1)
            assert(A.outDegree == 2)
            assert(A.degree == 3)

            assert(A.component == Set(A, B, C, D, G, X, E, F))
            assert(F.component == Set(A, B, C, D, G, X, E, F))
            assert(A.deepSuccessors == Set(B, C, X, G, E, F))
            assert(G.deepPredecessors == Set(A, D, C))

            'RemoveRelation {
              wrapped.removeRelation(ArC.startId, ArC.endId)
              wrapped.commit()

              assert(wrapped.nodes.map(_.id) == Set("A", "B", "C", "D", "E", "F", "G", "X"))
              val s = wrapped.relations.map(r => r.startId -> r.endId)
              assert(s == Set("A" -> "X", "X" -> "B", "C" -> "G", "D" -> "A", "E" -> "F", "C" -> "E"))

              assert(A.inRelations == Set(DrA))
              assert(A.outRelations == Set(ArX))
              assert(A.relations == Set(ArX, DrA))
              assert(A.predecessors == Set(D))
              assert(C.predecessors == Set.empty)
              assert(A.successors == Set(X))
              assert(A.neighbours == Set(X, D))
              assert(A.inDegree == 1)
              assert(A.outDegree == 1)
              assert(A.degree == 2)

              assert(A.component == Set(A, B, X, D))
              assert(F.component == Set(G, C, E, F))
              assert(A.deepSuccessors == Set(X, B))
              assert(G.deepPredecessors == Set(C))
            }
          }
        }

        'HyperGraph {
          val wrapped = g.hyperWrap("")
          // shadowing:
          val A = wrapped.nodeById("A")
          val B = wrapped.nodeById("B")
          val C = wrapped.nodeById("C")
          val D = wrapped.nodeById("D")
          val E = wrapped.nodeById("E")
          val F = wrapped.nodeById("F")
          val G = wrapped.nodeById("G")
          val X = wrapped.nodeById("X")
          assert(wrapped.nodes.map(_.id) == Set("A", "B", "C", "D", "E", "F", "G", "X"))
          val s = wrapped.relations.map(r => r.startId -> r.endId)
          assert(s == Set("A" -> "B"))

          assert(A.inRelations == Set())
          assert(A.outRelations == Set(X))
          assert(A.relations == Set(X))
          assert(A.predecessors == Set())
          assert(A.successors == Set(B))
          assert(A.neighbours == Set(B))
          assert(A.inDegree == 0)
          assert(A.outDegree == 1)
          assert(A.degree == 1)

          assert(A.component == Set(A, B))
          assert(F.component == Set(F))
          assert(A.deepSuccessors == Set(B))
          assert(G.deepPredecessors == Set())

          'AddNodeFromRawGraph {
            val newNode = n("new")
            wrapped.add(newNode)
            val fu = g.currentNewNodes
            val ck = g.currentNewRelations
            assert(fu == Set(newNode))
            assert(ck == Set.empty)
            wrapped.commit()
            val wrappedNewNode = wrapped.nodeById(newNode.id)

            assert(wrapped.nodes contains wrappedNewNode)
            assert(g.nodes contains newNode)
            assert(wrappedNewNode.inRelations == Set.empty)
            assert(wrappedNewNode.outRelations == Set.empty)

            assert(A.inRelations == Set())
            assert(A.outRelations == Set(X))
            assert(A.relations == Set(X))
            assert(A.predecessors == Set())
            assert(A.successors == Set(B))
            assert(A.neighbours == Set(B))
            assert(A.inDegree == 0)
            assert(A.outDegree == 1)
            assert(A.degree == 1)

            assert(A.component == Set(A, B))
            assert(F.component == Set(F))
            assert(A.deepSuccessors == Set(B))
            assert(G.deepPredecessors == Set())
          }

          'AddHyperRelation {
            wrapped.add(AUC)
            val fu = g.currentNewNodes
            val ck = g.currentNewRelations
            assert(fu == Set(AUC))
            assert(ck == Set(new RawRelation("A", "U"), new RawRelation("U", "C")))
            wrapped.commit()
            val WAUC = wrapped.relationByIds(AUC.startId.get -> AUC.endId.get)
            assert(wrapped.relations contains WAUC)

            val U = wrapped.nodeById("U")

            assert(A.inRelations == Set())
            assert(A.outRelations == Set(X, U))
            assert(A.relations == Set(X, U))
            assert(A.predecessors == Set())
            assert(A.successors == Set(B, C))
            assert(A.neighbours == Set(B, C))
            assert(A.inDegree == 0)
            assert(A.outDegree == 2)
            assert(A.degree == 2)

            assert(A.component == Set(A, B, C))
            assert(F.component == Set(F))
            assert(A.deepSuccessors == Set(B, C))
            assert(G.deepPredecessors == Set())

            'RemoveHyperRelation {
              wrapped.removeNode("U")
              wrapped.commit()

              assert(wrapped.nodes.map(_.id) == Set("A", "B", "C", "D", "E", "F", "G", "X"))
              val s = wrapped.relations.map(r => r.startId -> r.endId)
              assert(s == Set("A" -> "B"))

              assert(A.inRelations == Set())
              assert(A.outRelations == Set(X))
              assert(A.relations == Set(X))
              assert(A.predecessors == Set())
              assert(A.successors == Set(B))
              assert(A.neighbours == Set(B))
              assert(A.inDegree == 0)
              assert(A.outDegree == 1)
              assert(A.degree == 1)

              assert(A.component == Set(A, B))
              assert(F.component == Set(F))
              assert(A.deepSuccessors == Set(B))
              assert(G.deepPredecessors == Set())
            }
          }
          'AddHyperRelationAndNode {
            val newNode = n("new")
            val newRel = h("new", "muh", "A")
            wrapped.add(newNode)
            wrapped.add(newRel)
            val fu = g.currentNewNodes
            val ck = g.currentNewRelations
            assert(fu == Set(newRel, newNode))
            assert(ck == Set(new RawRelation("new", "muh"), new RawRelation("muh", "A")))
            wrapped.commit()
            val wrappedRel = wrapped.relationByIds("new" -> "A")
            val wrappedNode = wrapped.nodeById("new")
            assert(wrapped.relations contains wrappedRel)
            assert(wrapped.nodes contains wrappedNode)

            assert(A.inRelations == Set(wrappedRel))
            assert(A.outRelations == Set(X))
            assert(A.relations == Set(X, wrappedRel))
            assert(A.predecessors == Set(wrappedNode))
            assert(A.successors == Set(B))
            assert(A.neighbours == Set(B, wrappedNode))
            assert(A.inDegree == 1)
            assert(A.outDegree == 1)
            assert(A.degree == 2)

            assert(A.component == Set(A, B, wrappedNode))
            assert(F.component == Set(F))
            assert(A.deepSuccessors == Set(B))
            assert(A.deepPredecessors == Set(wrappedNode))

            'RemoveHyperRelation {
              wrapped.removeNode("muh")
              wrapped.commit()

              val nodeids = wrapped.nodes.map(_.id)
              assert(nodeids == Set("A", "B", "C", "D", "E", "F", "G", "X", "new"))
              val s = wrapped.relations.map(r => r.startId -> r.endId)
              assert(s == Set("A" -> "B"))

              assert(A.inRelations == Set())
              assert(A.outRelations == Set(X))
              assert(A.relations == Set(X))
              assert(A.predecessors == Set())
              assert(A.successors == Set(B))
              assert(A.neighbours == Set(B))
              assert(A.inDegree == 0)
              assert(A.outDegree == 1)
              assert(A.degree == 1)

              assert(A.component == Set(A, B))
              assert(F.component == Set(F))
              assert(A.deepSuccessors == Set(B))
              assert(G.deepPredecessors == Set())
            }
          }
        }
      }
    }
  }
}

object TestHelpers {
  
  implicit def n(id: String) = new RawNode(id = id, title = "", description = None, isHyperRelation = false, startId = None, endId = None, tags = js.Array(), timestamp = 0, voteCount = 0, viewCount = 0)
  implicit def r(t: (String, String)) = new RawRelation(t._1, t._2)
implicit def h(startId: String, id: String, endId: String) = new RawNode(id = id, title = "", description = None, isHyperRelation = true, startId = Some(startId), endId = Some(endId), tags = js.Array(), timestamp = 0, voteCount = 0, viewCount = 0)
  def graph(nodes: Set[RawNode], relations: Set[RawRelation]) = new RawGraph(nodes, relations, nodes.head.id)

  val A = n("A")
  val B = n("B")
  val C = n("C")
  val D = n("D")
  val E = n("E")
  val F = n("F")
  val G = n("G")
  val ArB = r("A" -> "B")
  val ArC = r("A" -> "C")
  val DrA = r("D" -> "A")
  val ErF = r("E" -> "F")
  val CrG = r("C" -> "G")
  val CrE = r("C" -> "E")

  val AXB = h("A", "X", "B")
  val ArX = r("A" -> "X")
  val XrB = r("X" -> "B")

  val AUC = h("A", "U", "C")
  val ArU = r("A" -> "U")
  val UrC = r("U" -> "C")

  val CrX = r("C" -> "X")

  val CYX = h("C", "Y", "X")
  val CrY = r("C" -> "Y")
  val YrX = r("Y" -> "X")

  val XZD = h("X", "Z", "D")
  val XrZ = r("X" -> "Z")
  val ZrD = r("Z" -> "D")

  val YrZ = r("Y" -> "Z")

  val APX = h("A", "P", "X")
  val ArP = r("A" -> "P")
  val PrX = r("P" -> "X")

  val BQX = h("B", "Q", "X")
  val BrQ = r("B" -> "Q")
  val QrX = r("Q" -> "X")

  val ASQ = h("A", "S", "Q")
  val ArS = r("A" -> "S")
  val SrQ = r("S" -> "Q")
}
