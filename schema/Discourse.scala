package model

import com.mohiva.play.silhouette.api.Identity
import renesca.schema.macros

@macros.GraphSchema
object WustSchema {
  // TODO: Type aliases for several HyperRelation combinations
  // TODO: custom local methods for NodeFactory
  // TODO: annotation for hidden defaults?

  @Node trait UuidNode {
    @unique val uuid: String = Helpers.uuidBase64
  }

  @Node trait Timestamp {
    val timestamp: Long = System.currentTimeMillis
  }

  // Authentification
  @Graph trait Auth {Nodes(User, LoginInfo, PasswordInfo) }
  @Node class User extends UuidNode with Identity {
    var name: String
    var email: Option[String]
    var karma: Long = 0
  }
  @Node class LoginInfo {
    val providerID: String
    @unique val providerKey: String
  }

  @Node class PasswordInfo {
    val hasher: String
    val password: String
    val salt: Option[String]
  }
  @Relation class HasLogin(startNode: User, endNode: LoginInfo)
  @Relation class HasPassword(startNode: LoginInfo, endNode: PasswordInfo)
  @Node class UserGroup extends UuidNode {
    @unique val name: String
  }
  @Relation class MemberOf(startNode: User, endNode: UserGroup)

  //TODO: rename
  @Graph trait Discourse {Nodes(User, UserGroup, Post, Tag, Scope) }
  @Relation trait ContentRelation
  @Relation trait HyperConnection
  @Node trait ContentNode extends UuidNode

  // Content
  @Node trait Connectable extends UuidNode with Taggable
  @HyperRelation class Connects(startNode: Connectable, endNode: Connectable) extends Connectable with ContentRelation with HyperConnection with UuidNode
  @Node trait Inheritable
  @HyperRelation class Inherits(startNode: Inheritable, endNode: Inheritable) extends ContentRelation with HyperConnection with UuidNode

  @Node class Post extends ContentNode with Connectable with Inheritable with ScopeChild with Taggable {
    var title: String
    var description: Option[String]

    // override def validate = if (title.trim.isEmpty)
    //     Some("Title may not be blank")
    //   else
    //     None
  }


  // Action
  @Node trait Action extends Timestamp
  //TODO: store content of action in action
  @HyperRelation class Created(startNode: User, endNode: ContentNode) extends Action
  @HyperRelation class Updated(startNode: User, endNode: ContentNode) extends Action
  // TODO: should be a node? as the to be deleted node will deleted and we cannot connect there?
  // hiding nodes seems like more work.
  @HyperRelation class Deleted(startNode: User, endNode: ContentNode) extends Action
  @Relation class Reviewed(startNode: User, endNode: Action)
  //TODO: multidimesional voting?


  // Tags
  @Node class Tag extends ContentNode {
    @unique val title: String
    var description: Option[String]
    var isType: Boolean = false
  }
  @Node trait Taggable extends UuidNode
  @HyperRelation class Categorizes(startNode: Tag, endNode: Taggable) extends ContentRelation with HyperConnection with UuidNode
  @Relation class TaggingAction(startNode: User, endNode: Categorizes)
  @Relation class Votes(startNode: User, endNode: Categorizes) {
    val weight: Long
  }

  // Scopes
  @Node class Scope extends ContentNode with Inheritable with Taggable
  @Relation class HasReadAccess(startNode: UserGroup, endNode: Scope)
  @Relation class HasWriteAccess(startNode: UserGroup, endNode: Scope)
  @Relation class Owns(startNode: UserGroup, endNode: Scope)

  @Node trait ScopeChild extends UuidNode
  @Relation class BelongsTo(startNode: ScopeChild, endNode: Scope)

  //TODO: Node trait ownable?
  //
}
