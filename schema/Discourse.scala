package model

import com.mohiva.play.silhouette.api.Identity
import renesca.parameter.LongPropertyValue
import renesca.schema.macros
import renesca.Transaction

object custom {
  import renesca.{graph => raw}
  import renesca.schema.RelationFactory
  import WustSchema._

  object UpdatedToContentNodeFactory extends RelationFactory[Updated, UpdatedToContentNode, ContentNode] {
    def relationType = Updated.endRelationType
    def wrap(relation: raw.Relation) = ???
  }
}

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
  @Node trait User extends UuidNode {
    @unique val name: String
    //TODO: cannot have abstract methods...renesca magic
    def isDummy = true
  }
  @Node class RealUser extends User with Identity {
    var email: Option[String]
    var karma: Long = 0
    override def isDummy = false
  }
  @Node class DummyUser extends User
  @Node class LoginInfo {
    val providerID: String
    @unique val providerKey: String
  }

  @Node class PasswordInfo {
    val hasher: String
    val password: String
    val salt: Option[String]
  }
  @Relation class HasLogin(startNode: RealUser, endNode: LoginInfo)
  @Relation class HasPassword(startNode: LoginInfo, endNode: PasswordInfo)
  @Node class UserGroup extends UuidNode {
    @unique val name: String
  }
  @Relation class MemberOf(startNode: User, endNode: UserGroup)

  //TODO: rename
  @Graph trait Discourse {Nodes(User, UserGroup, Post, TagLike) }
  @Relation trait ContentRelation
  //TODO: is HyperConnection really necessary?
  @Relation trait HyperConnection
  //TODO: rename to ExposedNode
  @Node trait ContentNode extends UuidNode

  // Content
  @Node trait Connectable extends UuidNode with Taggable with Votable
  @HyperRelation class Connects(startNode: Connectable, endNode: Connectable) extends Connectable with Taggable with ContentRelation with HyperConnection with UuidNode
  @Node trait Inheritable
  @HyperRelation class Inherits(startNode: Inheritable, endNode: Inheritable) extends ContentRelation with HyperConnection with UuidNode

  // post explicitly inherits timestamp to make it cheap to query recent posts
  // otherwise we would need to take include the created relation every time
  @Node class Post extends ContentNode with Connectable with Inheritable with Taggable with Votable with Timestamp {
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
  @HyperRelation class Created(startNode: User, endNode: ContentNode) extends Action
  @HyperRelation class Updated(startNode: User, endNode: ContentNode) extends Action with Votable {
    val oldTitle:String
    val newTitle:String
    val oldDescription:Option[String]
    val newDescription:Option[String]
    var applied:Boolean = false
  }

  @Node class UpdateAccept extends VoteDimension

  // TODO: should be a node? as the to be deleted node will deleted and we cannot connect there?
  // hiding nodes seems like more work.
  @HyperRelation class Deleted(startNode: User, endNode: ContentNode) extends Action
  @Relation class Reviewed(startNode: User, endNode: Action)
  //TODO: multidimesional voting?


  @Node trait VoteDimension extends UuidNode
  @Node trait Votable extends UuidNode
  @HyperRelation class Dimensionizes(startNode: VoteDimension, endNode: Votable) extends HyperConnection with UuidNode
  @Relation class Votes(startNode: User, endNode: Dimensionizes) extends RelationTimestamp {
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
  @Node class Categorization extends TagLike with VoteDimension
  @Node class Classification extends TagLike with VoteDimension
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
