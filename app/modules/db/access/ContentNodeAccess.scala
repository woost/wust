package modules.db.access

import javax.print.DocFlavor.STRING

import formatters.json.RequestFormat._
import model.WustSchema._
import modules.db._
import modules.db.Database._
import modules.live.Broadcaster
import modules.requests.{NodeAddRequestBase, TaggedNodeAddRequest, NodeAddRequest}
import play.api.libs.json.JsValue

class ContentNodeAccess[NODE <: ContentNode](override val factory: ContentNodeFactory[NODE]) extends NodeReadDelete(factory) {
  protected def createNode(discourse: Discourse, user: User, nodeAdd: NodeAddRequestBase): NODE = {
    val node = factory.localContentNode(title = nodeAdd.title, description = nodeAdd.description)
    val contribution = Contributes.local(user, node)
    discourse.add(node, contribution)
    node
  }

  protected def editNode(discourse: Discourse, user: User, uuid: String, nodeAdd: NodeAddRequestBase): Either[NODE,String] = {
    val nodeDef = FactoryUuidNodeDefinition(factory, uuid)
    val nodes = findNodes(discourse, nodeDef)
    if (nodes.isEmpty)
      return Right(s"Cannot find ContentNode with uuid '$uuid' and label '${factory.label}'")

    val node = nodes.head
    node.title = nodeAdd.title
    node.description = nodeAdd.description
    val contribution = Contributes.local(user, node)
    discourse.add(contribution)
    Left(node)
  }

  private def storeNode(discourse: Discourse, user: User, node: NODE): Unit = {
    db.persistChanges(discourse.graph)
    Broadcaster.broadcastConnect(user, RelationDefinition(ConcreteFactoryNodeDefinition(User), Contributes, ConcreteFactoryNodeDefinition(factory)), node)
  }

  protected def storeCreateNode(discourse: Discourse, user: User, node: NODE): Unit = {
    storeNode(discourse, user, node)
    Broadcaster.broadcastCreate(factory, node)
  }

  protected def storeEditNode(discourse: Discourse, user: User, node: NODE): Unit = {
    storeNode(discourse, user, node)
    Broadcaster.broadcastEdit(factory, node)
  }

  override def create(user: User, json: JsValue): Either[NODE,String] = {
    val nodeAdd = json.as[NodeAddRequest]
    val discourse = Discourse.empty
    val node = createNode(discourse, user, nodeAdd)
    storeCreateNode(discourse, user, node)
    Left(node)
  }

  override def update(uuid: String, user: User, json: JsValue): Either[NODE,String] = {
    val nodeAdd = json.as[NodeAddRequest]
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
  private def tagDefGraph(nodeAdd: TaggedNodeAddRequest, nodeDef: Option[NodeDefinition[Post]]): Discourse = {
    val defs = nodeDef.map(List(_)).getOrElse(List.empty) ++ nodeAdd.addedTags.map(tag => FactoryUuidNodeDefinition(Tag, tag)).toList
    val discourse = if(defs.isEmpty)
                      Discourse.empty
                    else
                      discourseGraph(defs: _*)
    discourse
  }

  private def handleAddedTags(discourse: Discourse, user: User, node: Post) {
    discourse.tags.foreach(tag => {
      val categorizes = CategorizesPost.local(tag, node)
      val action = TaggingAction.local(user, categorizes)
      discourse.add(categorizes, action)
      //TODO: broadcasts...
    })
  }

  override def create(user: User, json: JsValue): Either[Post,String] = {
    val nodeAdd = json.as[TaggedNodeAddRequest]
    val discourse = tagDefGraph(nodeAdd, None)

    val node = createNode(discourse, user, nodeAdd)
    handleAddedTags(discourse, user, node)

    storeCreateNode(discourse, user, node)

    Left(node)
  }

  override def update(uuid: String, user: User, json: JsValue): Either[Post,String] = {
    val nodeAdd = json.as[TaggedNodeAddRequest]
    val discourse = tagDefGraph(nodeAdd, Some(FactoryUuidNodeDefinition(Post, uuid)))

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
