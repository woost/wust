package model

import com.mohiva.play.silhouette.api.Identity
import renesca.parameter.LongPropertyValue
import renesca.schema.macros
import renesca.Transaction
import renesca.graph.Label
import moderation.Moderation

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
    var timestamp: Long = System.currentTimeMillis
  }

  @Node trait Hideable {
    //TODO: cannot persist changes of option in matches node
    var backupLabels: String = ""

    def hide() {
      backupLabels = rawItem.labels.mkString(":")
      rawItem.labels.clear()
      rawItem.labels ++= Hidden.labels
    }
  }

  @Node class Hidden {
    var backupLabels: String = ""

    def unhide() {
      rawItem.labels.clear()
      rawItem.labels ++= backupLabels.split(":").toSet.map(Label.apply)
      backupLabels = ""
    }
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
  @Graph trait Discourse {Nodes(User, UserGroup, Post, TagLike, ChangeRequest) }

  // relation trait for relation that can be created/merged without further arguments
  @Relation trait ConstructRelation

  // search and recent api make sure that only exposed node can be searched
  @Node trait ExposedNode extends UuidNode

  // the hyperconnection trait is needed in order to have a matches method generated by renesca-magic:
  // so a relationaccess can actually match the hyperrelation when handling nested members
  @Relation trait HyperConnection

  // Content
  @Node trait Connectable extends ExposedNode
  @HyperRelation class Connects(startNode: Post, endNode: Connectable) extends Connectable with ConstructRelation with HyperConnection with Reference
  @Node trait Inheritable extends UuidNode
  //TODO: different inherits relation for tags? normal relation instead of hyper relation?
  @HyperRelation class Inherits(startNode: Inheritable, endNode: Inheritable) extends ConstructRelation with HyperConnection with UuidNode

  // post explicitly inherits timestamp to make it cheap to query recent posts
  // otherwise we would need to take include the created relation every time
  @Node class Post extends Connectable with Timestamp with Hideable {
    var title: String
    var description: Option[String]

    var _locked: Boolean = false
    var viewCount: Long = 0

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

  @Relation class Viewed(startNode: User, endNode: Post) extends RelationTimestamp with ConstructRelation

  // Action
  @HyperRelation class Created(startNode: User, endNode: Post) extends UuidNode with Timestamp

  val REJECTED = -1
  val PENDING = 0
  val INSTANT = 1
  val APPLIED = 2
  //TODO: should be called change
  @Node trait ChangeRequest extends UuidNode with Timestamp with Votable {
    val applyThreshold:Long
    var approvalSum:Long = 0
    var applied:Long = PENDING

    def rejectThreshold = Moderation.rejectPostChangeThreshold(applyThreshold)
    def canApply(approvalSum: Long = approvalSum):Boolean = approvalSum >= applyThreshold
    def canReject(approvalSum: Long = approvalSum):Boolean = approvalSum < rejectThreshold
    def canApply:Boolean = canApply()
    def canReject:Boolean = canReject()
  }

  //TODO: rename to Edit(-change-request)
  @HyperRelation class Updated(startNode: User, endNode: Post) extends ChangeRequest with HyperConnection {
    val oldTitle:String
    val newTitle:String
    val oldDescription:Option[String]
    val newDescription:Option[String]
  }

  @HyperRelation class Deleted(startNode: User, endNode: Post) extends ChangeRequest with HyperConnection

  @Node trait TagChangeRequest extends ChangeRequest
  @Relation class ProposesTag(startNode: TagChangeRequest, endNode: Scope)
  @HyperRelation class AddTags(startNode: User, endNode: Post) extends TagChangeRequest with HyperConnection
  @HyperRelation class RemoveTags(startNode: User, endNode: Post) extends TagChangeRequest with HyperConnection

  // @Relation class Reviewed(startNode: User, endNode: Action)

  //TODO: multidimesional voting?
  @Node trait Votable extends UuidNode {
    var _locked: Boolean = false
  }

  @Relation class Votes(startNode: User, endNode: Votable) extends RelationTimestamp {
    val weight: Long // Up:+1 or Down:-1
  }

  // base class for connects and tags
  @Node trait Reference extends Votable {
    var voteCount: Long = 0
    def quality(viewCount: Long): Double = Moderation.postQuality(voteCount, viewCount - voteCount)
  }

  // generic Tags (base for Tags, Scopes)
  @Node trait TagLike extends ExposedNode with Inheritable {
    @unique val title: String
    var description: Option[String]
    var color: Long // Hue 0..360, -1 is gray
    var symbol: Option[String]
  }
  @HyperRelation class Tags(startNode: Scope, endNode: Post) extends ConstructRelation with HyperConnection with UuidNode with Reference

  // Tags
  @Node class Classification extends TagLike

  @Relation class Classifies(startNode: Classification, endNode: Connects) extends ConstructRelation

  // Scopes
  @Node class Scope extends TagLike
  //  @Relation class HasReadAccess(startNode: UserGroup, endNode: Scope)
  //  @Relation class HasWriteAccess(startNode: UserGroup, endNode: Scope)
  //  @Relation class Owns(startNode: UserGroup, endNode: Scope)

  //TODO: Node trait ownable?

  def deleteConnectsGarbage(tx: Transaction) {
    // while there are hyperrelations without any of both helper relations
    // delete them
    //TODO: still needed with fixed startnode to post?
    while(tx.queryTable(
      """MATCH (c:CONNECTS)
          WHERE NOT (()-[:POSTTOCONNECTS]->(c) AND (c)-[:CONNECTSTOCONNECTABLE]->())
          OPTIONAL MATCH (c)-[r]-()
          DELETE c,r
          RETURN COUNT(c)""").rows.head.cells.head.asLong > 0) {}
  }
}
