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


    object SchemaPattern {
      def unapply(tree: Tree) = condOpt(tree) {
        case q""" object $schemaName extends ..$schemaParents { ..$schemaStatements } """ =>
          (schemaName, schemaParents, schemaStatements)
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
        case q"""@Relation(${relationType: String}, ${plural: String}) class $className (startNode:$startNode, endNode:$endNode) {..$relationBody}""" =>
          (relationType, plural, className.toString, startNode.toString, endNode.toString, relationBody)
      }
    }

    object NodePattern {
      //TODO:...
      def unapply(tree: Tree) = condOpt(tree) {
        case q"""$mods class $className extends ${parentTrait:TypeName} { ..$nodeStatements }""" if mods.annotations.collectFirst {
            case Apply(Select(New(Ident(TypeName("Node"))), termNames.CONSTRUCTOR), List(Literal(Constant(label: String)), Literal(Constant(plural: String)))) => true
            case _ => false
        }.get =>
          val (label, plural) = mods.annotations.collectFirst {
              case Apply(Select(New(Ident(TypeName("Node"))), termNames.CONSTRUCTOR), List(Literal(Constant(label: String)), Literal(Constant(plural: String)))) => (label, plural)
          }.get
          (label, plural, className, parentTrait, nodeStatements)
      }
    }




    c.Expr[Any](annottees.map(_.tree).toList match {
      case SchemaPattern(schemaName, schemaParents, schemaStatements) :: Nil =>

        val schemaInnerName: String = schemaStatements.collectFirst { case q"val schemaName = ${schemaName: String}" => schemaName }.get
        val nodeTypeName: String = schemaStatements.collectFirst { case q"val nodeType = ${nodeType: String}" => nodeType }.get
        val nodeTypeToFactoryType: Map[TypeName, Tree] = schemaStatements.collect {
          case NodeTraitPattern(traitName, _, traitBody) => 
            println(traitBody)
            traitBody.collect {
            case q"def factory:$factoryType" => traitName -> factoryType
          }
        }.flatten.toMap
        println(nodeTypeToFactoryType)

    val nodePlurals: Map[String, String] = schemaStatements.collect { case NodePattern(_,plural, className, _, _) => className.toString -> plural }.toMap
        val relationPlurals: Map[String, String] = schemaStatements.collect { case RelationPattern(_,plural,className,_,_,_) => className -> plural }.toMap
      val relationStarts: Map[String, String] = schemaStatements.collect { case RelationPattern(_,_,className,startNode,_,_) => className -> startNode }.toMap
      val relationEnds: Map[String, String] = schemaStatements.collect { case RelationPattern(_,_,className,_,endNode,_) => className -> endNode }.toMap
        val allNodes = nodePlurals.values.foldLeft[Tree](q"Set.empty") { case (q"$all", plural) => q"$all ++ ${ TermName(plural) }" }
        val allRelations = relationPlurals.values.foldLeft[Tree](q"Set.empty") { case (q"$all", plural) => q"$all ++ ${ TermName(plural) }" }

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
          case RelationPattern(relationType, plural, className, startNode, endNode, relationBody) =>
            q"""
           object ${TermName(className)} extends SchemaRelationFactory[${TypeName(className)}, ${TypeName(startNode)}, ${TypeName(endNode)}] {
               def create(relation: Relation) = ${TermName(className)}(relation,
                 startNodeFactory.create(relation.startNode),
                 endNodeFactory.create(relation.endNode))
               def relationType = RelationType($relationType)
               def startNodeFactory = ${TermName(startNode)}
               def endNodeFactory = ${TermName(endNode)}
           }
           """
        }

        val relationClasses: List[Tree] = schemaStatements.collect {
          case RelationPattern(relationType, plural, className, startNode, endNode, relationBody) =>
            q"""
           case class ${TypeName(className)}(relation: Relation, startNode: ${TypeName(startNode)}, endNode: ${TypeName(endNode)})
             extends SchemaRelation[${TypeName(startNode)}, ${TypeName(endNode)}] {
             ..$relationBody
             }
           """
        }

        val relationSets: List[Tree] = schemaStatements.collect {
          case RelationPattern(relationType, plural, className, startNode, endNode, relationBody) =>
            q""" def ${TermName(plural)}: Set[${TypeName(className)}] = relationsAs(${TermName(className)}) """
        }

        val nodeFactories: List[Tree] = schemaStatements.collect {
          case NodePattern(label, plural, className, parentTrait, nodeStatements) =>
           q"""
           object ${TermName(className.toString)} extends ${nodeTypeToFactoryType(parentTrait)}[$className] { def create(node: Node) = new $className(node)
               val label = Label($label)
           }
           """
        }
        val nodeClasses: List[Tree] = schemaStatements.collect {
          case NodePattern(label, plural, className, parentTrait, nodeStatements) =>
            val classNameStr = className.toString
            def rev(s: String) = "rev_" + s
            val directNeighbours = schemaStatements.collect {
              case RelationPattern((_, _, relationName, `classNameStr`, endNode, _)) =>
                q"""def ${TermName(relationPlurals(relationName))}:Set[${TypeName(endNode)}] = successorsAs(${TermName(endNode)})"""
            }
            val directRevNeighbours = schemaStatements.collect {
              case RelationPattern((_, _, relationName, startNode, `classNameStr`, _)) =>
                q"""def ${TermName(rev(relationPlurals(relationName)))}:Set[${TypeName(startNode)}] = predecessorsAs(${TermName(startNode)})"""
            }

            val nodeBody = nodeStatements.map{
              case q"""def $chainName = $rel1 --> $rel2""" => 
                q"""def $chainName:Set[${TypeName(relationEnds(rel2.toString))}] = ${TermName(relationPlurals(rel1.toString))}.flatMap(_.${TermName(relationPlurals(rel2.toString))})"""
              case q"""def $chainName = $rel1 <-- $rel2""" => 
                q"""def $chainName:Set[${TypeName(relationStarts(rel1.toString))}] = ${TermName(rev(relationPlurals(rel2.toString)))}.flatMap(_.${TermName(rev(relationPlurals(rel1.toString)))})"""
              case otherStatement => otherStatement
            }

            q"""
           case class $className(node: Node) extends $parentTrait {
             ..$directNeighbours
             ..$directRevNeighbours
             ..$nodeBody
           }
           """
        }

        val nodeSets: List[Tree] = schemaStatements.collect {
          case NodePattern(label, plural, className, parentTrait, nodeStatements) =>
            q""" def ${TermName(plural)}: Set[$className] = nodesAs(${TermName(className.toString)}) """
        }


        val statementPatterns = List(NodeTraitPattern, RelationPattern, NodePattern)


        val otherStatements = schemaStatements.flatMap {
          case NodeTraitPattern(_) | RelationPattern(_) | NodePattern(_) => None
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

            object ${ TermName(schemaInnerName) } {def empty = new ${ TypeName(schemaInnerName) }(Graph.empty) }
            case class ${ TypeName(schemaInnerName) }(graph: Graph) extends SchemaGraph {
                ..$nodeSets
                ..$relationSets

                def nodes: Set[${ TypeName(nodeTypeName) }] = $allNodes
                def relations: Set[_ <: SchemaRelation[${ TypeName(nodeTypeName) },${ TypeName(nodeTypeName) }]] = $allRelations
            }
        }
        """
    })
  }
}
