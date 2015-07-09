package modules.db.access.custom

import formatters.json.RequestFormat._
import model.WustSchema._
import modules.db.Database.db
import modules.db.access.NodeReadDelete
import modules.requests._
import play.api.libs.json.JsValue
import renesca.parameter.implicits._

class ContentNodeWrite[NODE <: ContentNode](override val factory: ContentNodeFactory[NODE]) extends NodeReadDelete(factory) {
  protected def createNode(discourse: Discourse, user: User, nodeAdd: NodeAddRequestBase): NODE = {
    val node = factory.createContentNode(title = nodeAdd.title, description = nodeAdd.description)
    val contribution = Created.create(user, node)
    discourse.add(node, contribution)
    node
  }

  protected def storeNode(discourse: Discourse, user: User, node: NODE): Option[String] = {
    db.transaction(_.persistChanges(discourse))
  }

  override def create(user: User, json: JsValue): Either[NODE, String] = {
    json.validate[NodeAddRequest].map(request => {
      val discourse = Discourse.empty
      val node = createNode(discourse, user, request)

      storeNode(discourse, user, node) match {
        case Some(err) => Right(s"Cannot create ContentNode: $err'")
        case _         => Left(node)
      }
    }).getOrElse(Right("Error parsing create request"))
  }
}

object ContentNodeWrite {
  def apply[NODE <: ContentNode](factory: ContentNodeFactory[NODE]) = new ContentNodeWrite(factory)
}

class ContentNodeAccess[NODE <: ContentNode](override val factory: ContentNodeFactory[NODE]) extends ContentNodeWrite(factory) {
  protected def editNode(discourse: Discourse, user: User, uuid: String, nodeAdd: NodeUpdateRequestBase): NODE = {
    val node = factory.matchesContentNode(uuid = Some(uuid), matches = Set("uuid"))
    if(nodeAdd.description.isDefined) {
      if(nodeAdd.description.get.isEmpty)
        node.description = None
      else
        node.description = nodeAdd.description
    }

    if(nodeAdd.title.isDefined)
      node.title = nodeAdd.title.get

    if(nodeAdd.title.isDefined || nodeAdd.description.isDefined) {
      val contribution = Updated.create(user, node)
      discourse.add(contribution)
    }

    node
  }

  override def update(uuid: String, user: User, json: JsValue): Either[NODE, String] = {
    json.validate[NodeUpdateRequest].map(request => {
      val discourse = Discourse.empty
      val node = editNode(discourse, user, uuid, request)
      storeNode(discourse, user, node) match {
        case Some(err) => Right(s"Cannot update ContentNode with uuid '$uuid': $err'")
        case _         => Left(node)
      }
    }).getOrElse(Right("Error parsing update request"))
  }
}

object ContentNodeAccess {
  def apply[NODE <: ContentNode](factory: ContentNodeFactory[NODE]) = new ContentNodeAccess(factory)
}

// can add nested tags
class PostAccess extends ContentNodeAccess[Post](Post) {
  private def tagDefGraph(addedTags: List[String]): Discourse = {
    val discourse = Discourse.empty
    val nodes = addedTags.map(tag => Tag.matches(uuid = Some(tag), matches = Set("uuid")))
    discourse.add(nodes: _*)
    discourse
  }

  private def handleAddedTags(discourse: Discourse, user: User, node: Post) {
    discourse.tags.foreach(tag => {
      val categorizes = Categorizes.merge(tag, node)
      val action = TaggingAction.merge(user, categorizes)
      discourse.add(categorizes, action)
    })
  }

  //TODO: should create/update be nested?
  override def create(user: User, json: JsValue): Either[Post, String] = {
    json.validate[TaggedNodeAddRequest].map(request => {
      val discourse = tagDefGraph(request.addedTags)

      val node = createNode(discourse, user, request)
      handleAddedTags(discourse, user, node)
      storeNode(discourse, user, node)

      Left(node)
    }).getOrElse(Right("Error parsing create request for post"))
  }

  override def update(uuid: String, user: User, json: JsValue): Either[Post, String] = {
    json.validate[TaggedNodeUpdateRequest].map(request => {
      val discourse = tagDefGraph(request.addedTags)

      val node = editNode(discourse, user, uuid, request)
      handleAddedTags(discourse, user, node)
      storeNode(discourse, user, node) match {
        case Some(err) => Right(s"Cannot update Post with uuid '$uuid': $err'")
        case _         => Left(node)
      }
    }).getOrElse(Right("Error parsing update request for post"))
  }
}

object PostAccess {
  def apply = new PostAccess
}
