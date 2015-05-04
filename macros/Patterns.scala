package renesca.schema.macros

import scala.reflect.macros.whitebox
import scala.language.experimental.macros
import PartialFunction._

class PatternContext[C <: whitebox.Context](val context: C) {

  import Helpers._
  import context.universe._

  object Patterns {
    trait Name {
      def pattern: NamePattern
      def name = pattern.name

      def name_type = TypeName(name)
      def name_term = TermName(name)
      def name_label = nameToLabel(name)
      def name_plural = nameToPlural(name)
      def name_plural_term = TermName(name_plural)
    }

    trait NamePattern {
      def name: String
    }

    trait SuperTypes {
      def pattern: SuperTypesPattern
      def superTypes = pattern.superTypes

      def superTypes_type = superTypes.map(TypeName(_))
    }

    trait SuperTypesPattern {
      def superTypes: List[String]
    }

    trait StartEndNode {
      def pattern: StartEndNodePattern
      def startNode = pattern.startNode
      def endNode = pattern.endNode

      def startNode_type = TypeName(startNode)
      def startNode_term = TermName(startNode)
      def endNode_type = TypeName(endNode)
      def endNode_term = TermName(endNode)
    }

    trait StartEndNodePattern {
      def startNode: String
      def endNode: String
    }

    trait StartEndRelation extends StartEndNode with Name {
      def pattern: StartEndNodePattern with NamePattern

      def startRelation = relationName(startNode, name)
      def startRelation_type = TypeName(startRelation)
      def startRelation_term = TermName(startRelation)
      def startRelation_label = nameToLabel(startRelation)
      def endRelation = relationName(name, endNode)
      def endRelation_type = TypeName(endRelation)
      def endRelation_term = TermName(endRelation)
      def endRelation_label = nameToLabel(endRelation)
    }

    trait Statements {
      def pattern: StatementsPattern
      def statements = pattern.statements
    }

    trait StatementsPattern {
      def statements: List[Tree]
    }

    object SchemaPattern {
      def unapply(tree: Tree): Option[SchemaPattern] = condOpt(tree) {
        case q""" object $name extends ..$superTypes { ..$statements } """ =>
          SchemaPattern(name.toString, superTypes.map(_.toString) diff List("scala.AnyRef"), statements)
      }
    }

    case class SchemaPattern(name: String, superTypes: List[String], statements: List[Tree]) extends NamePattern

    object GroupPattern {
      //TODO: statements
      //TODO: extract modifier pattern
      def unapply(tree: Tree): Option[GroupPattern] = condOpt(tree) {
        case q""" $mods trait $name extends ..$superTypes { List(..$groupNodes) }""" if mods.annotations.collectFirst {
          case Apply(Select(New(Ident(TypeName("Group"))), termNames.CONSTRUCTOR), Nil) => true
          case _                                                                        => false
        }.get =>
          GroupPattern(name.toString, superTypes.map { _.toString } diff List("scala.AnyRef"), groupNodes.map { _.toString })
        case q""" $mods trait $name extends ..$superTypes""" if mods.annotations.collectFirst {
          case Apply(Select(New(Ident(TypeName("Group"))), termNames.CONSTRUCTOR), Nil) => true
          case _                                                                        => false
        }.get =>
          GroupPattern(name.toString, superTypes.map { _.toString } diff List("scala.AnyRef"), Nil)
      }
    }

    case class GroupPattern(name: String, superTypes: List[String], nodes: List[String]) extends NamePattern with SuperTypesPattern

    case class Group(
                     pattern: GroupPattern,
                     nodes: List[String],
                     relations: List[String],
                     hyperRelations: List[String],
                     nodeTraits: List[NodeTrait]
                      ) extends Name with SuperTypes

    object NodeTraitPattern {
      def unapply(tree: Tree): Option[NodeTraitPattern] = condOpt(tree) {
        //http://stackoverflow.com/questions/26305528/scala-annotations-are-not-found
        case q""" $mods trait $name extends ..$superTypes { ..$statements } """ if mods.annotations.collectFirst {
          case Apply(Select(New(Ident(TypeName("Node"))), termNames.CONSTRUCTOR), Nil) => true
          case _                                                                       => false
        }.get =>
          NodeTraitPattern(name.toString, superTypes.map { _.toString } diff List("scala.AnyRef"), statements)
      }
    }

    case class NodeTraitPattern(name: String, superTypes: List[String], statements: List[Tree]) extends NamePattern with SuperTypesPattern with StatementsPattern

    case class NodeTrait(
                         pattern: NodeTraitPattern,
                         override val superTypes: List[String],
                         subNodes: List[String],
                         subRelations: List[String],
                         subHyperRelations: List[String],
                         commonHyperNodeTraits: List[String],
                         flatStatements: List[Tree],
                         hasOwnFactory: Boolean
                          ) extends Name with SuperTypes with Statements {
      def commonHyperNodeTraits_type = commonHyperNodeTraits.map(TypeName(_))
    }

    object RelationTraitPattern {
      def unapply(tree: Tree): Option[RelationTraitPattern] = condOpt(tree) {
        case q""" $mods trait $name extends ..$superTypes { ..$statements } """ if mods.annotations.collectFirst {
          case Apply(Select(New(Ident(TypeName("Relation"))), termNames.CONSTRUCTOR), Nil) => true
          case _                                                                           => false
        }.get =>
          RelationTraitPattern(name.toString, superTypes.map { _.toString } diff List("scala.AnyRef"), statements)
      }
    }

    case class RelationTraitPattern(name: String, superTypes: List[String], statements: List[Tree]) extends NamePattern with SuperTypesPattern with StatementsPattern

    case class RelationTrait(pattern: RelationTraitPattern, flatStatements: List[Tree], hasOwnFactory: Boolean) extends Name with SuperTypes with Statements {
      if(pattern.superTypes.size > 1)
        context.abort(NoPosition, "Currently RelationTraits are restricted to only extend one trait")
    }

    object NodePattern {
      def unapply(tree: Tree): Option[NodePattern] = condOpt(tree) {
        case q"""@Node class $name extends ..${superTypes} { ..$statements }""" =>
          NodePattern(name.toString, superTypes.map { _.toString } diff List("scala.AnyRef"), statements)
      }
    }

    case class NodePattern(name: String, superTypes: List[String], statements: List[Tree]) extends NamePattern with SuperTypesPattern with StatementsPattern

    case class Node(
                     pattern: NodePattern,
                     superTypesFlatStatementsCount: Int,
                     neighbours: List[(String, String)],
                     rev_neighbours: List[(String, String)],
                     flatStatements: List[Tree]
                     ) extends Name with SuperTypes with Statements {
      if(superTypes.size > 1)
        context.abort(NoPosition, "Currently nodes are restricted to only extend one trait")

      def neighbours_terms = neighbours.map { case (relation, endNode) =>
        (TermName(nameToPlural(relation)), TypeName(endNode), TermName(endNode))
      }
      def rev_neighbours_terms = rev_neighbours.map { case (relation, startNode) =>
        (TermName(rev(nameToPlural(relation))), TypeName(startNode), TermName(startNode))
      }
    }

    object RelationPattern {
      def unapply(tree: Tree): Option[RelationPattern] = condOpt(tree) {
        case q"""@Relation class $name (startNode:$startNode, endNode:$endNode) extends ..$superTypes {..$statements}""" =>
          RelationPattern(name.toString, startNode.toString, endNode.toString, superTypes.map { _.toString } diff List("scala.AnyRef"), statements)
      }
    }

    case class RelationPattern(name: String, startNode: String, endNode: String, superTypes: List[String], statements: List[Tree]) extends NamePattern with StartEndNodePattern with SuperTypesPattern with StatementsPattern

    case class Relation(pattern: RelationPattern, flatStatements:List[Tree]) extends Name with StartEndNode with SuperTypes with Statements {
      if(superTypes.size > 1)
        context.abort(NoPosition, "Currently Relations are restricted to only extend one trait")
    }

    object HyperRelationPattern {
      def unapply(tree: Tree): Option[HyperRelationPattern] = condOpt(tree) {
        case q"""@HyperRelation class $name (startNode:$startNode, endNode:$endNode) extends ..$superTypes {..$statements}""" =>
          HyperRelationPattern(name.toString, startNode.toString, endNode.toString, superTypes.map { _.toString } diff List("scala.AnyRef"), statements)
      }
    }

    case class HyperRelationPattern(name: String, startNode: String, endNode: String, superTypes: List[String], statements: List[Tree]) extends NamePattern with SuperTypesPattern with StartEndNodePattern with StatementsPattern

    case class HyperRelation(pattern: HyperRelationPattern, superNodeTypes: List[String], superRelationTypes: List[String]) extends Name with SuperTypes with StartEndNode with Statements with StartEndRelation {
      if(superNodeTypes.size > 1 || superRelationTypes.size > 1)
        context.abort(NoPosition, "Currently HyperRelations are restricted to only extend one trait")
    }
  }
}
