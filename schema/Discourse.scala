package model

import com.mohiva.play.silhouette.api.Identity
import renesca.parameter.LongPropertyValue
import renesca.schema.macros
import renesca.Transaction

@macros.GraphSchema
object WustSchema {
  // TODO: Type aliases for several HyperRelation combinations
  // TODO: custom local methods for NodeFactory
  // TODO: annotation for hidden defaults?

  @Node trait UuidNode {
    @unique val uuid: String = Helpers.uuidBase64
  }

  //TODO: @Item trait in renesca-magic
  @Node trait Timestamp {
    val timestamp: Long = System.currentTimeMillis
  }
  @Relation trait RelationTimestamp {
    val timestamp: Long = System.currentTimeMillis
  }

  // Authentification
  @Graph trait Auth {Nodes(User, LoginInfo, PasswordInfo) }
  @Node class User extends UuidNode with Identity {
    @unique val name: String
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
  @Graph trait Discourse {Nodes(User, UserGroup, Post, TagLike) }
  @Relation trait ContentRelation
  //TODO: is HyperConnection really necessary?
  // --> this is only used by the requestschema and actually is not needed, should be removed.
  @Relation trait HyperConnection
  //TODO: rename to ExposedNode
  @Node trait ContentNode extends UuidNode

  // Content
  @Node trait Connectable extends UuidNode with Taggable
  @HyperRelation class Connects(startNode: Connectable, endNode: Connectable) extends Connectable with Taggable with ContentRelation with HyperConnection with UuidNode
  @Node trait Inheritable
  @HyperRelation class Inherits(startNode: Inheritable, endNode: Inheritable) extends ContentRelation with HyperConnection with UuidNode

  // post explicitly inherits timestamp to make it cheap to query recent posts
  // otherwise we would need to take include the created relation every time
  @Node class Post extends ContentNode with Connectable with Inheritable with Taggable with Timestamp {
    var title: String
    var description: Option[String]

    override def validate: Option[String] = {
      //TODO: challenge validation should work for matches nodes, but they are
      //weird do not have all properties set before they are resolved.
      if(!rawItem.properties.isDefinedAt("title"))
        return None

      if(title.trim.isEmpty)
        Some("Title may not be blank")
      else if(title.length > 140)
             Some("Title may not exceed length of 140 characters")
      else
        None
    }
  }


  // Action
  @Node trait Action extends Timestamp
  //TODO: store content of action in action, for example the new description and title
  @HyperRelation class Created(startNode: User, endNode: Post) extends Action
  @HyperRelation class Updated(startNode: User, endNode: Post) extends Action with UuidNode with HyperConnection {
    val oldTitle:String
    val newTitle:String
    val oldDescription:Option[String]
    val newDescription:Option[String]
    val applyThreshold:Long
    val applyVotes:Long
    var applied:Boolean = false
  }

  // TODO: should be a node? as the to be deleted node will deleted and we cannot connect there?
  // hiding nodes seems like more work.
  @HyperRelation class Deleted(startNode: User, endNode: Post) extends Action
  @Relation class Reviewed(startNode: User, endNode: Action)
  //TODO: multidimesional voting?

  @Relation class VotesOnUpdated(startNode: User, endNode: Updated) extends RelationTimestamp {
    val weight: Long // Up:+1 or Down:-1
  }

  // generic Tags (base for Tags, Scopes)
  @Node trait TagLike extends ContentNode with Inheritable {
    //TODO: catch db constraint error when creating new tag, and return the already existing tag.
    @unique val title: String
    var description: Option[String]
    var color: Long // Hue 0..360, -1 is gray
    var symbol: Option[String]
  }
  @Node trait Taggable extends UuidNode
  @HyperRelation class Tags(startNode: TagLike, endNode: Taggable) extends ContentRelation with HyperConnection with UuidNode

  // Tags
  @Node class Categorization extends TagLike
  @Node class Classification extends TagLike
  @Node class StaticTag extends TagLike


  // Scopes
  @Node class Scope extends TagLike
  //  @Relation class HasReadAccess(startNode: UserGroup, endNode: Scope)
  //  @Relation class HasWriteAccess(startNode: UserGroup, endNode: Scope)
  //  @Relation class Owns(startNode: UserGroup, endNode: Scope)

  //TODO: Node trait ownable?
  //

  def deleteConnectsGarbage(tx: Transaction) {
    // while there are hyperrelations without any of both helper relations
    // delete them
    while(tx.queryTable(
      """MATCH (c:CONNECTS)
          WHERE NOT (()-[:CONNECTABLETOCONNECTS]->(c) AND (c)-[:CONNECTSTOCONNECTABLE]->())
          OPTIONAL MATCH (c)-[r]-()
          DELETE c,r
          RETURN COUNT(c)""").rows.head.cells.head.asLong > 0) {}
  }
}
