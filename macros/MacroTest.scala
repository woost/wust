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
        val nodeDefs: List[(String, String, String, String, List[String], List[(String, String, String)], List[(String, String, String)])] = rawNodeDefs.map {
          case q"(${className: String},${plural: String},${label: String}, ${factory: String}, List(..$traits), List(..$neighboursChains), List(..$revNeighboursChains))" =>
            (className, plural, label, factory,
              traits.map { case q"${traitName: String}" => traitName },
              neighboursChains.map { case q"(${name: String}, ${rel1: String}, ${rel2: String})" => (name, rel1, rel2) },
              revNeighboursChains.map { case q"(${name: String}, ${rel1: String}, ${rel2: String})" => (name, rel1, rel2) })
        }
        val nodePlurals: Map[String, String] = nodeDefs.map { case (className, plural, _, _, _, _, _) => className -> plural }.toMap

        val relationDefs: List[(String, String, String, String, String)] = rawRelationDefs.map {
          case q"(${name: String}, ${plural: String}, ${relationType: String}, ${startNode: String}, ${endNode: String})" =>
            (name, plural, relationType, startNode, endNode)
        }
        val relationPlurals: Map[String, String] = relationDefs.map { case (className, plural, _, _, _) => className -> plural }.toMap
        val relationStarts: Map[String, String] = relationDefs.map { case (className, _, _, startNode, _) => className -> startNode }.toMap
        val relationEnds: Map[String, String] = relationDefs.map { case (className, _, _, _, endNode) => className -> endNode }.toMap

        def rev(s: String) = "rev_" + s


        val (nodeFactories, nodeClasses, nodeSets) = nodeDefs.map {
          //  ("Goal", "goals", "GOAL", "ContentNodeFactory", List("ContentNode"), List("Reaches"), List(List("Reaches", "Idea"))),
          case (className, plural, label, factory, traits, neighboursChains, revNeighboursChains) =>
            val traitsTypes = traits.map { TypeName(_) }
            val directNeighbours = relationDefs.collect {
              case (relationName, relationPlural, _, `className`, endNode) =>
                q"""def ${ TermName(relationPlurals(relationName)) }:Set[${ TypeName(endNode) }] = successorsAs(${ TermName(endNode) })"""
            }
            val directRevNeighbours = relationDefs.collect {
              case (relationName, relationPlural, _, startNode, `className`) =>
                q"""def ${ TermName(rev(relationPlurals(relationName))) }:Set[${ TypeName(startNode) }] = predecessorsAs(${ TermName(startNode) })"""
            }
            val indirectNeighbours = neighboursChains.map { case (name, rel1, rel2) =>
              q"""def ${ TermName(name) }:Set[${ TypeName(relationEnds(rel2)) }] = ${ TermName(relationPlurals(rel1)) }.flatMap(_.${ TermName(relationPlurals(rel2)) })"""
            }
            val indirectRevNeighbours = revNeighboursChains.map { case (name, rel1, rel2) =>
              q"""def ${ TermName(name) }:Set[${ TypeName(relationStarts(rel1)) }] = ${ TermName(rev(relationPlurals(rel2))) }.flatMap(_.${ TermName(rev(relationPlurals(rel1))) })"""
            }
            ( q"""

            object ${ TermName(className) } extends ${ TypeName(factory) }[${ TypeName(className) }] {
                def create(node: Node) = new ${ TypeName(className) }(node)
                val label = Label($label)
            }

            """, q"""

            case class ${ TypeName(className) }(node: Node) extends ..$traitsTypes {
              ..$directNeighbours
              ..$directRevNeighbours
              ..$indirectNeighbours
              ..$indirectRevNeighbours
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
