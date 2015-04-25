package renesca.schema.macros

import scala.reflect.macros.whitebox
import collection.mutable
import scala.language.experimental.macros
import PartialFunction._
import scala.annotation.StaticAnnotation

class GraphSchema extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro GraphSchemaMacro.graphSchema
}

//TODO: fake annotations for the patterns, to satisfy IDE

object GraphSchemaMacro {
  // TODO: why are implicits not working here?
  // implicit def treeToString(l: Tree): String = l match { case Literal(Constant(string: String)) => string }
  // TODO: validation: nodeTraits(propertyTypes), nodes need to inherit exactly one NodeTrait
  // TODO: compile error when nodes inherit not only from nodeTraits

  def graphSchema(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._


    object GraphSchemaPattern {
      def unapply(tree: Tree): Option[(String, List[String], List[Tree])] = condOpt(tree) {
        case q""" object $graphSchemaName extends ..$graphSchemaParents { ..$graphSchemaStatements } """ =>
          (graphSchemaName.toString, graphSchemaParents.map { _.toString } diff List("scala.AnyRef"), graphSchemaStatements)
      }
    }

    object GroupPattern {
      //TODO: statements
      def unapply(tree: Tree): Option[(String, List[String], List[String])] = condOpt(tree) {
        case q""" $mods trait $groupName extends ..$superGroups { List(..$groupNodes) }""" if mods.annotations.collectFirst {
          case Apply(Select(New(Ident(TypeName("Group"))), termNames.CONSTRUCTOR), Nil) => true
          case _                                                                        => false
        }.get =>
          (groupName.toString, superGroups.map { _.toString } diff List("scala.AnyRef"), groupNodes.map { _.toString })
        case q""" $mods trait $groupName extends ..$superGroups""" if mods.annotations.collectFirst {
          case Apply(Select(New(Ident(TypeName("Group"))), termNames.CONSTRUCTOR), Nil) => true
          case _                                                                        => false
        }.get =>
          (groupName.toString, superGroups.map { _.toString } diff List("scala.AnyRef"), Nil)
      }
    }

    object NodeTraitPattern {
      def unapply(tree: Tree): Option[(String, List[String], List[Tree])] = condOpt(tree) {
        //http://stackoverflow.com/questions/26305528/scala-annotations-are-not-found
        case q""" $mods trait $traitName extends ..$traitParents { ..$traitStatements } """ if mods.annotations.collectFirst {
          case Apply(Select(New(Ident(TypeName("Node"))), termNames.CONSTRUCTOR), Nil) => true
          case _                                                                       => false
        }.get =>
          (traitName.toString, traitParents.map { _.toString } diff List("scala.AnyRef"), traitStatements)
      }
    }

    object NodePattern {
      def unapply(tree: Tree): Option[(String, String, List[Tree])] = condOpt(tree) {
        case q"""@Node class $nodeName extends ${nodeParentTrait: TypeName} { ..$nodeStatements }""" =>
          (nodeName.toString, nodeParentTrait.toString, nodeStatements)
      }
    }

    object RelationPattern {
      def unapply(tree: Tree): Option[(String, String, String, List[Tree])] = condOpt(tree) {
        case q"""@Relation class $relationName (startNode:$startNode, endNode:$endNode) {..$relationStatements}""" =>
          (relationName.toString, startNode.toString, endNode.toString, relationStatements)
      }
    }

    object HyperRelationPattern {
      def unapply(tree: Tree): Option[(String, String, String, String, List[Tree])] = condOpt(tree) {
        case q"""@HyperRelation class $hyperRelationName (startNode:$startNode, endNode:$endNode) extends $hyperRelationParent {..$hyperRelationStatements}""" =>
          (hyperRelationName.toString, startNode.toString, endNode.toString, hyperRelationParent.toString, hyperRelationStatements)
      }
    }





    c.Expr[Any](annottees.map(_.tree).toList match {
      case GraphSchemaPattern(graphSchemaName, graphSchemaParents, graphSchemaStatements) :: Nil =>

        val nodeTraitNames: List[String] = graphSchemaStatements.collect {
          case NodeTraitPattern(traitName, _, _) => traitName
        }

        val nodeTraitToParents: Map[String, List[String]] = graphSchemaStatements.collect {
          case NodeTraitPattern(traitName, traitParents, _) => traitName -> traitParents
        }.toMap

        val hyperRelationToParent: Map[String, String] = graphSchemaStatements.collect {
          case HyperRelationPattern(hyperRelationName, _, _, hyperRelationParent, _) => hyperRelationName -> hyperRelationParent
        }.toMap

        val nodeTraitsWithoutChildren = nodeTraitToParents.keys.toList diff nodeTraitToParents.values.flatten.toList
        println("nodeTraitsWithoutChildren: " + nodeTraitsWithoutChildren)
        val nodeTraitToChildren: Map[String, List[String]] = nodeTraitToParents.toList.flatMap {
          case (traitName, parents) => parents.map { parent => parent -> traitName }
        }.groupBy { case (parent, _) => parent }.map {
          case (parent, parentsToNodeTraits) => parent -> parentsToNodeTraits.map { case (_, traitName) => traitName }
        } ++ nodeTraitsWithoutChildren.map(_ -> Nil)
        println("nodeTraitToChildren: " + nodeTraitToChildren)

        val nodeTraitToFlatChildren: Map[String, List[String]] = nodeTraitToChildren.keys.map { traitName =>
          def flatout(parent: String): List[String] = parent :: nodeTraitToChildren(parent).flatMap(flatout)
          traitName -> flatout(traitName).distinct
        }.toMap
        println("nodeTraitToFlatChildren: " + nodeTraitToFlatChildren)

        val nodeToTrait: Map[String, String] = graphSchemaStatements.collect {
          case NodePattern(nodeName, parentTrait, _) => nodeName -> parentTrait
        }.toMap

        val nodeTraitToNodes: Map[String, List[String]] = nodeTraitNames.map(_ -> Nil).toMap ++ nodeToTrait.toList.groupBy { case (_, traitName) => traitName }.mapValues { list => list.map { case (nodeName, _) => nodeName } }
        println("nodeTraitToNodes: " + nodeTraitToNodes)
        val nodeTraitToChildNodes: Map[String, List[String]] = nodeTraitToFlatChildren.mapValues {
          _.flatMap(childNodeTrait => nodeTraitToNodes(childNodeTrait))
        }
        println("nodeTraitToChildNodes: " + nodeTraitToChildNodes)
        val nodeTraitToChildRelations: Map[String, List[String]] = nodeTraitToChildNodes.mapValues { nodeNames =>
          graphSchemaStatements.collect {
            case RelationPattern(relationName, startNode, endNode, _) if (nodeNames contains startNode) && (nodeNames contains endNode) => relationName
          }
        }
        println("nodeTraitToChildRelations: " + nodeTraitToChildRelations)
        val nodeTraitToChildHyperRelations: Map[String, List[String]] = nodeTraitToChildNodes.mapValues { nodeNames =>
          graphSchemaStatements.collect {
            case HyperRelationPattern(hyperRelationName, startNode, endNode, _, _) if (nodeNames contains startNode) && (nodeNames contains endNode) => hyperRelationName
          }
        }
        println("nodeTraitToChildHyperRelations: " + nodeTraitToChildHyperRelations)

        val nodeTraitToCommonHyperNodeTraits = nodeTraitToChildHyperRelations.mapValues { childHyperRelations =>
          childHyperRelations.map(hyperRelationName => List(hyperRelationToParent(hyperRelationName))).reduce(_ intersect _)
        }
        println("nodeTraitToCommonHyperNodeTraits: " + nodeTraitToCommonHyperNodeTraits)

        val groupToNodes: Map[String, List[String]] = graphSchemaStatements.collect {
          case GroupPattern(groupName, _, nodes) => groupName -> nodes
        }.toMap

        val groupToParents: Map[String, List[String]] = graphSchemaStatements.collect {
          case GroupPattern(groupName, groupParents, _) => groupName -> groupParents
        }.toMap
        println("groupToParents: " + groupToParents)

        val groupsWithoutChildren = groupToParents.keys.toList diff groupToParents.values.flatten.toList
        val groupToChildren: Map[String, List[String]] = groupToParents.toList.flatMap {
          case (groupName, parents) => parents.map { parent => parent -> groupName }
        }.groupBy { case (parent, _) => parent }.map {
          case (parent, parentsToGroups) => parent -> parentsToGroups.map { case (_, groupName) => groupName }
        } ++ groupsWithoutChildren.map(_ -> Nil)
        println("groupToChildren: " + groupToChildren)

        //        val groupToFlatParents: Map[String, List[String]] = groupToParents.keys.map { groupName =>
        //          def flatout(child: String): List[String] = child :: groupToParents(child).flatMap(flatout)
        //          groupName -> flatout(groupName).distinct
        //        }.toMap
        //        println("groupToFlatParents: " + groupToFlatParents)

        val groupToFlatChildren: Map[String, List[String]] = groupToChildren.keys.map { groupName =>
          def flatout(parent: String): List[String] = parent :: groupToChildren(parent).flatMap(flatout)
          groupName -> flatout(groupName).distinct
        }.toMap
        println("groupToFlatChildren: " + groupToFlatChildren)

        val groupToChildNodes: Map[String, List[String]] = groupToFlatChildren.mapValues {
          _.flatMap(childGroup => groupToNodes(childGroup))
        }
        println("groupToChildNodes: " + groupToChildNodes)

        val groupToChildRelations: Map[String, List[String]] = groupToChildNodes.mapValues { nodeNames =>
          graphSchemaStatements.collect {
            case RelationPattern(relationName, startNode, endNode, _) if (nodeNames contains startNode) && (nodeNames contains endNode) => relationName
          }
        }
        println("groupToChildRelations: " + groupToChildRelations)

        val groupToChildHyperRelations: Map[String, List[String]] = groupToChildNodes.mapValues { nodeNames =>
          graphSchemaStatements.collect {
            case HyperRelationPattern(hyperRelationName, startNode, endNode, _, _) if (nodeNames contains startNode) && (nodeNames contains endNode) => hyperRelationName
          }
        }
        println("groupToChildHyperRelations: " + groupToChildHyperRelations)

        def nodeTraitToFactory(name: String) = name + "Factory"
        def nameToPlural(name: String) = {
          val lower = name.take(1).toLowerCase + name.drop(1)
          val suffix = if(lower.endsWith("s")) "" else "s"
          lower + suffix;
        }
        def nameToLabel(name: String) = name.toUpperCase
        def relationName(start: String, end: String) = s"${ start }To${ end }"

        val relationStarts: Map[String, String] = graphSchemaStatements.collect { case RelationPattern(className, startNode, _, _) => className -> startNode }.toMap
        val relationEnds: Map[String, String] = graphSchemaStatements.collect { case RelationPattern(className, _, endNode, _) => className -> endNode }.toMap

        val nodeTraits: List[Tree] = graphSchemaStatements.collect {
          case NodeTraitPattern(traitName, traitParents, traitStatements) =>

            def propertyGetter(name: String, typeName: Tree) =
              q""" def ${ TermName(name) }:$typeName = node.properties(${ name }).asInstanceOf[${ TypeName(typeName.toString + "PropertyValue") }] """
            def propertyOptionGetter(name: String, typeName: Tree) =
              q""" def ${ TermName(name) }:Option[$typeName] = node.properties.get(${ name }).asInstanceOf[Option[${ TypeName(typeName.toString + "PropertyValue") }]].map(propertyValueToPrimitive) """
            def propertySetter(name: String, typeName: Tree) =
              q""" def ${ TermName(name + "_$eq") }(newValue:$typeName){ node.properties(${ name }) = newValue} """
            def propertyOptionSetter(name: String, typeName: Tree) =
              q""" def ${ TermName(name + "_$eq") }(newValue:Option[$typeName]){ if(newValue.isDefined) node.properties(${ name }) = newValue.get else node.properties -= ${ name } }"""
            def generatePropertyAccessors(statement: Tree): List[Tree] = statement match {
              case q"val $propertyName:Option[$propertyType]" => List(propertyOptionGetter(propertyName.toString, propertyType))
              case q"var $propertyName:Option[$propertyType]" => List(propertyOptionGetter(propertyName.toString, propertyType), propertyOptionSetter(propertyName.toString, propertyType))
              case q"val $propertyName:$propertyType"         => List(propertyGetter(propertyName.toString, propertyType))
              case q"var $propertyName:$propertyType"         => List(propertyGetter(propertyName.toString, propertyType), propertySetter(propertyName.toString, propertyType))
              case somethingElse                              => List(somethingElse)
            }

            val traitBody = traitStatements.flatMap(generatePropertyAccessors(_)).flatMap {
              case q"def factory:$factoryType" => Nil
              case somethingElse               => List(somethingElse)
            }
            val traitParentsWithSchemaNode = (if(traitParents.nonEmpty && traitParents != List("scala.AnyRef")) traitParents else List("SchemaNode")).map(TypeName(_))

            q""" trait ${ TypeName(traitName) } extends ..$traitParentsWithSchemaNode { ..$traitBody } """
        }

        val relationFactories: List[Tree] = graphSchemaStatements.collect {
          case RelationPattern(className, startNode, endNode, _) =>
            q"""
           object ${ TermName(className) } extends SchemaRelationFactory[${ TypeName(startNode) }, ${ TypeName(className) }, ${ TypeName(endNode) }] {
               def startNodeFactory = ${ TermName(startNode) }
               def endNodeFactory = ${ TermName(endNode) }
               def relationType = RelationType(${ nameToLabel(className) })
               def create(relation: Relation) = ${ TermName(className) }(
                 ${ TermName(startNode) }.create(relation.startNode),
                 relation,
                 ${ TermName(endNode) }.create(relation.endNode))
           }
           """
        }

        val relationClasses: List[Tree] = graphSchemaStatements.collect {
          case RelationPattern(className, startNode, endNode, relationBody) =>
            q"""
           case class ${ TypeName(className) }(startNode: ${ TypeName(startNode) }, relation: Relation, endNode: ${ TypeName(endNode) })
             extends SchemaRelation[${ TypeName(startNode) }, ${ TypeName(endNode) }] {
             ..$relationBody
             }
           """
        }


        val hyperRelationFactories: List[Tree] = graphSchemaStatements.collect {
          case HyperRelationPattern(className, startNode, endNode, hyperRelationParent, _) =>
            val startRelation = relationName(startNode, className)
            val endRelation = relationName(className, endNode)

            q"""
           object ${ TermName(className) } extends SchemaHyperRelationFactory[${ TypeName(startNode) }, ${ TypeName(startRelation) }, ${ TypeName(className) }, ${ TypeName(endRelation) }, ${ TypeName(endNode) }]
            with ${ TypeName(nodeTraitToFactory(hyperRelationParent)) }[${ TypeName(className) }] {
               override def middleNodeLocal = super[${ TypeName(nodeTraitToFactory(hyperRelationParent)) }].local

               override def label = Label(${ nameToLabel(className) })
               override def startRelationType = RelationType(${ nameToLabel(startRelation) })
               override def endRelationType = RelationType(${ nameToLabel(endRelation) })

               override def startNodeFactory = ${ TermName(startNode) }
               override def factory = ${ TermName(className) }
               override def endNodeFactory = ${ TermName(endNode) }

               override def create(node: Node) = new ${ TypeName(className) }(node)
               override def startRelationCreate(relation: Relation) = ${ TermName(startRelation) }(startNodeFactory.create(relation.startNode), relation, factory.create(relation.endNode))
               override def endRelationCreate(relation: Relation) = ${ TermName(endRelation) }(factory.create(relation.startNode), relation, endNodeFactory.create(relation.endNode))
           }
           """
        }

        val hyperRelationClasses: List[Tree] = graphSchemaStatements.collect {
          case HyperRelationPattern(hyperRelation, startNode, endNode, hyperRelationParent, hyperRelationBody) =>
            val startRelation = relationName(startNode, hyperRelation)
            val endRelation = relationName(hyperRelation, endNode)
            List( q"""
           case class ${ TypeName(hyperRelation) }(node:Node)
              extends SchemaHyperRelation[${ TypeName(startNode) }, ${ TypeName(startRelation) }, ${ TypeName(hyperRelation) }, ${ TypeName(endRelation) }, ${ TypeName(endNode) }]
              with ${ TypeName(hyperRelationParent) } {
             ..$hyperRelationBody
           }
           """, q"""
           case class ${ TypeName(startRelation) }(startNode: ${ TypeName(startNode) }, relation: Relation, endNode: ${ TypeName(hyperRelation) })
             extends SchemaRelation[${ TypeName(startNode) }, ${ TypeName(hyperRelation) }]
           """, q"""
           case class ${ TypeName(endRelation) }(startNode: ${ TypeName(hyperRelation) }, relation: Relation, endNode: ${ TypeName(endNode) })
             extends SchemaRelation[${ TypeName(hyperRelation) }, ${ TypeName(endNode) }]
           """)
        }.flatten

        val nodeFactories: List[Tree] = graphSchemaStatements.collect {
          case NodePattern(className, parentTrait, nodeStatements) =>
            q"""
           object ${ TermName(className) } extends ${ TypeName(nodeTraitToFactory(parentTrait)) }[${ TypeName(className) }] { def create(node: Node) = new ${ TypeName(className) }(node)
               val label = Label(${ nameToLabel(className) })
           }
           """
        }
        val nodeClasses: List[Tree] = graphSchemaStatements.collect {
          // TODO: acessors for traits
          // TODO: create subgroups
          case NodePattern(className, parentTrait, nodeStatements) =>
            def rev(s: String) = "rev_" + s
            val directNeighbours = graphSchemaStatements.collect {
              case RelationPattern(relationName, `className`, endNode, _) =>
                q"""def ${ TermName(nameToPlural(relationName)) }:Set[${ TypeName(endNode) }] = successorsAs(${ TermName(endNode) })"""
            }
            val directRevNeighbours = graphSchemaStatements.collect {
              case RelationPattern(relationName, startNode, `className`, _) =>
                q"""def ${ TermName(rev(nameToPlural(relationName))) }:Set[${ TypeName(startNode) }] = predecessorsAs(${ TermName(startNode) })"""
            }

            val nodeBody = nodeStatements.map {
              case q"""def $chainName = $rel1 --> $rel2""" =>
                q"""def $chainName:Set[${ TypeName(relationEnds(rel2.toString)) }] = ${ TermName(nameToPlural(rel1.toString)) }.flatMap(_.${ TermName(nameToPlural(rel2.toString)) })"""
              case q"""def $chainName = $rel1 <-- $rel2""" =>
                q"""def $chainName:Set[${ TypeName(relationStarts(rel1.toString)) }] = ${ TermName(rev(nameToPlural(rel2.toString))) }.flatMap(_.${ TermName(rev(nameToPlural(rel1.toString))) })"""
              case otherStatement                          => otherStatement
            }

            q"""
           case class ${ TypeName(className) }(node: Node) extends ${ TypeName(parentTrait) } {
             ..$directNeighbours
             ..$directRevNeighbours
             ..$nodeBody
           }
           """
        }

        val groupFactories: List[Tree] = graphSchemaStatements.collect {
          case GroupPattern(groupName, _, _) =>
            q""" object ${ TermName(groupName) } {def empty = new ${ TypeName(groupName) }(Graph.empty) } """
        }

        val groupClasses: List[Tree] = graphSchemaStatements.collect {
          case GroupPattern(groupName, schemaNodeTraits, _) =>
            val nodes: List[String] = graphSchemaStatements.collect { case NodePattern(nodeName, _, _) if groupToChildNodes(groupName) contains nodeName => nodeName }
            val relations: List[String] = graphSchemaStatements.collect { case RelationPattern(relationName, _, _, _) if groupToChildRelations(groupName) contains relationName => relationName }
            val hyperRelations: List[String] = graphSchemaStatements.collect { case HyperRelationPattern(hyperRelationName, _, _, _, _) if groupToChildHyperRelations(groupName) contains hyperRelationName => hyperRelationName }

            def itemSets(nameAs: String, names: List[String]) = names.map { name => q""" def ${ TermName(nameToPlural(name)) }: Set[${ TypeName(name) }] = ${ TermName(nameAs) }(${ TermName(name) }) """ }
            val nodeSets = itemSets("nodesAs", nodes)
            val relationSets = itemSets("relationsAs", relations)
            val hyperRelationSets = itemSets("hyperRelationsAs", hyperRelations)

            def allOf(items: List[String]) = items.foldLeft[Tree](q"Set.empty") { case (q"$all", name) => q"$all ++ ${ TermName(nameToPlural(name)) }" }
            val allNodes = allOf(nodes)
            val allRelations = allOf(relations)
            val allHyperRelations = allOf(hyperRelations)

            val nodeTraitSets = nodeTraitNames.map { traitName => q"def ${ TermName(nameToPlural(traitName)) }:Set[${ TypeName(traitName) }] = ${ allOf(nodeTraitToChildNodes(traitName)) }" }
            val relationTraitSets = nodeTraitNames.map { traitName => q"def ${ TermName(nameToPlural(traitName + "Relation")) }:Set[_ <: SchemaRelation[${ TypeName(traitName) }, ${ TypeName(traitName) }]] = ${ allOf(nodeTraitToChildRelations(traitName)) }" }
            val hyperRelationTraitSets = nodeTraitNames.map { traitName =>
              val traitType = TypeName(traitName)
              q"""def ${ TermName(nameToPlural(traitName + "HyperRelation")) } :Set[SchemaHyperRelation[$traitType, _ <: SchemaRelation[$traitType, _], _ <: SchemaHyperRelation[$traitType, _, _, _, $traitType] with ..${ nodeTraitToCommonHyperNodeTraits(traitName).map(TypeName(_)) }, _ <: SchemaRelation[_, $traitType], $traitType] with ..${ nodeTraitToCommonHyperNodeTraits(traitName).map(TypeName(_)) }]
              = ${ allOf(nodeTraitToChildHyperRelations(traitName)) }"""
            }

            q"""
           case class ${ TypeName(groupName) }(graph: Graph) extends SchemaGraph {
             ..$nodeSets
             ..$relationSets
             ..$hyperRelationSets

             ..$nodeTraitSets
             ..$relationTraitSets
             ..$hyperRelationTraitSets

             def nodes: Set[SchemaNode] = $allNodes
             def relations: Set[_ <: SchemaRelation[_,_]] = $allRelations
             def hyperRelations: Set[_ <: SchemaHyperRelation[_,_,_,_,_]] = $allHyperRelations
           }
           """
        }

        val otherStatements = graphSchemaStatements.flatMap {
          case NodeTraitPattern(_, _, _)
               | RelationPattern(_, _, _, _)
               | NodePattern(_, _, _)
               | GroupPattern(_, _, _)
               | HyperRelationPattern(_, _, _, _, _) => None
          case other                                 => Some(other)
        }


        q"""
       object ${ TermName(graphSchemaName) } extends ..${ graphSchemaParents.map(TypeName(_)) } {
         import renesca.graph.{Graph,Label,RelationType,Node,Relation}
         import renesca.parameter.StringPropertyValue
         import renesca.parameter.implicits._

         ..$nodeTraits
         ..$nodeFactories
         ..$nodeClasses

         ..$relationFactories
         ..$relationClasses

         ..$hyperRelationFactories
         ..$hyperRelationClasses

         ..$groupFactories
         ..$groupClasses

         ..$otherStatements
       }
       """
    }
    )
  }
}
