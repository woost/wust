package model

import com.mohiva.play.silhouette.api.Identity
import common.Helpers
import renesca.schema.macros

@macros.GraphSchema
object WustSchema {
  // TODO: Type aliases for several HyperRelation combinations
  // TODO: custom local methods for NodeFactory
  // TODO: annotation for hidden defaults?

  @Node trait UuidNode {
    val uuid: String = Helpers.uuidBase64
  }
  @Node trait Timestamp {
    val timestamp: Long = System.currentTimeMillis
  }

  // Authentification
  @Graph trait Auth {List(User, LoginInfo, PasswordInfo) }
  @Node class User extends UuidNode with Identity {
    var name: String
    var email: Option[String]
    var karma: Long = 0
  }
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
  @Node class UserGroup extends UuidNode {
    var name: String
  }
  @Relation class MemberOf(startNode: User, endNode: UserGroup)


  //TODO: rename
  @Graph trait Discourse {List(User, UserGroup, Post, Tag, Scope, Connects, Inherits) }
  @Relation trait ContentRelation
  @Node trait ContentNode {//extends Hidable
    var title: Option[String]
    var description: String
  }


  // Content
  @Node trait Connectable extends Taggable with UuidNode
  @HyperRelation class Connects(startNode: Connectable, endNode: Connectable) extends Connectable with ContentRelation
  @Node trait Inheritable
  @HyperRelation class Inherits(startNode: Inheritable, endNode: Inheritable) extends ContentRelation
  // @Node trait Hidable {
  //   var visible: Boolean = true
  // }

  @Node class Post extends ContentNode with Connectable with Inheritable with Taggable


  // Actions
  @Node trait Action extends UuidNode with Timestamp
  //TODO: store content of action in action
  @HyperRelation class Created(startNode: User, endNode: ContentNode)
  @HyperRelation class Updated(startNode: User, endNode: ContentNode)
  @HyperRelation class Deleted(startNode: User, endNode: ContentNode)
  @Relation class Reviewed(startNode: User, endNode: Action)
  //TODO: TaggingAction here? was heisst das hier? worum gehts? steht doch
  //unten...tagging action
  //TODO: multidimesional voting?


  // Tags
  @Node class Tag extends ContentNode {
    var isType: Boolean = false
  }
  @Node trait Taggable extends UuidNode
  @HyperRelation class Categorizes(startNode: Tag, endNode: Taggable) extends ContentRelation
  @Relation class TaggingAction(startNode: User, endNode: Categorizes) /// HERE
  @Relation class Votes(startNode: User, endNode: Categorizes)


  // Scopes
  @Node class Scope extends ContentNode with Inheritable with Taggable {
    var isPrivate: Boolean = false
  }
  @Node trait ScopeChild extends UuidNode
  @Relation class BelongsTo(startNode: ScopeChild, endNode: Scope)
  @Relation class Owns(startNode: User, endNode: Scope)
  //TODO: Node trait ownable?
  @Relation class WriteAccess(startNode: User, endNode: Scope)
  //TODO: Node trait  writeaccessable?

}
