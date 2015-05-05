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
  import patternContext.Patterns

  object Schemas {

    object NodeTrait {

      import Schema._

      def apply(
                 nodeTraitPattern: NodeTraitPattern,
                 nodeTraitPatterns: List[NodeTraitPattern],
                 relationTraitPatterns: List[RelationTraitPattern],
                 selectedNodePatterns: List[NodePattern],
                 relationPatterns: List[RelationPattern],
                 hyperRelationPatterns: List[HyperRelationPattern],
                 hasOwnFactory: Boolean
                 ) = {
        val nodes = nodeTraitToNodes(nodeTraitPatterns, selectedNodePatterns ::: hyperRelationPatterns, nodeTraitPattern)
        new NodeTrait(
          nodeTraitPattern,
          superTypes = nodeTraitPattern.superTypes,
          subNodes = nodes,
          subRelations = nodeNamesToRelations(nodes, relationPatterns).map(_.name),
          subHyperRelations = nodeNamesToRelations(nodes, hyperRelationPatterns).map(_.name),
          commonHyperNodeNodeTraits = nodeTraitToCommonHyperNodeTraits(nodeTraitPatterns, nodeTraitPatterns, selectedNodePatterns, hyperRelationPatterns, nodeTraitPattern),
          commonHyperNodeRelationTraits = nodeTraitToCommonHyperNodeTraits(nodeTraitPatterns, relationTraitPatterns, selectedNodePatterns, hyperRelationPatterns, nodeTraitPattern),
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

        val nodeTraits = nodeTraitPatterns.map(nodeTraitPattern =>
          NodeTrait(nodeTraitPattern, nodeTraitPatterns, relationTraitPatterns, nodePatterns, relationPatterns, hyperRelationPatterns, traitCanHaveOwnFactory(nodePatterns ::: hyperRelationPatterns ::: relationTraitPatterns ::: nodeTraitPatterns, nodeTraitPattern)))
        val nodes = nodePatterns.map { nodePattern => {
          import nodePattern._
          Node(nodePattern, neighbours(nodePattern, relationPatterns), rev_neighbours(nodePattern, relationPatterns),
            flatSuperStatements(nodeTraitPatterns, nodePattern), findSuperFactoryParameterList(nodeTraitPatterns, nodePattern, nodeTraits))
        }}
        val relationTraits = relationTraitPatterns.map(relationTraitPattern =>
          RelationTrait(relationTraitPattern,
            flatSuperStatements(relationTraitPatterns, relationTraitPattern),
            traitCanHaveOwnFactory(relationPatterns ::: hyperRelationPatterns ::: nodeTraitPatterns ::: relationTraitPatterns, relationTraitPattern))) //TODO: why nodeTraitPatterns
        val groups = groupPatterns.map(groupPattern =>
            Group(groupPattern,
              nodes = groupToNodes(groupPatterns, groupPattern),
              relations = groupToRelations(groupPatterns, hyperRelationPatterns, relationPatterns, groupPattern),
              hyperRelations = groupToRelations(groupPatterns, hyperRelationPatterns, hyperRelationPatterns, groupPattern),
              nodeTraits = nodeTraitPatterns.map(nodeTraitPattern =>
                NodeTrait(nodeTraitPattern, nodeTraitPatterns, relationTraitPatterns, groupToNodes(groupPatterns, groupPattern).map(nameToPattern(nodePatterns, _)), relationPatterns, hyperRelationPatterns, false))
            )
          )
        val hyperRelations = hyperRelationPatterns.map(hyperRelationPattern => HyperRelation(hyperRelationPattern, filterSuperTypes(nodeTraitPatterns, hyperRelationPattern), filterSuperTypes(relationTraitPatterns, hyperRelationPattern), flatSuperStatements(nodeTraitPatterns ::: relationTraitPatterns, hyperRelationPattern), findSuperFactoryParameterList(nodeTraitPatterns ::: relationTraitPatterns, hyperRelationPattern, relationTraits)))
        val relations = relationPatterns.map(relationPattern => Relation(relationPattern, flatSuperStatements(relationTraitPatterns, relationPattern), findSuperFactoryParameterList(relationTraitPatterns, relationPattern, relationTraits)))
        Schema(name, superTypes, nodes, relations, hyperRelations, nodeTraits, relationTraits, groups, statements)
      }

      def findSuperFactoryParameterList[P <: NamePattern with SuperTypesPattern, Q <: Patterns.Name with HasOwnFactory](patterns: List[_ <: P], pattern: P, nameClasses: List[Q]): Option[ParameterList] = patternToNameClasses(patternToFlatSuperTypes(patterns, pattern), nameClasses).find(_.hasOwnFactory).map(_.parameterList)

      def patternToNameClasses[P <: Patterns.Name with HasOwnFactory](patterns: List[_ <: NamePattern], nameClasses: List[P]): List[P] = nameClasses.filter(nameClass => patterns.map(_.name).contains(nameClass.name))

      def filterSuperTypes(patterns: List[_ <: NamePattern], pattern: SuperTypesPattern): List[String] = {
        pattern.superTypes intersect patterns.map(_.name)
      }
      def flatSuperStatements[P <: NamePattern with SuperTypesPattern with StatementsPattern](superTypePatterns: List[NamePattern with SuperTypesPattern with StatementsPattern], pattern: P): List[Tree] = {
        val superTypes: List[StatementsPattern with NamePattern with SuperTypesPattern] = pattern.superTypes.map(superType => nameToPattern(superTypePatterns, superType))
        val flatSuperTypes: List[StatementsPattern] = pattern :: patternToFlatSuperTypes(superTypePatterns, pattern)
        flatSuperTypes.flatMap(_.statements)
      }
      val testNode = NodePattern("a", List("superx"), List(q"def title:String"))
      assertX(flatSuperStatements(
        List(
          NodeTraitPattern("superx", List("supery"), List(q"def titlex:String")),
          NodeTraitPattern("supery", Nil, List(q"def titley:String")),
          testNode
        ), testNode).map(_.toString).toSet,
        List(q"def titlex:String", q"def title:String", q"def titley:String").map(_.toString).toSet)
      def nameToPattern[P <: NamePattern](patterns: List[P], name: String): P = patterns.find(_.name == name).get
      def neighbours(nodePattern: NodePattern, relations: List[RelationPattern]): List[(String, String)] = relations.filter(_.startNode == nodePattern.name).map(r => r.name -> r.endNode)
      def rev_neighbours(nodePattern: NodePattern, relations: List[RelationPattern]): List[(String, String)] = relations.filter(_.endNode == nodePattern.name).map(r => r.name -> r.startNode)
      def isDeepSuperType[P <: NamePattern with SuperTypesPattern](patterns: List[P], subPattern: P, superPattern: P): Boolean = {
        // assert(subPattern.superTypes.forall(superType => patterns.map(_.name) contains superType), s"${ subPattern.superTypes } ## NOT IN ## ${ patterns.map(_.name) }")
        subPattern.superTypes match {
          case Nil        => false
          case superTypes => superTypes.exists { name =>
            superPattern.name == name || (patterns.exists(_.name == name) && isDeepSuperType(patterns, nameToPattern(patterns, name), superPattern))
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
      def patternToFlatSuperTypes[P <: NamePattern with SuperTypesPattern, SUPER <: P](patterns: List[SUPER], pattern: P): List[SUPER] = patterns.filter { superPattern =>
        isDeepSuperType(patterns, pattern, superPattern)
      }
      def patternToSubTypes[P <: NamePattern with SuperTypesPattern](patterns: List[P], pattern: P): List[P] = patterns.filter(_.superTypes.contains(pattern.name))
      def patternToFlatSubTypes[P <: NamePattern with SuperTypesPattern, SUB <: P](patterns: List[SUB], pattern: P): List[SUB] = patterns.filter { subPattern =>
        isDeepSuperType(patterns, subPattern, pattern)
      }

      def nodeTraitToNodes(nodeTraits: List[NodeTraitPattern], nodePatterns: List[NamePattern with SuperTypesPattern], nodeTrait: NodeTraitPattern): List[String] = {
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

      def nodeTraitToCommonHyperNodeTraits[P <: NamePattern with SuperTypesPattern](nodeTraitPatterns: List[NodeTraitPattern], middleNodeTraitPatterns: List[P], nodePatterns: List[NodePattern], hyperRelationPatterns: List[HyperRelationPattern], nodeTrait: NodeTraitPattern): List[String] = {
        val nodes = nodeTraitToNodes(nodeTraitPatterns, nodePatterns ::: hyperRelationPatterns, nodeTrait)
        val subHyperRelations = nodeNamesToRelations(nodes, hyperRelationPatterns)
        val flatSuperTypes: List[List[String]] = subHyperRelations.map(hyperRelation => patternToFlatSuperTypes(middleNodeTraitPatterns, hyperRelation).map(_.name))
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

      def groupToRelations(groupPatterns: List[GroupPattern], hyperRelationPatterns: List[HyperRelationPattern], relations: List[NamePattern with StartEndNodePattern], groupPattern: GroupPattern): List[String] =
        nodeNamesToRelations(groupToNodes(groupPatterns, groupPattern) ::: hyperRelationPatterns.map(_.name), relations).map(_.name)

      def traitCanHaveOwnFactory(hierarchyPatterns: List[NamePattern with SuperTypesPattern with StatementsPattern], currentTrait: NamePattern with SuperTypesPattern): Boolean = {
        val children = patternToFlatSubTypes(hierarchyPatterns, currentTrait)
        // if we currently are at NodeTrait, we need to check whether one of its
        // children is a HyperRelation. If that is the case, a factory cannot be
        // generated, as the HyperRelation additionally needs Start-/EndNode in
        // its local method.
        val isNodeTrait = currentTrait.isInstanceOf[NodeTraitPattern]
        val hasHyperRelationChild = children.exists(_.isInstanceOf[HyperRelationPattern])
        if (isNodeTrait && hasHyperRelationChild)
          return false

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
