package modules.db.access

import formatters.json.RequestFormat._
import model.WustSchema._
import modules.db.Database._
import modules.db._
import modules.live.Broadcaster
import modules.requests._
import play.api.libs.json.JsValue
import renesca.parameter.implicits._

class ContentNodeWrite[NODE <: ContentNode](override val factory: ContentNodeFactory[NODE]) extends NodeReadDelete(factory) {
  protected def createNode(discourse: Discourse, user: User, nodeAdd: NodeAddRequestBase): NODE = {
    val node = factory.createContentNode(title = nodeAdd.title, description = nodeAdd.description)
    val contribution = Contributes.create(user, node)
    discourse.add(node, contribution)
    node
  }

  protected def storeNode(discourse: Discourse, user: User, node: NODE): Option[String] = {
    val failure = db.transaction { tx =>
      tx.persistChanges(discourse)
    }

    if(failure.isEmpty) {
      Broadcaster.broadcastConnect(user, RelationDefinition(ConcreteFactoryNodeDefinition(User), Contributes, ConcreteFactoryNodeDefinition(factory)), node)
    }

    failure
  }

  protected def storeCreateNode(discourse: Discourse, user: User, node: NODE): Option[String] = {
    val failure = storeNode(discourse, user, node)
    if(failure.isEmpty) {
      Broadcaster.broadcastCreate(factory, node)
    }

    failure
  }

  override def create(user: User, json: JsValue): Either[NODE, String] = {
    val nodeAdd = json.as[NodeAddRequest]
    val discourse = Discourse.empty
    val node = createNode(discourse, user, nodeAdd)

    storeCreateNode(discourse, user, node) match {
      case Some(err) => Right(s"Cannot create ContentNode with label '${ factory.label }: $err'")
      case _         => Left(node)
    }
  }
}

class ContentNodeAccess[NODE <: ContentNode](override val factory: ContentNodeFactory[NODE]) extends ContentNodeWrite(factory) {
  protected def editNode(discourse: Discourse, user: User, uuid: String, nodeAdd: NodeUpdateRequestBase): NODE = {
    val node = factory.matchesContentNode(uuid = Some(uuid), matches = Set("uuid"))
    if(nodeAdd.title.isDefined) {
      if(node.title.get.isEmpty())
        node.title = None;
      else
        node.title = nodeAdd.title
    }

    if(nodeAdd.description.isDefined)
      node.description = nodeAdd.description.get

    if(nodeAdd.title.isDefined || nodeAdd.description.isDefined) {
      val contribution = Contributes.create(user, node)
      discourse.add(contribution)
    }

    node
  }

  protected def storeEditNode(discourse: Discourse, user: User, node: NODE): Option[String] = {
    val failure = storeNode(discourse, user, node)
    if(failure.isEmpty) {
      Broadcaster.broadcastEdit(factory, node)
    }

    failure
  }

  override def update(uuid: String, user: User, json: JsValue): Either[NODE, String] = {
    val nodeAdd = json.as[NodeUpdateRequest]
    val discourse = Discourse.empty
    val node = editNode(discourse, user, uuid, nodeAdd)
    storeEditNode(discourse, user, node) match {
      case Some(err) => Right(s"Cannot update ContentNode with uuid '$uuid' label '${ factory.label }: $err'")
      case _         => Left(node)
    }
  }
}

// can add nested tags
class PostAccess extends ContentNodeAccess[Post](Post) {
  private def tagDefGraph(addedTags: Seq[String]): Discourse = {
    val discourse = Discourse.empty
    val nodes = addedTags.map(tag => Tag.matches(uuid = Some(tag), matches = Set("uuid")))
    discourse.add(nodes: _*)
    discourse
  }

  private def handleAddedTags(discourse: Discourse, user: User, node: Post) {
    discourse.tags.foreach(tag => {
      val categorizes = CategorizesPost.create(tag, node)
      val action = TaggingAction.create(user, categorizes)
      discourse.add(categorizes, action)
      //TODO: broadcasts...
    })
  }

  //TODO: should create/update be nested?
  override def create(user: User, json: JsValue): Either[Post, String] = {
    val nodeAdd = json.as[TaggedNodeAddRequest]
    val discourse = tagDefGraph(nodeAdd.addedTags)

    val node = createNode(discourse, user, nodeAdd)
    handleAddedTags(discourse, user, node)
    storeCreateNode(discourse, user, node)

    Left(node)
  }

  override def update(uuid: String, user: User, json: JsValue): Either[Post, String] = {
    val nodeAdd = json.as[TaggedNodeUpdateRequest]
    val discourse = tagDefGraph(nodeAdd.addedTags)

    val node = editNode(discourse, user, uuid, nodeAdd)
    handleAddedTags(discourse, user, node)
    storeEditNode(discourse, user, node) match {
      case Some(err) => Right(s"Cannot update Post with uuid '$uuid' label '${ factory.label }: $err'")
      case _         => Left(node)
    }
  }
}
