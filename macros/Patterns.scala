package renesca.schema.macros

import scala.reflect.macros.whitebox
import scala.language.experimental.macros
import PartialFunction._

class PatternContext[C <: whitebox.Context](val context: C) {

  import Helpers._
  import context.universe._

  object Patterns {
    implicit def typeNameToString(tn: TypeName): String = tn.toString
    implicit def termNameToString(tn: TermName): String = tn.toString
    implicit def treeToString(tn: Tree): String = tn.toString
    implicit def treeListToStringList(tnl: List[Tree]): List[String] = tnl.map(_.toString)

    case class Parameter(name: Tree, typeName: Tree, optional: Boolean, default: Option[Tree], mutable: Boolean) {
      def canEqual(other: Any): Boolean = other.isInstanceOf[Parameter]
      override def equals(other: Any): Boolean = other match {
        case that: Parameter =>
          (that canEqual this) &&
            this.name.toString == that.name.toString &&
            this.typeName.toString == that.typeName.toString &&
            this.optional == that.optional &&
            this.default.toString == that.default.toString &&
            this.mutable == that.mutable
        case _               => false
      }
      override def hashCode: Int = List(this.name.toString, this.typeName.toString, this.optional, this.default.toString, this.mutable).hashCode
      def toParamCode: Tree = this match {
        case Parameter(propertyName, propertyType, _, None, _)               => q"val ${ TermName(propertyName.toString) }:${ propertyType }"
        case Parameter(propertyName, propertyType, _, Some(defaultValue), _) => q"val ${ TermName(propertyName.toString) }:${ propertyType } = ${ defaultValue }"
      }
      def toAssignmentCode(schemaItem: Tree): Tree = this match {
        case Parameter(propertyName, propertyType, false, _, _) => q"$schemaItem.properties(${ propertyName.toString }) = $propertyName"
        case Parameter(propertyName, propertyType, true, _, _)  => q"if($propertyName.isDefined) $schemaItem.properties(${ propertyName.toString }) = $propertyName.get"
      }
    }
    case class ParameterList(parameters: List[Parameter]) {

      import ParameterList.supplementMissingParameters

      val (withDefault, nonDefault) = parameters.sortBy(_.name.toString).partition(_.default.isDefined)
      val (withDefaultOptional, withDefaultNonOptional) = withDefault.partition(_.optional)
      val ordered = nonDefault ::: withDefaultNonOptional ::: withDefaultOptional
      def toParamCode: List[List[Tree]] = List(ordered.map(_.toParamCode))
      def toAssignmentCode(schemaItem: Tree): List[Tree] = ordered.map(_.toAssignmentCode(schemaItem))
      def supplementMissingParametersOf(that: ParameterList): List[Tree] = this.nonDefault.map(_.name) ::: supplementMissingParameters(this.withDefault, that.withDefault) ::: supplementMissingParameters(this.withDefaultOptional, that.withDefaultOptional)
    }

    object ParameterList {
      def supplementMissingParameters(providerParameters: List[Parameter], receiverParameters: List[Parameter]): List[Tree] = {
        providerParameters.map(Some(_)).zipAll(receiverParameters.map(Some(_)), None, None).map {
          case (Some(mine), Some(other)) => mine.name
          case (Some(mine), None)        => mine.default.get // we know that we only handle list of default params (put into typesystem?)
          case (None, _)                 => q"" //TODO: context.abort(NoPosition, "This should never happen: Subclass has less properties than TraitFactory")
        }
      }

      def create(flatStatements: List[Tree]): ParameterList = new ParameterList(flatStatements.collect {
        case statement@(q"val $propertyName:Option[$propertyType] = $default") => Parameter(q"$propertyName", tq"Option[$propertyType]", optional = true, default = Some(q"$default"), mutable = false)
        case statement@(q"var $propertyName:Option[$propertyType] = $default") => Parameter(q"$propertyName", tq"Option[$propertyType]", optional = true, default = Some(q"$default"), mutable = true)
        case statement@(q"val $propertyName:Option[$propertyType]")            => Parameter(q"$propertyName", tq"Option[$propertyType]", optional = true, default = Some(q"None"), mutable = false)
        case statement@(q"var $propertyName:Option[$propertyType]")            => Parameter(q"$propertyName", tq"Option[$propertyType]", optional = true, default = Some(q"None"), mutable = true)
        case statement@(q"val $propertyName:$propertyType = $default")         => Parameter(q"$propertyName", q"$propertyType", optional = false, default = Some(q"$default"), mutable = false)
        case statement@(q"var $propertyName:$propertyType = $default")         => Parameter(q"$propertyName", q"$propertyType", optional = false, default = Some(q"$default"), mutable = true)
        case statement@(q"val $propertyName:$propertyType")                    => Parameter(q"$propertyName", q"$propertyType", optional = false, default = None, mutable = false)
        case statement@(q"var $propertyName:$propertyType")                    => Parameter(q"$propertyName", q"$propertyType", optional = false, default = None, mutable = true)
      })
    }

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
      def _superTypes: List[String]
      def superTypes = _superTypes diff List("scala.AnyRef")
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

    trait HasOwnFactory {
      val hasOwnFactory: Boolean
      val parameterList: ParameterList
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
          SchemaPattern(name, superTypes, statements)
      }
    }

    case class SchemaPattern(name: String, _superTypes: List[String], statements: List[Tree]) extends NamePattern with SuperTypesPattern

    object GroupPattern {
      //TODO: statements
      //TODO: extract modifier pattern
      def unapply(tree: Tree): Option[GroupPattern] = condOpt(tree) {
        case q""" $mods trait $name extends ..$superTypes { List(..$groupNodes) }""" if mods.annotations.collectFirst {
          case Apply(Select(New(Ident(TypeName("Group"))), termNames.CONSTRUCTOR), Nil) => true
          case _                                                                        => false
        }.get =>
          GroupPattern(name, superTypes, groupNodes)
        case q""" $mods trait $name extends ..$superTypes""" if mods.annotations.collectFirst {
          case Apply(Select(New(Ident(TypeName("Group"))), termNames.CONSTRUCTOR), Nil) => true
          case _                                                                        => false
        }.get =>
          GroupPattern(name, superTypes, Nil)
      }
    }

    case class GroupPattern(name: String, _superTypes: List[String], nodes: List[String]) extends NamePattern with SuperTypesPattern

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
          NodeTraitPattern(name, superTypes, statements)
      }
    }

    case class NodeTraitPattern(name: String, _superTypes: List[String], statements: List[Tree]) extends NamePattern with SuperTypesPattern with StatementsPattern

    case class NodeTrait(
                          pattern: NodeTraitPattern,
                          subNodes: List[String],
                          subRelations: List[String],
                          subHyperRelations: List[String],
                          commonHyperNodeNodeTraits: List[String],
                          commonHyperNodeRelationTraits: List[String],
                          flatStatements: List[Tree],
                          hasOwnFactory: Boolean
                          ) extends Name with SuperTypes with Statements with HasOwnFactory {
      if(pattern.superTypes.size > 1)
        context.abort(NoPosition, "Currently NodeTraits are restricted to only extend one trait")

      def commonHyperNodeNodeTraits_type = commonHyperNodeNodeTraits.map(TypeName(_))
      def commonHyperNodeRelationTraits_type = commonHyperNodeRelationTraits.map(TypeName(_))

      val parameterList = ParameterList.create(flatStatements)
    }

    object RelationTraitPattern {
      def unapply(tree: Tree): Option[RelationTraitPattern] = condOpt(tree) {
        case q""" $mods trait $name extends ..$superTypes { ..$statements } """ if mods.annotations.collectFirst {
          case Apply(Select(New(Ident(TypeName("Relation"))), termNames.CONSTRUCTOR), Nil) => true
          case _                                                                           => false
        }.get =>
          RelationTraitPattern(name, superTypes, statements)
      }
    }

    case class RelationTraitPattern(name: String, _superTypes: List[String], statements: List[Tree]) extends NamePattern with SuperTypesPattern with StatementsPattern

    case class RelationTrait(
                              pattern: RelationTraitPattern,
                              flatStatements: List[Tree],
                              hasOwnFactory: Boolean
                              ) extends Name with SuperTypes with Statements with HasOwnFactory {
      if(pattern.superTypes.size > 1)
        context.abort(NoPosition, "Currently RelationTraits are restricted to only extend one trait")

      val parameterList = ParameterList.create(flatStatements)
    }
    object NodePattern {
      def unapply(tree: Tree): Option[NodePattern] = condOpt(tree) {
        case q"""@Node class $name extends ..${superTypes} { ..$statements }""" =>
          NodePattern(name, superTypes, statements)
      }
    }

    case class NodePattern(name: String, _superTypes: List[String], statements: List[Tree]) extends NamePattern with SuperTypesPattern with StatementsPattern

    case class Node(
                     pattern: NodePattern,
                     neighbours: List[(String, String)],
                     rev_neighbours: List[(String, String)],
                     flatStatements: List[Tree],
                     traitFactoryParameterList: Option[ParameterList]
                     ) extends Name with SuperTypes with Statements {
      if(superTypes.size > 1)
        context.abort(NoPosition, "Currently Nodes are restricted to only extend one trait")

      val parameterList = ParameterList.create(flatStatements)
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
          RelationPattern(name, startNode, endNode, superTypes, statements)
      }
    }

    case class RelationPattern(name: String, startNode: String, endNode: String, _superTypes: List[String], statements: List[Tree]) extends NamePattern with StartEndNodePattern with SuperTypesPattern with StatementsPattern

    case class Relation(
                         pattern: RelationPattern,
                         flatStatements: List[Tree], // TODO: rename to flatSuperStatements (same for node etc)
                         traitFactoryParameterList: Option[ParameterList]
                         ) extends Name with StartEndNode with SuperTypes with Statements {
      if(superTypes.size > 1)
        context.abort(NoPosition, "Currently Relations are restricted to only extend one trait")

      val parameterList = ParameterList.create(flatStatements)
    }

    object HyperRelationPattern {
      def unapply(tree: Tree): Option[HyperRelationPattern] = condOpt(tree) {
        case q"""@HyperRelation class $name (startNode:$startNode, endNode:$endNode) extends ..$superTypes {..$statements}""" =>
          HyperRelationPattern(name, startNode, endNode, superTypes, statements)
      }
    }

    case class HyperRelationPattern(name: String, startNode: String, endNode: String, _superTypes: List[String], statements: List[Tree]) extends NamePattern with SuperTypesPattern with StartEndNodePattern with StatementsPattern

    case class HyperRelation(
                              pattern: HyperRelationPattern,
                              superNodeTypes: List[String],
                              superRelationTypes: List[String],
                              flatSuperStatements: List[Tree],
                              traitFactoryParameterList: Option[ParameterList]
                              ) extends Name with SuperTypes with StartEndNode with Statements with StartEndRelation {
      if(superNodeTypes.size > 1 || superRelationTypes.size > 1)
        context.abort(NoPosition, "Currently HyperRelations are restricted to only extend one trait")

      val parameterList = ParameterList.create(flatSuperStatements)
    }
  }
}
