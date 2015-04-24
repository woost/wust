package modules.json

import collection.mutable
import modules.requests._
import play.api.libs.json._
import renesca.graph.Graph
import renesca.graph._
import renesca.parameter.implicits._
import renesca.parameter.PropertyKey._
import renesca.parameter.StringPropertyValue
import model._
import model.WustSchema._
import renesca.schema._

object GraphFormat {
  implicit def LabelToString(label: Label): String = label.name
  implicit def RelationTypeToString(relationType: RelationType): String = relationType.name

  implicit object DiscourseRelationFormat extends Format[SchemaRelation[ContentNode, ContentNode]] {
    def reads(json: JsValue) = ???

    def writes(relation: SchemaRelation[ContentNode, ContentNode]) = JsObject(Seq(
      ("label", JsString(relation.relationType)),
      ("from", JsString(relation.startNode.uuid)),
      ("to", JsString(relation.endNode.uuid))
    ))
  }

  implicit object ContentNodeFormat extends Format[ContentNode] {
    def reads(json: JsValue) = ???

    def writes(node: ContentNode) = JsObject(Seq(
      ("id", JsString(node.uuid)),
      ("title", JsString(node.title)),
      ("label", JsString(node.label))
    ))
  }

  implicit object DiscourseFormat extends Format[Discourse] {
    def reads(json: JsValue) = ???

    def writes(discourseGraph: Discourse) = {
      implicit val newGraph = Graph(
        discourseGraph.graph.nodes,
        discourseGraph.graph.relations
      )
      val newDiscourseGraph = Discourse(newGraph)

      case class Replacement(deleteNode: Node, deleteRelations: Iterable[Relation], newRelation: Option[Relation] = None)
      val simplify: PartialFunction[SchemaHyperRelation[ContentNode, _ <: SchemaRelation[ContentNode, _], _ <: SchemaHyperRelation[ContentNode, _, _, _, ContentNode] with UuidNode, _ <: SchemaRelation[_, ContentNode], ContentNode] with UuidNode, Replacement] = {
        case Solves(node) if node.inDegree == 1 && node.outDegree == 1   =>
          Replacement(node, node.relations, Some(Relation.local(node.predecessors.head, node.successors.head, "SOLVES")))
        case Achieves(node) if node.inDegree == 1 && node.outDegree == 1 =>
          Replacement(node, node.relations, Some(Relation.local(node.predecessors.head, node.successors.head, "REACHES")))
        case Solves(node) if node.degree < 2                             =>
          Replacement(node, node.relations)
        case Achieves(node) if node.degree < 2                           =>
          Replacement(node, node.relations)
      }

      val fakeRelations = mutable.ArrayBuffer.empty[SchemaRelation[ContentNode, ContentNode]]

      var next: Option[Replacement] = None
      while( { next = newDiscourseGraph.contentNodeHyperRelations.collectFirst(simplify); next.isDefined }) {
        val replacement = next.get
        import replacement._
        newGraph.nodes -= deleteNode
        newGraph.relations --= deleteRelations
        fakeRelations ++= newRelation.map(newRel => new SchemaRelation[ContentNode, ContentNode] {
          val relation = newRel
          val startNode = new ContentNode {val node = relation.startNode }
          val endNode = new ContentNode {val node = relation.endNode }
        })
      }

      JsObject(Seq(
        ("nodes", Json.toJson(newDiscourseGraph.contentNodes)),
        ("edges", Json.toJson(newDiscourseGraph.contentNodeRelations ++ fakeRelations))
      ))
    }
  }

  implicit object NodeAddFormat extends Format[NodeAddRequest] {
    def reads(json: JsValue) = json match {
      case JsObject(_) => {
        JsSuccess(NodeAddRequest((json \ "title").as[String]))
      }
      case otherwise   => JsError()
    }

    def writes(nodeAdd: NodeAddRequest) = ???
  }

  implicit object ConnectFormat extends Format[ConnectRequest] {
    def reads(json: JsValue) = json match {
      case JsObject(_) => {
        JsSuccess(ConnectRequest((json \ "id").as[String]))
      }
      case otherwise   => JsError()
    }

    def writes(connect: ConnectRequest) = ???
  }

  implicit def nodeSchemaWrites[NODE <: ContentNode] = new Writes[NodeSchema[NODE]] {
    def writes(schema: NodeSchema[NODE]) = JsObject(Seq(
      ("label", JsString(schema.factory.label)),
      ("path", JsString(schema.path)),
      ("name", JsString(schema.name)),
      ("subs", JsObject(schema.connectSchemas.map {
        case (k, v: ConnectSchema[NODE]) => (k, JsObject(Seq(
          ("cardinality", JsString(v.cardinality))
        )))
      }.toList))
  ))
  }
}
