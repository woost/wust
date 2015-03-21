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

        val nodeFactories = nodedef.map{
            case q"""(${name:String},${label:String})""" =>
            q"""
            object ${TermName(name)} extends ContentNodeFactory[${TypeName(name)}] {
                def create(node: Node) = new ${TypeName(name)}(node)
                val label = Label($label)
            }
            """
        }

        val (relationFactories, relationClasses, relationSets) = relationdef.map{
            case q"""(${name:String}, ${plural:String}, ${relationtype:String},
                      ${startNode:String}, ${endNode:String})""" =>
            (q"""

            object ${TermName(name)} extends SchemaRelationFactory[${TypeName(name)}, ${TypeName(startNode)}, ${TypeName(endNode)}] {
                def create(relation: Relation) = ${TermName(name)}(relation,
                  startNodeFactory.create(relation.startNode),
                  endNodeFactory.create(relation.endNode))
                def relationType = RelationType($relationtype)
                def startNodeFactory = ${TermName(startNode)}
                def endNodeFactory = ${TermName(endNode)}
            }

            """,q"""

            case class ${TypeName(name)}(relation: Relation, startNode: ${TypeName(startNode)}, endNode: ${TypeName(endNode)})
                       extends DiscourseRelation[${TypeName(startNode)}, ${TypeName(endNode)}]

            """, q"""

            def ${TermName(plural)}: Set[${TypeName(name)}] = relationsAs(${TermName(name)})

            """)

        }.unzip3

        // val plurals = relationdef.map{case pq"""(_,${plural:String},_,_,_)""" => plural}


        q"""
        object $name extends ..$parents {
            ..$nodeFactories
            ..$relationFactories
            ..$relationClasses

            ..$body

            object Discourse {def empty = new Discourse(Graph.empty) }

            case class Discourse($discourseParams) extends $discourseParents {
                ..$relationSets

                ..$discoursebody
            }

        }
        """
    })
  }
}
class GraphSchema extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro GraphSchemaMacro.impl
}
