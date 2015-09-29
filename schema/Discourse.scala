package model

import com.mohiva.play.silhouette.api.Identity
import renesca.parameter.LongPropertyValue
import renesca.schema.macros
import renesca.Transaction
import renesca.graph.Label
import moderation.Moderation
import Helpers.BooleanWithImplies
import renesca.schema.Item

trait Validator extends Item {
  import renesca.parameter.implicits._
  import renesca.graph.Match
  import scala.collection.mutable

  case class Validation(errorMessage: String, properties: Seq[String], handler: () => Boolean)
  private var validations: mutable.ArrayBuffer[Validation] = mutable.ArrayBuffer.empty

  def valid(errorMessage: String, properties: String*)(handler: => Boolean) {
    validations += Validation(errorMessage, properties, () => handler)
  }

  override final def validate = {
    val invalid = validations.view.map { validation =>
      if (rawItem.origin.kind != Match.kind || validation.properties.flatMap(rawItem.properties.get(_)).size == validation.properties.size)
        if (validation.handler()) None else Some(validation.errorMessage)
      else
        None
    }.find(_.isDefined)

    invalid.getOrElse(None)
  }
}

@macros.GraphSchema
object WustSchema {
  // TODO: Type aliases for several HyperRelation combinations
  // TODO: custom local methods for NodeFactory
  // TODO: annotation for hidden defaults?

  @Node trait UuidNode extends Validator {
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

    def hide() = {
      backupLabels = rawItem.labels.mkString(":")
      //TODO: workaround for rawItem.labels.clear(), which leaves some nodes?!
      rawItem.labels --= rawItem.labels.toList
      rawItem.labels ++= Hidden.labels
      Hidden.wrap(rawItem)
    }
  }

  //TODO: we should have a maybehidden trait from which hidden and post can inherit
  //so we do not have to match for UuidNode or Set.empty
  @Node class Hidden extends UuidNode {
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

    valid("Username may not be empty", "name") { !name.trim.isEmpty }
    valid("Email address is invalid", "email") { email.map(_.contains("@")).getOrElse(true) }
  }
  @Node class LoginInfo {
    val providerID: String
    @unique val providerKey: String
  }

  @Relation class Marks(startNode: User, endNode: Post) extends ConstructRelation

  // if you add/remove any properties (including inherited), you need to update modules.karma.KarmaStore
  @Relation class LogOnScope(startNode: KarmaLog, endNode: Scope) {
    val currentKarma: Long
  }
  @HyperRelation class KarmaLog(startNode: User, endNode: Post) extends Timestamp with UuidNode {
    val karmaChange: Long
    val reason: String
  }
  @Relation class HasKarma(startNode: User, endNode: Scope) {
    var karma: Long = 0
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

    valid("Groupname may not be empty", "name") { !name.trim.isEmpty }
  }
  @Relation class MemberOf(startNode: User, endNode: UserGroup)

  //TODO: rename
  @Graph trait Discourse {Nodes(User, UserGroup, Post, TagLike, Hidden, Action) }

  // relation trait for matching relations via start and endnode
  @Relation trait MatchableRelation
  // relation trait for relation that can be created/merged without further arguments
  @Relation trait ConstructRelation extends MatchableRelation

  // search and recent api make sure that only exposed node can be searched
  @Node trait ExposedNode extends UuidNode

  // Content
  @Node trait Connectable extends ExposedNode
  @HyperRelation class Connects(startNode: Post, endNode: Connectable) extends Connectable with MatchableRelation with Reference with RelationTimestamp
  //TODO: normal relation instead of hyper relation?
  @Relation class Inherits(startNode: Scope, endNode: Scope) extends ConstructRelation

  // post explicitly inherits timestamp to make it cheap to query recent posts
  // otherwise we would need to take include the created relation every time
  @Node class Post extends Connectable with Timestamp with Hideable {
    var title: String
    var description: Option[String]

    var _locked: Boolean = false
    var viewCount: Long = 0

    valid("Title may not be blank", "title") { !title.trim.isEmpty }
    valid("Title may not exceed length of 140 characters", "title") { title.length <= 140 }
    valid("ViewCount must be positive", "viewCount") { viewCount >= 0 }
  }

  @Relation class Viewed(startNode: User, endNode: Post) extends RelationTimestamp

  // Action
  @Node trait Action extends UuidNode with Timestamp
  @HyperRelation class Created(startNode: User, endNode: Post) extends Action

  val CONFLICT = -2
  val REJECTED = -1
  val PENDING = 0
  val INSTANT = 1
  val APPROVED = 2
  //TODO: should be called change
  @Node trait ChangeRequest extends Action with Votable {
    val applyThreshold:Long
    var approvalSum:Long = 0

    //TODO: rename to applyStatus
    var status:Long = PENDING

    def rejectThreshold = Moderation.rejectPostChangeThreshold(applyThreshold)
    def canApply(approvalSum: Long = approvalSum):Boolean = Moderation.canApply(approvalSum, applyThreshold)
    def canReject(approvalSum: Long = approvalSum):Boolean = Moderation.canReject(approvalSum, rejectThreshold)
    def canApply:Boolean = canApply()
    def canReject:Boolean = canReject()

    valid("Appliable change requests should be approved", "status", "approvalSum", "applyThreshold") {
      (canApply implies status == APPROVED)
    }
    valid("Rejectable change requests should be rejected", "status", "approvalSum", "applyThreshold") {
      (canReject implies status == REJECTED)
    }
    valid("Change request status must be in Range", "status") {
      Seq[Long](CONFLICT,REJECTED,PENDING,INSTANT,APPROVED).contains(status)
    }
  }

  @Relation class Skipped(startNode: User, endNode: ChangeRequest)

  //TODO: rename to Edit(-change-request)
  @HyperRelation class Updated(startNode: User, endNode: Post) extends ChangeRequest with MatchableRelation {
    val oldTitle:String
    val newTitle:String
    val oldDescription:Option[String]
    val newDescription:Option[String]

    valid("Change request should change something", "oldTitle", "newTitle", "oldDescription", "newDescription") {
      oldTitle != newTitle || oldDescription != newDescription
    }
  }

  @HyperRelation class Deleted(startNode: User, endNode: Post) extends ChangeRequest with MatchableRelation

  @Node trait TagChangeRequest extends ChangeRequest
  @Relation class ProposesTag(startNode: TagChangeRequest, endNode: Scope) // one-to-one relation
  @Relation class ProposesClassify(startNode: TagChangeRequest, endNode: Classification) // one-to-many relation
  @HyperRelation class AddTags(startNode: User, endNode: Post) extends TagChangeRequest with MatchableRelation
  @HyperRelation class RemoveTags(startNode: User, endNode: Post) extends TagChangeRequest with MatchableRelation

  // @Relation class Reviewed(startNode: User, endNode: Action)

  //TODO: multidimesional voting?
  @Node trait Votable extends UuidNode {
    var _locked: Boolean = false
  }

  @Relation class Votes(startNode: User, endNode: Votable) extends RelationTimestamp with Validator {
    val weight: Long // Up:>0 or Down:<0

    valid("Votes cannot have 0 weight", "weight") { weight != 0 }
  }

  // base class for connects and tags
  @Node trait Reference extends Votable {
    var voteCount: Long = 0
    def quality(viewCount: Long): Double = Moderation.postQuality(voteCount, viewCount - voteCount)

    valid("VoteCount must be positive", "voteCount") { voteCount >= 0 }
  }

  // generic Tags (base for Tags, Scopes)
  @Node trait TagLike extends ExposedNode {
    @unique val title: String
    var description: Option[String]
    var color: Long // Hue 0..360, -1 is gray
    var symbol: Option[String]

    valid("Title of Tags cannot be empty", "title") { !title.trim.isEmpty }
    valid("Color must be between -1 and 360", "color") { color >= -1 && color <= 360 }
  }
  @HyperRelation class Tags(startNode: Scope, endNode: Post) extends MatchableRelation with UuidNode with Reference

  // Tags
  @Node class Classification extends TagLike

  @Relation class Classifies(startNode: Classification, endNode: Reference)

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
