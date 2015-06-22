package modules.db.access

import formatters.json.RequestFormat._
import model.WustSchema._
import modules.db.Database._
import modules.db.GraphHelper._
import modules.db._
import modules.live.Broadcaster
import modules.requests._
import play.api.libs.json.JsValue

class ContentNodeWrite[NODE <: ContentNode](override val factory: ContentNodeFactory[NODE]) extends NodeReadDelete(factory) {
  protected def createNode(discourse: Discourse, user: User, nodeAdd: NodeAddRequestBase): NODE = {
    val node = factory.createContentNode(title = nodeAdd.title, description = nodeAdd.description)
    val contribution = Contributes.create(user, node)
    discourse.add(node, contribution)
    node
  }

  protected def storeNode(discourse: Discourse, user: User, node: NODE): Unit = {
    db.persistChanges(discourse.graph)
    Broadcaster.broadcastConnect(user, RelationDefinition(ConcreteFactoryNodeDefinition(User), Contributes, ConcreteFactoryNodeDefinition(factory)), node)
  }

  protected def storeCreateNode(discourse: Discourse, user: User, node: NODE): Unit = {
    storeNode(discourse, user, node)
    Broadcaster.broadcastCreate(factory, node)
  }

  override def create(user: User, json: JsValue): Either[NODE,String] = {
    val nodeAdd = json.as[NodeAddRequest]
    val discourse = Discourse.empty
    val node = createNode(discourse, user, nodeAdd)
    storeCreateNode(discourse, user, node)
    Left(node)
  }
}

class ContentNodeAccess[NODE <: ContentNode](override val factory: ContentNodeFactory[NODE]) extends ContentNodeWrite(factory) {
  protected def editNode(discourse: Discourse, user: User, uuid: String, nodeAdd: NodeUpdateRequestBase): Either[NODE,String] = {
    val nodeDef = FactoryUuidNodeDefinition(factory, uuid)
    val nodes = findNodes(discourse, nodeDef)
    if (nodes.isEmpty)
      return Right(s"Cannot find ContentNode with uuid '$uuid' and label '${factory.label}'")

    val node = nodes.head
    if (nodeAdd.title.isDefined) {
      if (node.title.get.isEmpty())
        node.title = None;
      else
        node.title = nodeAdd.title
    }

    if (nodeAdd.description.isDefined)
      node.description = nodeAdd.description.get

    if (nodeAdd.title.isDefined || nodeAdd.description.isDefined) {
      val contribution = Contributes.create(user, node)
      discourse.add(contribution)
    }

    Left(node)
  }

  protected def storeEditNode(discourse: Discourse, user: User, node: NODE): Unit = {
    storeNode(discourse, user, node)
    Broadcaster.broadcastEdit(factory, node)
  }

  override def update(uuid: String, user: User, json: JsValue): Either[NODE,String] = {
    val nodeAdd = json.as[NodeUpdateRequest]
    val nodeDef = FactoryUuidNodeDefinition(factory, uuid)
    val discourse = discourseGraph(nodeDef)
    editNode(discourse, user, uuid, nodeAdd) match {
      case Left(node) =>
        storeEditNode(discourse, user, node)
        Left(node)
      case Right(msg) =>
        Right(msg)
    }
  }
}

// can add nested tags
class PostAccess extends ContentNodeAccess[Post](Post) {
  private def tagDefGraph(addedTags: Seq[String], nodeDef: Option[NodeDefinition[Post]]): Discourse = {
    val defs = nodeDef.map(List(_)).getOrElse(List.empty) ++ addedTags.map(tag => FactoryUuidNodeDefinition(Tag, tag)).toList
    if(defs.isEmpty)
      Discourse.empty
    else
      discourseGraph(defs: _*)
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
  override def create(user: User, json: JsValue): Either[Post,String] = {
    val nodeAdd = json.as[TaggedNodeAddRequest]
    val discourse = tagDefGraph(nodeAdd.addedTags, None)

    val node = createNode(discourse, user, nodeAdd)
    handleAddedTags(discourse, user, node)
    storeCreateNode(discourse, user, node)

    Left(node)
  }

  override def update(uuid: String, user: User, json: JsValue): Either[Post,String] = {
    val nodeAdd = json.as[TaggedNodeUpdateRequest]
    val discourse = tagDefGraph(nodeAdd.addedTags, Some(FactoryUuidNodeDefinition(Post, uuid)))

    editNode(discourse, user, uuid, nodeAdd) match {
      case Left(node) =>
        handleAddedTags(discourse, user, node)
        storeEditNode(discourse, user, node)
        Left(node)
      case Right(msg) =>
        Right(msg)
    }
  }
}
