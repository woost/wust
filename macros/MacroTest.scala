package macros

import scala.reflect.macros.whitebox.Context
import scala.language.experimental.macros
import scala.annotation.StaticAnnotation

object GraphSchemaMacro {
  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    c.Expr[Any](annottees.map(_.tree).toList match {
      case q"""
    object $name extends ..$parents {
        val nodes = List(..$nodedef)
        val relations = List(..$relationdef)

        ..$body

        case class Discourse($discourseParams) extends $discourseParents {
            ..$discoursebody
        }

    }
    """ :: Nil =>

        val nodePlurals: Map[String, String] = nodedef.map { case q"""(${className: String},${plural: String},$_, $_, List(..$_), List(..$_), List(..$_))""" => className -> plural }.toMap
        val (nodeFactories, nodeClasses, nodeSets) = nodedef.map {
          //  ("Goal", "goals", "GOAL", "ContentNodeFactory", List("ContentNode"), List("Reaches"), List(List("Reaches", "Idea"))),
          case q"""(${className: String},${plural: String},${label: String}, ${factory: String}, List(..$traits), List(..$neighbours), List(..$neighboursChain))""" =>
            val withTraits = traits.map { case q"""${traitName: String}""" => TypeName(traitName) }
            val directNeighbours = neighbours.map {
              case q"""${nodeName: String}""" =>
                q"""def ${ TermName(nodePlurals(nodeName)) }:Set[${ TypeName(nodeName) }] = neighboursAs(${ TermName(nodeName) })"""
            }
            val indirectNeighbours = neighboursChain.map {
              case q"""(${over: String}, ${nodeName: String})""" =>
                q"""def ${ TermName(nodePlurals(nodeName)) }:Set[${ TypeName(nodeName) }] = ${ TermName(nodePlurals(over)) }.flatMap(_.${ TermName(nodePlurals(nodeName)) })"""
            }
            ( q"""

            object ${ TermName(className) } extends ${ TypeName(factory) }[${ TypeName(className) }] {
                def create(node: Node) = new ${ TypeName(className) }(node)
                val label = Label($label)
            }

            """, q"""

            case class ${ TypeName(className) }(node: Node) extends ..$withTraits {
              ..$directNeighbours
              ..$indirectNeighbours
            }

            """, q"""

            def ${ TermName(plural) }: Set[${ TypeName(className) }] = nodesAs(${ TermName(className) })

            """)
        }.unzip3

        val (relationFactories, relationClasses, relationSets) = relationdef.map {
          case q"""(${name: String}, ${plural: String}, ${relationtype: String},
                      ${startNode: String}, ${endNode: String})""" =>
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
