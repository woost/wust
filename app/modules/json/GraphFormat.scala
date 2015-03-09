package modules.json

import collection.mutable
import modules.requests.{GoalAddRequest, IdeaAddRequest, NodeAddRequest, ProblemAddRequest}
import play.api.libs.json._
import renesca.graph.Graph
import renesca.graph._
import renesca.parameter.implicits._
import renesca.parameter.PropertyKey._
import renesca.parameter.StringPropertyValue
import model._

object GraphFormat {
  implicit def LabelToString(label: Label): String = label.name
  implicit def RelationTypeToString(relationType: RelationType): String = relationType.name

  implicit object DiscourseRelationFormat extends Format[DiscourseRelation[DiscourseNode, DiscourseNode]] {
    def reads(json: JsValue) = ???

    def writes(relation: DiscourseRelation[DiscourseNode, DiscourseNode]) = JsObject(Seq(
      ("label", JsString(relation.relationType)),
      ("from", JsString(relation.startNode.uuid)),
      ("to", JsString(relation.endNode.uuid))
    ))
  }

  implicit object DiscourseNodeFormat extends Format[DiscourseNode] {
    def reads(json: JsValue) = ???

    def writes(node: DiscourseNode) = JsObject(Seq(
      ("id", JsString(node.uuid)),
      ("title", JsString(node.title)),
      ("label", JsString(node.label))
    ))
  }

  implicit object DiscourseFormat extends Format[Discourse] {
    def reads(json: JsValue) = ???

    def writes(discourseGraph: Discourse) = {
      implicit val newGraph = Graph(
        discourseGraph.nodes.map(_.node),
        discourseGraph.relations.map(_.relation)
      )
      val newDiscourseGraph = Discourse(newGraph)

      case class Replacement(deleteNode: Node, deleteRelations: Iterable[Relation], newRelation: Option[Relation] = None)
      val simplify: PartialFunction[DiscourseNode, Replacement] = {
        case Solves(node) if node.inDegree == 1 && node.outDegree == 1   =>
          Replacement(node, node.relations, Some(Relation.local(node.predecessors.head, node.successors.head, "SOLVES")))
        case Reaches(node) if node.inDegree == 1 && node.outDegree == 1   =>
          Replacement(node, node.relations, Some(Relation.local(node.predecessors.head, node.successors.head, "REACHES")))
        case Solves(node) if node.degree < 2 =>
          Replacement(node, node.relations)
        case Reaches(node) if node.degree < 2 =>
          Replacement(node, node.relations)
      }

      var next: Option[Replacement] = discourseGraph.nodes.collectFirst(simplify)
      while( { next = newDiscourseGraph.nodes.collectFirst(simplify); next.isDefined }) {
        val replacement = next.get
        import replacement._
        newGraph.nodes -= deleteNode
        newGraph.relations --= deleteRelations
        for (relation <- newRelation)
          newGraph.relations += relation
      }

      JsObject(Seq(
        ("nodes", Json.toJson(newDiscourseGraph.nodes)),
        ("edges", Json.toJson(newDiscourseGraph.relations))
      ))
    }
  }

  implicit object ProblemAddFormat extends Format[ProblemAddRequest] {
    def reads(json: JsValue) = json match {
      case JsObject(_) => {
        JsSuccess(ProblemAddRequest((json \ "title").as[String]))
      }
      case otherwise   => JsError()
    }

    def writes(problemAdd: ProblemAddRequest) = ???
  }

  implicit object GoalAddFormat extends Format[GoalAddRequest] {
    def reads(json: JsValue) = json match {
      case JsObject(_) => {
        JsSuccess(GoalAddRequest((json \ "title").as[String]))
      }
      case otherwise   => JsError()
    }

    def writes(problemAdd: GoalAddRequest) = ???
  }

  implicit object IdeaAddFormat extends Format[IdeaAddRequest] {
    def reads(json: JsValue) = json match {
      case JsObject(_) => {
        JsSuccess(IdeaAddRequest((json \ "title").as[String]))
      }
      case otherwise   => JsError()
    }

    def writes(problemAdd: IdeaAddRequest) = ???
  }
}
