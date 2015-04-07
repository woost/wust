package macros

import scala.reflect.macros.whitebox
import collection.mutable
import scala.language.experimental.macros
import PartialFunction._
import scala.annotation.StaticAnnotation

class GraphSchema extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro GraphSchemaMacro.graphSchema
}

object GraphSchemaMacro {
  // TODO: why are implicits not working here?
  // implicit def treeToString(l: Tree): String = l match { case Literal(Constant(string: String)) => string }
  // TODO: validation: nodeTraits(propertyTypes), nodes need to inherit exactly one NodeTrait

  def graphSchema(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._


    // TODO: Expose everything as strings
    object SchemaPattern {
      def unapply(tree: Tree) = condOpt(tree) {
        case q""" object $schemaName extends ..$schemaParents { ..$schemaStatements } """ =>
          (schemaName, schemaParents, schemaStatements)
      }
    }

    object InnerSchemaPattern {
      def unapply(tree: Tree) = condOpt(tree) {
        case q""" @Schema class $innerName extends ${_} { def nodes:Set[$superNodeTrait] }""" =>
          (innerName, superNodeTrait)
      }
    }
        
    object NodeTraitPattern {
      def unapply(tree: Tree) = condOpt(tree) {
        //http://stackoverflow.com/questions/26305528/scala-annotations-are-not-found
        case q""" $mods trait $traitName extends ..$traitParents { ..$traitBody } """ if mods.annotations.collectFirst {
            case Apply(Select(New(Ident(TypeName("Node"))), termNames.CONSTRUCTOR), Nil) => true
            case _ => false
        }.get =>
          (traitName, traitParents.map {_.toString}, traitBody)
      }
    }

    object RelationPattern {
      def unapply(tree: Tree) = condOpt(tree) {
        case q"""@Relation class $className (startNode:$startNode, endNode:$endNode) {..$relationBody}""" =>
          (className.toString, startNode.toString, endNode.toString, relationBody)
      }
    }

    object NodePattern {
      def unapply(tree: Tree) = condOpt(tree) {
        case q"""@Node class $className extends ${parentTrait:TypeName} { ..$nodeStatements }""" =>
          (className.toString, parentTrait, nodeStatements)
      }
    }




    c.Expr[Any](annottees.map(_.tree).toList match {
      case SchemaPattern(schemaName, schemaParents, schemaStatements) :: Nil =>

        val (innerSchemaName,superNodeTrait) = schemaStatements.collectFirst { case InnerSchemaPattern(innerName, superNodeTrait) => (innerName.toString, superNodeTrait.toString) }.get

        val nodeTypeToFactoryType: Map[TypeName, Tree] = schemaStatements.collect {
          case NodeTraitPattern(traitName, _, traitBody) => 
            traitBody.collect {
            case q"def factory:$factoryType" => traitName -> factoryType
          }
        }.flatten.toMap

    def nameToPlural(name: String) = {
      val lower = name.take(1).toLowerCase + name.drop(1)
      val suffix = if (lower.endsWith("s")) "" else "s"
      lower + suffix;
    }

    def nameToLabel(name: String) = name.toUpperCase

    val nodes: Seq[String] = schemaStatements.collect { case NodePattern( className, _, _) => className }
    val relationStarts: Map[String, String] = schemaStatements.collect { case RelationPattern(className,startNode,_,_) => className -> startNode }.toMap
    val relationEnds: Map[String, String] = schemaStatements.collect { case RelationPattern(className,_,endNode,_) => className -> endNode }.toMap
    val allNodes = nodes.foldLeft[Tree](q"Set.empty") { case (q"$all", name) => q"$all ++ ${ TermName(nameToPlural(name)) }" }
    val allRelations = relationStarts.keys.foldLeft[Tree](q"Set.empty") { case (q"$all", name) => q"$all ++ ${ TermName(nameToPlural(name)) }" }

        val nodeTraits: List[Tree] = schemaStatements.collect {
          // has side effects on nodeTypeToFactory mapping
          case NodeTraitPattern(traitName, traitParents, traitStatements) =>
            def getter(name: String, typeName: Tree) =
              q""" def ${TermName(name)}:$typeName = node.properties(${name}).asInstanceOf[${TypeName(typeName.toString + "PropertyValue")}] """
            def setter(name: String, typeName: Tree) =
              q""" def ${TermName(name + "_$eq")}(newValue:$typeName){ node.properties(${name}) = newValue} """
            val traitBody = traitStatements.flatMap {
              case q"val $propertyName:$propertyType" => List(getter(propertyName.toString, propertyType))
              case q"var $propertyName:$propertyType" => List(getter(propertyName.toString, propertyType), setter(propertyName.toString, propertyType))
              case q"def factory:$factoryType" => Nil
              case somethingElse => List(somethingElse)
            }
            val traitParentsWithSchemaNode = (if(traitParents.nonEmpty && traitParents != List("scala.AnyRef")) traitParents else List("SchemaNode")).map(TypeName(_))

            q""" trait $traitName extends ..$traitParentsWithSchemaNode { ..$traitBody } """
        }

        val relationFactories: List[Tree] = schemaStatements.collect {
          case RelationPattern( className, startNode, endNode, relationBody) =>
            q"""
           object ${TermName(className)} extends SchemaRelationFactory[${TypeName(className)}, ${TypeName(startNode)}, ${TypeName(endNode)}] {
               def create(relation: Relation) = ${TermName(className)}(relation,
                 startNodeFactory.create(relation.startNode),
                 endNodeFactory.create(relation.endNode))
               def relationType = RelationType(${nameToLabel(className)})
               def startNodeFactory = ${TermName(startNode)}
               def endNodeFactory = ${TermName(endNode)}
           }
           """
        }

        val relationClasses: List[Tree] = schemaStatements.collect {
          case RelationPattern( className, startNode, endNode, relationBody) =>
            q"""
           case class ${TypeName(className)}(relation: Relation, startNode: ${TypeName(startNode)}, endNode: ${TypeName(endNode)})
             extends SchemaRelation[${TypeName(startNode)}, ${TypeName(endNode)}] {
             ..$relationBody
             }
           """
        }

        val relationSets: List[Tree] = schemaStatements.collect {
          case RelationPattern( className, startNode, endNode, relationBody) =>
            q""" def ${TermName(nameToPlural(className))}: Set[${TypeName(className)}] = relationsAs(${TermName(className)}) """
        }

        val nodeFactories: List[Tree] = schemaStatements.collect {
          case NodePattern( className, parentTrait, nodeStatements) =>
           q"""
           object ${TermName(className)} extends ${nodeTypeToFactoryType(parentTrait)}[${TypeName(className)}] { def create(node: Node) = new ${TypeName(className)}(node)
               val label = Label(${nameToLabel(className)})
           }
           """
        }
        val nodeClasses: List[Tree] = schemaStatements.collect {
          case NodePattern( className, parentTrait, nodeStatements) =>
            def rev(s: String) = "rev_" + s
            val directNeighbours = schemaStatements.collect {
              case RelationPattern(relationName, `className`, endNode, _) =>
                q"""def ${TermName(nameToPlural(relationName))}:Set[${TypeName(endNode)}] = successorsAs(${TermName(endNode)})"""
            }
            val directRevNeighbours = schemaStatements.collect {
              case RelationPattern( relationName, startNode, `className`, _) =>
                q"""def ${TermName(rev(nameToPlural(relationName)))}:Set[${TypeName(startNode)}] = predecessorsAs(${TermName(startNode)})"""
            }

            val nodeBody = nodeStatements.map{
              case q"""def $chainName = $rel1 --> $rel2""" => 
                q"""def $chainName:Set[${TypeName(relationEnds(rel2.toString))}] = ${TermName(nameToPlural(rel1.toString))}.flatMap(_.${TermName(nameToPlural(rel2.toString))})"""
              case q"""def $chainName = $rel1 <-- $rel2""" => 
                q"""def $chainName:Set[${TypeName(relationStarts(rel1.toString))}] = ${TermName(rev(nameToPlural(rel2.toString)))}.flatMap(_.${TermName(rev(nameToPlural(rel1.toString)))})"""
              case otherStatement => otherStatement
            }

            q"""
           case class ${TypeName(className)}(node: Node) extends $parentTrait {
             ..$directNeighbours
             ..$directRevNeighbours
             ..$nodeBody
           }
           """
        }

        val nodeSets: List[Tree] = schemaStatements.collect {
          case NodePattern( className, parentTrait, nodeStatements) =>
            q""" def ${TermName(nameToPlural(className))}: Set[${TypeName(className)}] = nodesAs(${TermName(className)}) """
        }


        val statementPatterns = List(NodeTraitPattern, RelationPattern, NodePattern)


        val otherStatements = schemaStatements.flatMap {
          case NodeTraitPattern(_) | RelationPattern(_) | NodePattern(_) | InnerSchemaPattern(_) => None
          case other => Some(other)
        }


        q"""
        object ${TermName(schemaName.toString)} extends ..$schemaParents {
            ..$nodeTraits
            ..$nodeFactories
            ..$nodeClasses
            ..$relationFactories
            ..$relationClasses

            ..$otherStatements

            object ${ TermName(innerSchemaName) } {def empty = new ${ TypeName(innerSchemaName) }(Graph.empty) }
            case class ${ TypeName(innerSchemaName) }(graph: Graph) extends SchemaGraph {
                ..$nodeSets
                ..$relationSets

                def nodes: Set[${ TypeName(superNodeTrait) }] = $allNodes
                def relations: Set[_ <: SchemaRelation[${ TypeName(superNodeTrait) },${ TypeName(superNodeTrait) }]] = $allRelations
            }
        }
        """
    })
  }
}
