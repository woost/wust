package model

import com.mohiva.play.silhouette.api.Identity
import common.Helpers
import renesca.schema.macros

@macros.GraphSchema
object WustSchema {
  // TODO: Type aliases for several HyperRelation combinations
  // TODO: custom local methods for NodeFactory
  // TODO: annotation for hidden defaults?

  @Group trait Discourse {List(User, UserGroup, Post, Tag, Scope, Connects, Inherits) }
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

  @Node trait PostLike extends UuidNode

  @Node trait ContentNode extends PostLike {
    var title: Option[String]
    var description: String
  }

  @Node trait Categorizes extends UuidNode

  @Node trait Penos extends ContentNode

  @Node class Tag extends ContentNode {
    var isType: Boolean = false
  }
  @Node class Post extends Penos
  @Node class Scope extends ContentNode

  @Relation trait ContentRelation

  @Relation trait Votes extends ContentRelation

  @HyperRelation class CategorizesScope(startNode: Tag, endNode: Scope) extends Categorizes with ContentRelation
  @HyperRelation class CategorizesPost(startNode: Tag, endNode: Post) extends Categorizes with ContentRelation
  @HyperRelation class CategorizesConnects(startNode: Tag, endNode: Connects) extends Categorizes with ContentRelation

  @Relation class UpVotes(startNode: User, endNode: Categorizes) extends Votes
  @Relation class DownVotes(startNode: User, endNode: Categorizes) extends Votes

  @Relation class TaggingAction(startNode: User, endNode: Categorizes)

  @Relation class Belongs(startNode: Post, endNode: Scope)

  //TODO: restrict to Posts and Connects itself
  @HyperRelation class Connects(startNode: PostLike, endNode: PostLike) extends PostLike with ContentRelation

  @HyperRelation class Inherits(startNode: ContentNode, endNode: ContentNode) extends PostLike with ContentRelation

  @Relation class Contributes(startNode: User, endNode: ContentNode) {
    val createdAt: Long = System.currentTimeMillis
  }
}
