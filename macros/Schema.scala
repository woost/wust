package renesca.schema.macros

import scala.reflect.macros.whitebox
import scala.language.experimental.macros
import PartialFunction._
import scala.annotation.StaticAnnotation

class SchemaContext[C <: whitebox.Context](val context: C) {
  //TODO: abort when wrong superType inheritance. example: Relation extends NodeTrait
  val patternContext = new PatternContext[context.type](context)

  import Helpers._
  import context.universe._

  import patternContext.Patterns._

  object Schemas {

    object NodeTrait {

      import Schema._

      def apply(
                 nodeTraitPatterns: List[NodeTraitPattern],
                 selectedNodePatterns: List[NodePattern],
                 relationPatterns: List[RelationPattern],
                 hyperRelationPatterns: List[HyperRelationPattern],
                 nodeTraitPattern: NodeTraitPattern,
                 hasOwnFactory: Boolean
                 ) = {
        new NodeTrait(
          name = nodeTraitPattern.name,
          superTypes = if(nodeTraitPattern.superTypes.nonEmpty) nodeTraitPattern.superTypes else List("SchemaNode"),
          subNodes = nodeTraitToNodes(nodeTraitPatterns, selectedNodePatterns, nodeTraitPattern),
          subRelations = nodeNamesToRelations(nodeTraitToNodes(nodeTraitPatterns, selectedNodePatterns, nodeTraitPattern), relationPatterns).map(_.name),
          subHyperRelations = nodeNamesToRelations(nodeTraitToNodes(nodeTraitPatterns, selectedNodePatterns, nodeTraitPattern), hyperRelationPatterns).map(_.name),
          commonHyperNodeTraits = nodeTraitToCommonHyperNodeTraits(nodeTraitPatterns, selectedNodePatterns, hyperRelationPatterns, nodeTraitPattern),
          statements = nodeTraitPattern.statements,
          flatStatements = flatSuperStatements(nodeTraitPatterns, nodeTraitPattern),
          hasOwnFactory = hasOwnFactory
        )
      }
    }
    object Schema {
      def apply(schema: SchemaPattern): Schema = {
        import schema._
        val nodePatterns: List[NodePattern] = schema.statements.collect { case NodePattern(node) => node }
        val relationPatterns: List[RelationPattern] = schema.statements.collect { case RelationPattern(relationPattern) => relationPattern }
        val hyperRelationPatterns: List[HyperRelationPattern] = schema.statements.collect { case HyperRelationPattern(hyperRelationPattern) => hyperRelationPattern }
        val nodeTraitPatterns: List[NodeTraitPattern] = schema.statements.collect { case NodeTraitPattern(nodeTraitpattern) => nodeTraitpattern }
        val relationTraitPatterns: List[RelationTraitPattern] = schema.statements.collect { case RelationTraitPattern(nodeTraitpattern) => nodeTraitpattern }
        val groupPatterns: List[GroupPattern] = schema.statements.collect { case GroupPattern(groupPattern) => groupPattern }

        val nodes = nodePatterns.map { nodePattern => {
          import nodePattern._
          Node(name, superTypes,
            superTypes.headOption.map(superType => flatSuperStatements(nodeTraitPatterns, nameToPattern(nodeTraitPatterns, superType)).size).getOrElse(0),
            neighbours(nodePattern, relationPatterns), rev_neighbours(nodePattern, relationPatterns),
            statements, flatSuperStatements(nodeTraitPatterns, nodePattern))
        }
        }
        val nodeTraits = nodeTraitPatterns.map(nodeTraitPattern => NodeTrait(nodeTraitPatterns, nodePatterns, relationPatterns, hyperRelationPatterns, nodeTraitPattern, traitCanHaveOwnFactory(nodePatterns ::: nodeTraitPatterns, nodeTraitPattern)))
        val relationTraits = relationTraitPatterns.map(relationTraitPattern => RelationTrait(relationTraitPattern, flatSuperStatements(relationTraitPatterns, relationTraitPattern), traitCanHaveOwnFactory(relationPatterns ::: relationTraitPatterns, relationTraitPattern)))
        val groups = groupPatterns.map(groupPattern =>
          Group(groupPattern.name,
            nodes = groupToNodes(groupPatterns, groupPattern),
            relations = groupToRelations(groupPatterns, relationPatterns, groupPattern),
            hyperRelations = groupToRelations(groupPatterns, hyperRelationPatterns, groupPattern),
            nodeTraits = nodeTraitPatterns.map(nodeTraitPattern =>
              NodeTrait(nodeTraitPatterns, groupToNodes(groupPatterns, groupPattern).map(nameToPattern(nodePatterns, _)), relationPatterns, hyperRelationPatterns, nodeTraitPattern, false))
          )
        )
        val hyperRelations = hyperRelationPatterns.map(hyperRelationPattern => HyperRelation(hyperRelationPattern, filterSuperTypes(nodeTraitPatterns, hyperRelationPattern), filterSuperTypes(relationTraitPatterns, hyperRelationPattern)))
        val relations = relationPatterns.map(relationPattern => Relation(relationPattern, flatSuperStatements(relationTraitPatterns, relationPattern)))
        Schema(name, superTypes, nodes, relations, hyperRelations, nodeTraits, relationTraits, groups, statements)
      }


      def filterSuperTypes(patterns: List[_ <: NamePattern], pattern: SuperTypesPattern): List[String] = {
        pattern.superTypes intersect patterns.map(_.name)
      }
      def flatSuperStatements[P <: NamePattern with SuperTypesPattern with StatementsPattern](nodeTraitPatterns: List[NamePattern with SuperTypesPattern with StatementsPattern], nodePattern: P): List[Tree] = {
        val superTypes: List[StatementsPattern with NamePattern with SuperTypesPattern] = nodePattern.superTypes.map(superType => nameToPattern(nodeTraitPatterns, superType))
        val flatSuperTypes: List[StatementsPattern] = nodePattern :: patternToFlatSuperTypes(nodeTraitPatterns, nodePattern)
        flatSuperTypes.flatMap(_.statements)
      }
      assertX(flatSuperStatements(
        List(
          NodeTraitPattern("superx", List("supery"), List(q"def titlex:String")),
          NodeTraitPattern("supery", Nil, List(q"def titley:String"))
        ), NodePattern("a", List("superx"), List(q"def title:String"))).map(_.toString).toSet,
        List(q"def titlex:String", q"def title:String", q"def titley:String").map(_.toString).toSet)
      def nameToPattern[P <: NamePattern](patterns: List[P], name: String): P = patterns.find(_.name == name).get
      def neighbours(nodePattern: NodePattern, relations: List[RelationPattern]): List[(String, String)] = relations.filter(_.startNode == nodePattern.name).map(r => r.name -> r.endNode)
      def rev_neighbours(nodePattern: NodePattern, relations: List[RelationPattern]): List[(String, String)] = relations.filter(_.endNode == nodePattern.name).map(r => r.name -> r.startNode)
      def isDeepSuperType[P <: NamePattern with SuperTypesPattern](patterns: List[P], subPattern: P, superPattern: P): Boolean = {
        subPattern.superTypes match {
          case Nil        => false
          case superTypes => superTypes.exists { name =>
            superPattern.name == name || isDeepSuperType(patterns, nameToPattern(patterns, name), superPattern)
          }
        }
      }
      assertX(isDeepSuperType(List(
        NodeTraitPattern("superx", List("supery"), Nil),
        NodeTraitPattern("supery", List("superz"), Nil),
        NodeTraitPattern("superz", Nil, Nil)
      ), subPattern = NodeTraitPattern("superx", List("supery"), Nil),
        superPattern = NodeTraitPattern("superz", Nil, Nil)), true)

      def patternToSuperTypes[P <: NamePattern with SuperTypesPattern](patterns: List[P], pattern: P): List[P] = pattern.superTypes.map(nameToPattern(patterns, _))
      def patternToFlatSuperTypes[P <: NamePattern with SuperTypesPattern](patterns: List[P], pattern: P): List[P] = patterns.filter { superPattern =>
        isDeepSuperType(patterns, pattern, superPattern)
      }
      def patternToSubTypes[P <: NamePattern with SuperTypesPattern](patterns: List[P], pattern: P): List[P] = patterns.filter(_.superTypes.contains(pattern.name))
      def patternToFlatSubTypes[P <: NamePattern with SuperTypesPattern, SUB <: P](patterns: List[SUB], pattern: P): List[SUB] = patterns.filter { subPattern =>
        isDeepSuperType(patterns, subPattern, pattern)
      }

      def nodeTraitToNodes(nodeTraits: List[NodeTraitPattern], nodePatterns: List[NodePattern], nodeTrait: NodeTraitPattern): List[String] = {
        (nodeTrait :: patternToFlatSubTypes(nodeTraits, nodeTrait)).flatMap(subTrait => nodePatterns.filter(_.superTypes contains subTrait.name)).distinct.map(_.name)
      }
      assertX(nodeTraitToNodes(
        List(
          NodeTraitPattern("traitA", Nil, Nil),
          NodeTraitPattern("traitB", List("traitA"), Nil),
          NodeTraitPattern("traitC", Nil, Nil)
        ),
        List(
          NodePattern("nodeC", List("traitA"), Nil),
          NodePattern("nodeD", List("traitB"), Nil)
        ),
        NodeTraitPattern("traitA", Nil, Nil)
      ), List("nodeC", "nodeD"))

      def nodeNamesToRelations[R <: StartEndNodePattern](nodeNames: List[String], relations: List[R]): List[R] = {
        relations.filter(relation => nodeNames.contains(relation.startNode) && nodeNames.contains(relation.endNode))
      }

      def nodeTraitToCommonHyperNodeTraits(nodeTraitPatterns: List[NodeTraitPattern], nodePatterns: List[NodePattern], hyperRelations: List[HyperRelationPattern], nodeTrait: NodeTraitPattern): List[String] = {
        val nodes = nodeTraitToNodes(nodeTraitPatterns, nodePatterns, nodeTrait)
        val subHyperRelations = nodeNamesToRelations(nodes, hyperRelations)
        val flatSuperTypes: List[List[String]] = subHyperRelations.map(hyperRelation => patternToFlatSuperTypes(nodeTraitPatterns, hyperRelation).map(_.name))
        if(flatSuperTypes.isEmpty) Nil
        else if(flatSuperTypes.size == 1) flatSuperTypes.head
        else flatSuperTypes.reduce(_ intersect _)
      }
      assertX(nodeTraitToCommonHyperNodeTraits(
        List(
          NodeTraitPattern("traitA", Nil, Nil),
          NodeTraitPattern("traitB", Nil, Nil),
          NodeTraitPattern("traitC", Nil, Nil)
        ),
        List(
          NodePattern("nodeC", List("traitA"), Nil),
          NodePattern("nodeD", List("traitA"), Nil)
        ),
        List(
          HyperRelationPattern("hyperX", "nodeC", "nodeD", List("traitB", "traitC"), Nil),
          HyperRelationPattern("hyperY", "nodeD", "nodeC", List("traitA", "traitB", "traitC"), Nil)
        ),
        NodeTraitPattern("traitA", Nil, Nil)
      ).toSet, List("traitB", "traitC").toSet)

      def groupToNodes(groupPatterns: List[GroupPattern], groupPattern: GroupPattern): List[String] = (groupPattern :: patternToFlatSubTypes(groupPatterns, groupPattern)).flatMap(_.nodes)
      assertX(groupToNodes(List(
        GroupPattern("groupA", Nil, List("nodeA", "nodeB")),
        GroupPattern("groupB", List("groupA"), List("nodeC", "nodeD"))
      ), GroupPattern("groupA", Nil, List("nodeA", "nodeB"))).toSet, List("nodeA", "nodeB", "nodeC", "nodeD").toSet)

      def groupToRelations(groupPatterns: List[GroupPattern], relations: List[NamePattern with StartEndNodePattern], groupPattern: GroupPattern): List[String] =
        nodeNamesToRelations(groupToNodes(groupPatterns, groupPattern), relations).map(_.name)

      def traitCanHaveOwnFactory(hierarchyPatterns: List[NamePattern with SuperTypesPattern with StatementsPattern], currentTrait: NamePattern with SuperTypesPattern): Boolean = {
        val children = patternToFlatSubTypes(hierarchyPatterns, currentTrait)
        val statements = children.flatMap(_.statements)
        statements.find {
          case q"val $x:Option[$propertyType]" => false
          case q"var $x:Option[$propertyType]" => false
          case q"val $x:$propertyType = $y"    => false
          case q"var $x:$propertyType = $y"    => false
          case q"val $x:$propertyType"         => true
          case q"var $x:$propertyType"         => true
          case _                               => false
        }.isEmpty
      }
    }

    case class Schema(
                       name: String,
                       superTypes: List[String],
                       nodes: List[Node],
                       relations: List[Relation],
                       hyperRelations: List[HyperRelation],
                       nodeTraits: List[NodeTrait],
                       relationTraits: List[RelationTrait],
                       groups: List[Group],
                       statements: List[Tree]
                       ) extends NamePattern with SuperTypesPattern {

      def name_type = TypeName(name)
      def name_term = TermName(name)
      def name_label = nameToLabel(name)
      def name_plural = nameToPlural(name)
      def name_plural_term = TermName(name_plural)

      def superTypes_type = superTypes.map(TypeName(_))
    }
  }
}
