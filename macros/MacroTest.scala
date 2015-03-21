package macros

import scala.reflect.macros.whitebox.Context
import scala.language.experimental.macros
import scala.annotation.StaticAnnotation
import scala.reflect.api._

object GraphSchemaMacro {
  // TODO impicits for TypeName and TermName
  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._
    import Flag._

    val result = {
      annottees.map(_.tree).toList match {
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
          val nodes = nodedef.map{
            case pq"""(${name:String},${label:String})""" => //TODO working only with q?
                q"""
                object ${TermName(name)} extends ContentNodeFactory[${TypeName(name)}] {
                    def create(node: Node) = new ${TypeName(name)}(node)
                    val label = Label($label)
                }
                """

 }
          val relationFactories = relationdef.map{
            case pq"""(${name:String}, ${plural:String}, ${relationtype:String}, ${fromNode:String}, ${toNode:String})""" => //TODO rename from/to -> start/end
                q"""
                object ${TermName(name)} extends SchemaRelationFactory[${TypeName(name)}, ${TypeName(fromNode)}, ${TypeName(toNode)}] {
                    def create(relation: Relation) = ${TermName(name)}(relation, startNodeFactory.create(relation.startNode), endNodeFactory.create(relation.endNode))
                    def relationType = RelationType($relationtype)
                    def startNodeFactory = ${TermName(fromNode)}
                    def endNodeFactory = ${TermName(toNode)}
                }"""
          }

          val relationClasses = relationdef.map{
            case pq"""(${name:String}, ${plural:String}, ${relationtype:String}, ${fromNode:String}, ${toNode:String})""" => //TODO rename from/to -> start/end
                q"""
                case class ${TypeName(name)}(relation: Relation, startNode: ${TypeName(fromNode)}, endNode: ${TypeName(toNode)}) extends DiscourseRelation[${TypeName(fromNode)}, ${TypeName(toNode)}]
                """
          }
          val relationSets = relationdef.map{
            case pq"""(${name:String}, ${plural:String}, ${relationtype:String}, ${fromNode:String}, ${toNode:String})""" => //TODO rename from/to -> start/end
                q"""
                def ${TermName(plural)}: Set[${TypeName(name)}] = relationsAs(${TermName(name)})
                """
          }
          // val plurals = relationdef.map{case pq"""(_,${plural:String},_,_,_)""" => plural}
          q"""
object $name extends ..$parents {
    ..$nodes
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
      }
    }
    c.Expr[Any](result)
  }
}
class GraphSchema extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro GraphSchemaMacro.impl
}
