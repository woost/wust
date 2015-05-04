package renesca.schema.macros

import scala.reflect.macros.whitebox
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
  // TODO: compile error when nodes inherit not only from nodeTraits

  def graphSchema(context: whitebox.Context)(annottees: context.Expr[Any]*): context.Expr[Any] = {
    import Helpers._
    import context.universe._

    // import schema and patterns:
    // we need to pass the context and its type to the objects we want to
    // import, because the macro types depend on the context. furthermore, we
    // need to make sure that we import the same patternContext as the
    // schemaContext, otherwise the compiler complains about mismatching types.
    val schemaContext = new SchemaContext[context.type](context)
    import schemaContext.patternContext.Patterns._
    import schemaContext.Schemas.Schema
    import schemaContext.Schemas.Schema._

    context.Expr[Any](annottees.map(_.tree).toList match {
      case SchemaPattern(schemaPattern) :: Nil =>
        object Code {

          def relationStart(schema: Schema, name: String): String = schema.relations.find(_.name == name).get.startNode
          def relationEnd(schema: Schema, name: String): String = schema.relations.find(_.name == name).get.endNode

          def propertyGetter(name: String, typeName: Tree) =
            q""" def ${ TermName(name) }:$typeName = node.properties(${ name }).asInstanceOf[${ TypeName(typeName.toString + "PropertyValue") }] """
          def propertyOptionGetter(name: String, typeName: Tree) =
            q""" def ${ TermName(name) }:Option[$typeName] = node.properties.get(${ name }).asInstanceOf[Option[${ TypeName(typeName.toString + "PropertyValue") }]].map(propertyValueToPrimitive) """
          def propertySetter(name: String, typeName: Tree) =
            q""" def ${ TermName(name + "_$eq") }(newValue:$typeName){ node.properties(${ name }) = newValue} """
          def propertyOptionSetter(name: String, typeName: Tree) =
            q""" def ${ TermName(name + "_$eq") }(newValue:Option[$typeName]){ if(newValue.isDefined) node.properties(${ name }) = newValue.get else node.properties -= ${ name } }"""
          def generatePropertyAccessors(statement: Tree): List[Tree] = statement match {
            case q"val $propertyName:Option[$propertyType]"      => List(propertyOptionGetter(propertyName.toString, propertyType))
            case q"var $propertyName:Option[$propertyType]"      => List(propertyOptionGetter(propertyName.toString, propertyType), propertyOptionSetter(propertyName.toString, propertyType))
            case q"val $propertyName:Option[$propertyType] = $x" => List(propertyOptionGetter(propertyName.toString, propertyType))
            case q"var $propertyName:Option[$propertyType] = $x" => List(propertyOptionGetter(propertyName.toString, propertyType), propertyOptionSetter(propertyName.toString, propertyType))
            case q"val $propertyName:$propertyType"              => List(propertyGetter(propertyName.toString, propertyType))
            case q"var $propertyName:$propertyType"              => List(propertyGetter(propertyName.toString, propertyType), propertySetter(propertyName.toString, propertyType))
            case q"val $propertyName:$propertyType = $x"         => List(propertyGetter(propertyName.toString, propertyType))
            case q"var $propertyName:$propertyType = $y"         => List(propertyGetter(propertyName.toString, propertyType), propertySetter(propertyName.toString, propertyType))
            case somethingElse                                   => List(somethingElse)
          }

          def generateIndirectNeighbourAccessors(schema: Schema, statement: Tree): Tree = statement match {
            case q"""def $chainName = $rel1 --> $rel2""" =>
              q"""def $chainName:Set[${ TypeName(relationEnd(schema, rel2.toString)) }] = ${ TermName(nameToPlural(rel1.toString)) }.flatMap(_.${ TermName(nameToPlural(rel2.toString)) })"""
            case q"""def $chainName = $rel1 <-- $rel2""" =>
              q"""def $chainName:Set[${ TypeName(relationStart(schema, rel1.toString)) }] = ${ TermName(rev(nameToPlural(rel2.toString))) }.flatMap(_.${ TermName(rev(nameToPlural(rel1.toString))) })"""
            case otherStatement                          => otherStatement
          }

          def createLocalFactoryMethod(flatStatements: List[Tree], schemaItem: Tree, factoryTemplate: (List[List[Tree]], List[Tree]) => Tree) = {
            val localNonOptionalParamsWithoutDefault = flatStatements.flatMap {
              case statement@(q"val $x:Option[$propertyType]") => None
              case statement@(q"var $x:Option[$propertyType]") => None
              case statement@(q"val $x:$propertyType")         => Some(statement)
              case statement@(q"var $x:$propertyType")         => Some(statement) //TODO: test! does var work as param?
              case _                                           => None
            }
            val localParamsWithDefault = flatStatements.collect {
              case statement@(q"val $x: $y = $default") if !default.isEmpty => statement
              case statement@(q"var $x: $y = $default") if !default.isEmpty => statement
            }
            val localOptionalParamsWithoutDefault = flatStatements.collect {
              case statement@(q"val $propertyName:Option[$propertyType]") => q"val $propertyName:Option[$propertyType] = None"
              case statement@(q"var $propertyName:Option[$propertyType]") => q"val $propertyName:Option[$propertyType] = None"
            }

            val localParams = List(localNonOptionalParamsWithoutDefault ::: localParamsWithDefault ::: localOptionalParamsWithoutDefault)

            val properties = flatStatements.flatMap {
              case statement@(q"val $x:Option[$propertyType]")      => None
              case statement@(q"var $x:Option[$propertyType]")      => None
              case statement@(q"val $x:Option[$propertyType] = $y") => None
              case statement@(q"var $x:Option[$propertyType] = $y") => None
              case q"val $propertyName:$propertyType"               => Some(propertyName)
              case q"var $propertyName:$propertyType"               => Some(propertyName)
              case q"val $propertyName:$propertyType = $x"          => Some(propertyName)
              case q"var $propertyName:$propertyType = $x"          => Some(propertyName)
            }
            val optionalProperties = flatStatements.collect {
              case q"val $propertyName:Option[$x]"      => propertyName
              case q"var $propertyName:Option[$x]"      => propertyName
              case q"val $propertyName:Option[$x] = $y" => propertyName
              case q"var $propertyName:Option[$x] = $y" => propertyName
            }

            val nonOptionalAssignments = properties.map { propertyName => q"$schemaItem.properties(${ propertyName.toString }) = $propertyName" }
            val optionalPropertyAssignments = optionalProperties.map { propertyName => q"if($propertyName.isDefined) $schemaItem.properties(${ propertyName.toString }) = $propertyName.get" }
            val propertyAssignments = nonOptionalAssignments ::: optionalPropertyAssignments

            factoryTemplate(localParams, propertyAssignments)
          }

          def nodeTraitFactories(schema: Schema): List[Tree] = schema.nodeTraits.filter(_.hasOwnFactory).map { nodeTrait => import nodeTrait._
            val localWithParams = createLocalFactoryMethod(flatStatements, q"node.node", { (localParams, propertyAssignments) =>
              q"""
              def local (...$localParams): NODE = {
              val node = create(Node.local(List(label)))
              ..$propertyAssignments
              node
              }"""
            })
            val factoryName = TypeName(traitFactoryName(name))
            q"""
           trait $factoryName[NODE <: SchemaNode] extends SchemaNodeFactory[NODE] {
             $localWithParams
           }
           """
          }
          def relationTraitFactories(schema: Schema): List[Tree] = schema.relationTraits.filter(_.hasOwnFactory).map { relationTrait => import relationTrait._
            val localWithParams = createLocalFactoryMethod(flatStatements, q"relation.relation", { (localParams, propertyAssignments) =>
              val startEndlocalParams = List(List(q"val startNode:START", q"val endNode:END") ::: localParams.head)
              q""" def local (...$startEndlocalParams): RELATION """
            })
            val factoryName = TypeName(traitFactoryName(name))
            q"""
           trait $factoryName[START <: SchemaNode, RELATION <: SchemaRelation[START,END], END <: SchemaNode] extends SchemaAbstractRelationFactory[START,RELATION,END] {
             $localWithParams
           }
           """
          }

          def nodeFactories(schema: Schema): List[Tree] = schema.nodes.map { node => import node._
            //TODO: remove if?
            val localWithParams = if(superTypesFlatStatementsCount != flatStatements.size) //TODO: DRY machen, gibts auch in relationTraitfactories
                                    createLocalFactoryMethod(flatStatements, q"node.node", { (localParams, propertyAssignments) =>
                                      q"""
                                    def local (...$localParams):$name_type = {
                                    val node = create(Node.local(List(label)))
                                    ..$propertyAssignments
                                    node
                                    }"""
                                    })
                                  else
                                    q""

            val superFactory = TypeName(superTypes.headOption match {
              case Some(superType) => traitFactoryName(superType)
              case None            => "SchemaNodeFactory"
            })
            // Extending superFactory is enough, because SchemaNodeFactory is pulled in every case.
            // This works because SchemaNodeFactory does not get any generics.
            q"""
           object $name_term extends $superFactory[$name_type] {
             def create(node: Node) = new $name_type(node)
             val label = Label($name_label)
             $localWithParams
           }
           """
          }

          def nodeClasses(schema: Schema): List[Tree] = schema.nodes.map { node => import node._
            val directNeighbours = node.neighbours_terms.map {
              case (relationPlural, endNode_type, endNode_term) =>
                q"""def $relationPlural:Set[$endNode_type] = successorsAs($endNode_term)"""
            }

            val directRevNeighbours = node.rev_neighbours_terms.map {
              case (relationPlural, startNode_type, startNode_term) =>
                q"""def $relationPlural:Set[$startNode_type] = predecessorsAs($startNode_term)"""
            }

            val nodeBody = statements.map(generateIndirectNeighbourAccessors(schema, _)).flatMap(generatePropertyAccessors(_))
            val superTyesWithDefault = if(superTypes.isEmpty) List(TypeName("SchemaNode")) else superTypes_type

            q"""
           case class $name_type(node: Node) extends ..$superTyesWithDefault {
             ..$directNeighbours
             ..$directRevNeighbours
             ..$nodeBody
           }
           """
          }

          def relationFactories(schema: Schema): List[Tree] = schema.relations.map { relation => import relation._
            val localWithParams = createLocalFactoryMethod(flatStatements, q"relation.relation", { (localParams, propertyAssignments) => //TODO: DRY
              val startEndlocalParams = List(List(q"val startNode:$startNode_type", q"val endNode:$endNode_type") ::: localParams.head)
              q"""
              def local(...$startEndlocalParams): $name_type = {
              val relation = create(Relation.local(startNode.node, endNode.node, relationType))
              ..$propertyAssignments
              relation
              }"""
            })
            val superRelationFactory: TypeName = superTypes.headOption match {
              case Some(superType) => TypeName(traitFactoryName(superType))
              case None            => TypeName(s"SchemaAbstractRelationFactory[$startNode_type, $name_type, $endNode_type]")
            }
            q"""
           object $name_term extends SchemaRelationFactory[$startNode_type, $name_type, $endNode_type]
            with $superRelationFactory {
               def startNodeFactory = $startNode_term
               def endNodeFactory = $endNode_term
               def relationType = RelationType($name_label)
               def create(relation: Relation) = $name_term(
                 $startNode_term.create(relation.startNode),
                 relation,
                 $endNode_term.create(relation.endNode))
               $localWithParams
           }
           """
          }

          def relationClasses(schema: Schema): List[Tree] = schema.relations.map { relation => import relation._
            q"""
           case class $name_type(startNode: $startNode_type, relation: Relation, endNode: $endNode_type)
             extends SchemaRelation[$startNode_type, $endNode_type] {
             ..$statements
           }
           """
          }


          def hyperRelationFactories(schema: Schema): List[Tree] = schema.hyperRelations.map { hyperRelation => import hyperRelation._
            val superRelationFactory = superRelationTypes.headOption match {
              case Some(superType) => tq"${ traitFactoryName(superType) }"
              case None            => tq"SchemaAbstractRelationFactory[$startNode_type, $name_type, $endNode_type]"
            }
            q"""
           object $name_term extends SchemaHyperRelationFactory[$startNode_type, $startRelation_type, $name_type, $endRelation_type, $endNode_type]
             with $superRelationFactory {

             override def label = Label($name_label)
             override def startRelationType = RelationType($startRelation_label)
             override def endRelationType = RelationType($endRelation_label)

             override def startNodeFactory = $startNode_term
             override def factory = $name_term
             override def endNodeFactory = $endNode_term

             override def create(node: Node) = new $name_type(node)
             override def startRelationCreate(relation: Relation) = $startRelation_term(startNodeFactory.create(relation.startNode), relation, factory.create(relation.endNode))
             override def endRelationCreate(relation: Relation) = $endRelation_term(factory.create(relation.startNode), relation, endNodeFactory.create(relation.endNode))
           }
           """
          }

          def hyperRelationClasses(schema: Schema): List[Tree] = schema.hyperRelations.map { hyperRelation => import hyperRelation._
            //TODO: generate indirect neighbour-accessors based on hyperrelations
            //TODO: property accessors
            List( q"""
           case class $name_type(node:Node)
              extends SchemaHyperRelation[$startNode_type, $startRelation_type, $name_type, $endRelation_type, $endNode_type]
              with ..$superTypes_type {
             ..$statements
           }
           """, q"""
           case class $startRelation_type(startNode: $startNode_type, relation: Relation, endNode: $name_type)
             extends SchemaRelation[$startNode_type, $name_type]
           """, q"""
           case class $endRelation_type(startNode: $name_type, relation: Relation, endNode: $endNode_type)
             extends SchemaRelation[$name_type, $endNode_type]
           """)
          }.flatten

          def nodeSuperTraits(schema: Schema): List[Tree] = schema.nodeTraits.map { nodeTrait => import nodeTrait._
            val traitBody = statements.flatMap(generatePropertyAccessors(_))
            q""" trait $name_type extends ..$superTypes_type { ..$traitBody } """
          }

          def relationSuperTraits(schema: Schema): List[Tree] = schema.relationTraits.map { relationTrait => import relationTrait._
            val traitBody = statements.flatMap(generatePropertyAccessors(_))
            q""" trait $name_type extends ..$superTypes_type { ..$traitBody } """
          }

          def groupFactories(schema: Schema): List[Tree] = schema.groups.map { group => import group._
            q""" object $name_term {def empty = new $name_type(Graph.empty) } """
          }

          def groupClasses(schema: Schema): List[Tree] = schema.groups.map { group => import group._
            // TODO: create subgroups

            def itemSets(nameAs: String, names: List[String]) = names.map { name => q""" def ${ TermName(nameToPlural(name)) }: Set[${ TypeName(name) }] = ${ TermName(nameAs) }(${ TermName(name) }) """ }
            def allOf(items: List[String]) = items.foldLeft[Tree](q"Set.empty") { case (q"$all", name) => q"$all ++ ${ TermName(nameToPlural(name)) }" }

            val nodeSets = itemSets("nodesAs", nodes)
            val relationSets = itemSets("relationsAs", relations)
            val hyperRelationSets = itemSets("hyperRelationsAs", hyperRelations)

            val nodeTraitSets = nodeTraits.map { nodeTrait => import nodeTrait._
              q"def $name_plural_term:Set[$name_type] = ${ allOf(subNodes) }"
            }
            val relationTraitSets = nodeTraits.map { nodeTrait => import nodeTrait._
              q"def ${ TermName(nameToPlural(name + "Relation")) }:Set[_ <: SchemaRelation[$name_type, $name_type]] = ${ allOf(subRelations) }"
            }
            val hyperRelationTraitSets = nodeTraits.map { nodeTrait => import nodeTrait._
              q"""def ${ TermName(nameToPlural(name + "HyperRelation")) } :Set[SchemaHyperRelation[
                  $name_type,
                  _ <: SchemaRelation[$name_type, _],
                  _ <: SchemaHyperRelation[$name_type, _, _, _, $name_type] with ..$commonHyperNodeTraits_type,
                  _ <: SchemaRelation[_, $name_type],
                  $name_type]
                  with ..$commonHyperNodeTraits_type]
              = ${ allOf(subHyperRelations) }"""
            }

            q"""
           case class $name_type(graph: Graph) extends SchemaGraph {
             ..$nodeSets
             ..$relationSets
             ..$hyperRelationSets

             ..$nodeTraitSets
             ..$relationTraitSets
             ..$hyperRelationTraitSets

             def nodes: Set[SchemaNode] = ${ allOf(nodes) }
             def relations: Set[_ <: SchemaRelation[_,_]] = ${ allOf(relations) }
             def hyperRelations: Set[_ <: SchemaHyperRelation[_,_,_,_,_]] = ${ allOf(hyperRelations) }
           }
           """
          }

          def otherStatements(schema: Schema): List[Tree] = schema.statements.filterNot { statement =>
            NodePattern.unapply(statement).isDefined ||
              RelationPattern.unapply(statement).isDefined ||
              HyperRelationPattern.unapply(statement).isDefined ||
              NodeTraitPattern.unapply(statement).isDefined ||
              RelationTraitPattern.unapply(statement).isDefined ||
              GroupPattern.unapply(statement).isDefined
          }


          def schema(schema: Schema): Tree = {
            import schema.{name_term, superTypes_type}
            q"""
           object $name_term extends ..$superTypes_type {
             import renesca.graph.{Graph,Label,RelationType,Node,Relation}
             import renesca.parameter.StringPropertyValue
             import renesca.parameter.implicits._

             ..${ nodeTraitFactories(schema) }
             ..${ nodeFactories(schema) }
             ..${ nodeClasses(schema) }

             ..${ relationTraitFactories(schema) }
             ..${ relationFactories(schema) }
             ..${ relationClasses(schema) }

             ..${ hyperRelationFactories(schema) }
             ..${ hyperRelationClasses(schema) }

             ..${ nodeSuperTraits(schema) }
             ..${ relationSuperTraits(schema) }

             ..${ groupFactories(schema) }
             ..${ groupClasses(schema) }

             ..${ otherStatements(schema) }
           }
           """
          }
        }

        Schema
        Helpers.crashOnAsserted()
        val code = Code.schema(Schema(schemaPattern))
        code
    }
    )
  }
}
