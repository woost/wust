package model

import com.mohiva.play.silhouette.api.Identity
import common.Helpers
import renesca.schema.macros

@macros.GraphSchema
object WustSchema {
  // TODO: Type aliases for several HyperRelation combinations
  // TODO: custom local methods for NodeFactory
  // TODO: annotation for hidden defaults?

@Group trait Discourse {List(User, UserGroup, Problem, Idea, Goal, ProArgument, ConArgument, Untyped) }
  @Group trait Auth {List(User, LoginInfo, PasswordInfo) }

  @Node trait UuidNode {
    val uuid: String = Helpers.uuidBase64
  }

  @Node class UserGroup extends UuidNode {
    var name: String
  }
  @Node class User extends UuidNode with Identity {
    var name: String
    var email: Option[String]
  }

  @Relation class MemberOf(startNode: User, endNode: UserGroup)

  @Node class LoginInfo {
    val providerID: String
    val providerKey: String
  }

  @Node class PasswordInfo {
    val hasher: String
    val password: String
    val salt: Option[String]
  }

  @Relation class HasLogin(startNode: User, endNode: LoginInfo)
  @Relation class HasPassword(startNode: LoginInfo, endNode: PasswordInfo)

  @Node trait ContentNode extends UuidNode {
    var title: String
    var description: Option[String]
  }

  @Relation trait ContentRelation

  //TODO: generate indirect neighbour-accessors based on hyperrelations
  @Node class Untyped extends ContentNode
  @Node class Goal extends ContentNode
  @Node class Problem extends ContentNode
  @Node class Idea extends ContentNode
  @Node class ProArgument extends ContentNode
  @Node class ConArgument extends ContentNode

  @Relation class Refers(startNode: ContentNode, endNode: ContentNode) extends ContentRelation

  @Relation class SubIdea(startNode: Idea, endNode: Idea) extends ContentRelation
  @Relation class SubGoal(startNode: Goal, endNode: Goal) extends ContentRelation
  @Relation class Causes(startNode: Problem, endNode: Problem) extends ContentRelation

  @Relation class Prevents(startNode: Problem, endNode: Goal) extends ContentRelation
  @HyperRelation class Solves(startNode: Idea, endNode: Problem) extends ContentRelation with UuidNode
  @HyperRelation class Achieves(startNode: Idea, endNode: Goal) extends ContentRelation with UuidNode

  @Relation class SupportsSolution(startNode: ProArgument, endNode: Solves) extends ContentRelation
  @Relation class OpposesSolution(startNode: ConArgument, endNode: Solves) extends ContentRelation
  @Relation class SupportsAchievement(startNode: ProArgument, endNode: Achieves) extends ContentRelation
  @Relation class OpposesAchievement(startNode: ConArgument, endNode: Achieves) extends ContentRelation

  @Relation class Contributes(startNode: User, endNode: ContentNode) {
    val createdAt: Long = System.currentTimeMillis
  }
}
