package macros

import scala.reflect.macros.whitebox
import scala.language.experimental.macros
import scala.annotation.StaticAnnotation

object GraphSchemaMacro {
  // TODO: why are implicits not working here?
  // implicit def treeToString(l: Tree): String = l match { case Literal(Constant(string: String)) => string }
  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    c.Expr[Any](annottees.map(_.tree).toList match {
      case q"""
    object $name extends ..$parents {
        val nodes = List(..$rawNodeDefs)
        val relations = List(..$rawRelationDefs)

        ..$body

        case class Discourse($discourseParams) extends $discourseParents {
            ..$discoursebody
        }

    }
    """ :: Nil =>
        val nodeDefs: List[(String, String, String, String, List[String], List[String], List[(String, String)])] = rawNodeDefs.map {
          case q"(${className: String},${plural: String},${label: String}, ${factory: String}, List(..$traits), List(..$neighbours), List(..$neighboursChains))" =>
            (className, plural, label, factory,
              traits.map { case q"${traitName: String}" => traitName },
              neighbours.map { case q"${neighbour: String}" => neighbour },
              neighboursChains.map { case q"(${over: String}, ${nodeName: String})" => (over, nodeName) })
        }

        val relationDefs: List[(String, String, String, String, String)] = rawRelationDefs.map {
          case q"""(${name: String}, ${plural: String}, ${relationType: String}, ${startNode: String}, ${endNode: String})""" =>
            (name, plural, relationType, startNode, endNode)
        }

        val nodePlurals: Map[String, String] = nodeDefs.map { case (className, plural, _, _, _, _, _) => className -> plural }.toMap
        val (nodeFactories, nodeClasses, nodeSets) = nodeDefs.map {
          //  ("Goal", "goals", "GOAL", "ContentNodeFactory", List("ContentNode"), List("Reaches"), List(List("Reaches", "Idea"))),
          case (className, plural, label, factory, traits, neighbours, neighboursChain) =>
            val traitsTypes = traits.map { TypeName(_) }
            val directNeighbours = neighbours.map { nodeName =>
              q"""def ${ TermName(nodePlurals(nodeName)) }:Set[${ TypeName(nodeName) }] = neighboursAs(${ TermName(nodeName) })"""
            }
            val indirectNeighbours = neighboursChain.map { case (over, nodeName) =>
              q"""def ${ TermName(nodePlurals(nodeName)) }:Set[${ TypeName(nodeName) }] = ${ TermName(nodePlurals(over)) }.flatMap(_.${ TermName(nodePlurals(nodeName)) })"""
            }
            ( q"""

            object ${ TermName(className) } extends ${ TypeName(factory) }[${ TypeName(className) }] {
                def create(node: Node) = new ${ TypeName(className) }(node)
                val label = Label($label)
            }

            """, q"""

            case class ${ TypeName(className) }(node: Node) extends ..$traitsTypes {
              ..$directNeighbours
              ..$indirectNeighbours
            }

            """, q"""

            def ${ TermName(plural) }: Set[${ TypeName(className) }] = nodesAs(${ TermName(className) })

            """)
        }.unzip3

        val (relationFactories, relationClasses, relationSets) = relationDefs.map {
          case (name, plural, relationtype, startNode, endNode) =>
            ( q"""

            object ${ TermName(name) } extends SchemaRelationFactory[${ TypeName(name) }, ${ TypeName(startNode) }, ${ TypeName(endNode) }] {
                def create(relation: Relation) = ${ TermName(name) }(relation,
                  startNodeFactory.create(relation.startNode),
                  endNodeFactory.create(relation.endNode))
                def relationType = RelationType($relationtype)
                def startNodeFactory = ${ TermName(startNode) }
                def endNodeFactory = ${ TermName(endNode) }
            }

            """, q"""

            case class ${ TypeName(name) }(relation: Relation, startNode: ${ TypeName(startNode) }, endNode: ${ TypeName(endNode) })
                       extends DiscourseRelation[${ TypeName(startNode) }, ${ TypeName(endNode) }]

            """, q"""

            def ${ TermName(plural) }: Set[${ TypeName(name) }] = relationsAs(${ TermName(name) })

            """)

        }.unzip3


        q"""
        object $name extends ..$parents {
            ..$nodeFactories
            ..$nodeClasses
            ..$relationFactories
            ..$relationClasses

            ..$body

            object Discourse {def empty = new Discourse(Graph.empty) }

            case class Discourse($discourseParams) extends $discourseParents {
                ..$nodeSets
                ..$relationSets

                ..$discoursebody
            }

        }
        """
    })
  }
}
class GraphSchema extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro GraphSchemaMacro.impl
}
