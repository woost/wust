package renesca.schema.macros

import scala.reflect.macros.whitebox
import scala.language.experimental.macros
import PartialFunction._

class PatternContext[C <: whitebox.Context](val context: C) {
  import Helpers._
  import context.universe._

  object Patterns {
    trait NamePattern {
      def name: String
      //      def name_type = TypeName(name)
      //      def name_term = TermName(name)
      //      def name_label = nameToLabel(name)
      //      def name_plural = nameToPlural(name)
      //      def name_plural_term = TermName(name_plural)
    }
    trait SuperTypesPattern {
      def superTypes: List[String]
      //      def superTypes_type = superTypes.map(TypeName(_))
    }
    trait StartEndNodePattern {
      def startNode: String
      def endNode: String
      //      def startNode_type = TypeName(startNode)
      //      def startNode_term = TermName(startNode)
      //      def endNode_type = TypeName(endNode)
      //      def endNode_term = TermName(endNode)
    }
    trait StatementsPattern {
      def statements: List[Tree]
    }

    object SchemaPatternTree {
      def unapply(tree: Tree): Option[SchemaPattern] = condOpt(tree) {
        case q""" object $name extends ..$superTypes { ..$statements } """ =>
          SchemaPattern(name.toString, superTypes.map(_.toString) diff List("scala.AnyRef"), statements)
      }
    }
    case class SchemaPattern(name: String, superTypes: List[String], statements: List[Tree]) extends NamePattern {
      def name_type = TypeName(name)
      def name_term = TermName(name)
      def name_label = nameToLabel(name)
      def name_plural = nameToPlural(name)
      def name_plural_term = TermName(name_plural)
    }

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
    case class GroupPattern(name: String, superTypes: List[String], nodes: List[String]) extends NamePattern with SuperTypesPattern {
      def name_type = TypeName(name)
      def name_term = TermName(name)
      def name_label = nameToLabel(name)
      def name_plural = nameToPlural(name)
      def name_plural_term = TermName(name_plural)
      def superTypes_type = superTypes.map(TypeName(_))
    }

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
    case class NodeTraitPattern(name: String, superTypes: List[String], statements: List[Tree]) extends NamePattern with SuperTypesPattern with StatementsPattern {
      def name_type = TypeName(name)
      def name_term = TermName(name)
      def name_label = nameToLabel(name)
      def name_plural = nameToPlural(name)
      def name_plural_term = TermName(name_plural)
      def superTypes_type = superTypes.map(TypeName(_))
    }

    object NodePattern {
      def unapply(tree: Tree): Option[NodePattern] = condOpt(tree) {
        case q"""@Node class $name extends ..${superTypes} { ..$statements }""" =>
          NodePattern(name.toString, superTypes.map { _.toString } diff List("scala.AnyRef"), statements)
      }
    }
    case class NodePattern(name: String, superTypes: List[String], statements: List[Tree]) extends NamePattern with SuperTypesPattern with StatementsPattern {
      def name_type = TypeName(name)
      def name_term = TermName(name)
      def name_label = nameToLabel(name)
      def name_plural = nameToPlural(name)
      def name_plural_term = TermName(name_plural)
    }

    object RelationPattern {
      def unapply(tree: Tree): Option[Relation] = condOpt(tree) {
        case q"""@Relation class $name (startNode:$startNode, endNode:$endNode) {..$statements}""" =>
          Relation(name.toString, startNode.toString, endNode.toString, statements)
      }
    }
    case class Relation(name: String, startNode: String, endNode: String, statements: List[Tree]) extends NamePattern with StartEndNodePattern with StatementsPattern {
      def name_type = TypeName(name)
      def name_term = TermName(name)
      def name_label = nameToLabel(name)
      def name_plural = nameToPlural(name)
      def name_plural_term = TermName(name_plural)
      def startNode_type = TypeName(startNode)
      def startNode_term = TermName(startNode)
      def endNode_type = TypeName(endNode)
      def endNode_term = TermName(endNode)
    }

    object HyperRelationPattern {
      def unapply(tree: Tree): Option[HyperRelation] = condOpt(tree) {
        case q"""@HyperRelation class $name (startNode:$startNode, endNode:$endNode) extends ..$superTypes {..$statements}""" =>
          HyperRelation(name.toString, startNode.toString, endNode.toString, superTypes.map { _.toString } diff List("scala.AnyRef"), statements)
      }
    }
    case class HyperRelation(name: String, startNode: String, endNode: String, superTypes: List[String], statements: List[Tree]) extends NamePattern with SuperTypesPattern with StartEndNodePattern with StatementsPattern {
      def name_type = TypeName(name)
      def name_term = TermName(name)
      def name_label = nameToLabel(name)
      def name_plural = nameToPlural(name)
      def name_plural_term = TermName(name_plural)
      def startNode_type = TypeName(startNode)
      def startNode_term = TermName(startNode)
      def endNode_type = TypeName(endNode)
      def endNode_term = TermName(endNode)
      def superTypes_type = superTypes.map(TypeName(_))
      def startRelation = relationName(startNode, name)
      def startRelation_type = TypeName(startRelation)
      def startRelation_term = TermName(startRelation)
      def startRelation_label = nameToLabel(startRelation)
      def endRelation = relationName(name, endNode)
      def endRelation_type = TypeName(endRelation)
      def endRelation_term = TermName(endRelation)
      def endRelation_label = nameToLabel(endRelation)
    }

    case class Node(
                      name: String,
                      superTypes: List[String],
                      superTypesFlatStatementsCount: Int,
                      neighbours: List[(String, String)],
                      rev_neighbours: List[(String, String)],
                      statements: List[Tree],
                      flatStatements: List[Tree]
                      ) extends NamePattern with SuperTypesPattern {
      if(superTypes.size > 1)
        context.abort(NoPosition, "Currently nodes are restricted to only extend one trait")

      def name_type = TypeName(name)
      def name_term = TermName(name)
      def name_label = nameToLabel(name)
      def name_plural = nameToPlural(name)
      def name_plural_term = TermName(name_plural)
      def superTypes_type = superTypes.map(TypeName(_))
      def neighbours_terms = neighbours.map { case (relation, endNode) =>
        (TermName(nameToPlural(relation)), TypeName(endNode), TermName(endNode))
      }
      def rev_neighbours_terms = rev_neighbours.map { case (relation, startNode) =>
        (TermName(rev(nameToPlural(relation))), TypeName(startNode), TermName(startNode))
      }

    }

    case class NodeTrait(name: String,
                          superTypes: List[String],
                          subNodes: List[String],
                          subRelations: List[String],
                          subHyperRelations: List[String],
                          commonHyperNodeTraits: List[String],
                          statements: List[Tree],
                          flatStatements: List[Tree],
                          hasOwnFactory: Boolean
                          ) extends NamePattern with SuperTypesPattern {
      def name_type = TypeName(name)
      def name_term = TermName(name)
      def name_label = nameToLabel(name)
      def name_plural = nameToPlural(name)
      def name_plural_term = TermName(name_plural)
      def commonHyperNodeTraits_type = commonHyperNodeTraits.map(TypeName(_))
      def superTypes_type = superTypes.map(TypeName(_))
    }
    case class Group(name: String,
                      nodes: List[String],
                      relations: List[String],
                      hyperRelations: List[String],
                      nodeTraits: List[NodeTrait]
                      ) extends NamePattern {
      def name_type = TypeName(name)
      def name_term = TermName(name)
      def name_label = nameToLabel(name)
      def name_plural = nameToPlural(name)
      def name_plural_term = TermName(name_plural)

    }
  }
}
