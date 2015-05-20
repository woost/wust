package modules.db.access

import formatters.json.RequestFormat._
import model.WustSchema._
import modules.db.{ConcreteFactoryNodeDefinition, RelationDefinition, ConcreteNodeDefinition}
import modules.db.Database._
import modules.live.Broadcaster
import modules.requests.NodeAddRequest
import play.api.libs.json.JsValue

class ContentNodeAccess[NODE <: ContentNode](override val factory: ContentNodeFactory[NODE]) extends NodeReadDelete(factory) {
  override def create(user: User, json: JsValue) = {
    val nodeAdd = json.as[NodeAddRequest]
    val discourse = Discourse.empty
    val node = factory.local(nodeAdd.title, description = nodeAdd.description)
    val contribution = Contributes.local(user, node)
    discourse.add(node, contribution)
    db.persistChanges(discourse.graph)
    Broadcaster.broadcastCreate(factory, node)
    Broadcaster.broadcastConnect(user, RelationDefinition(ConcreteFactoryNodeDefinition(User), Contributes, ConcreteFactoryNodeDefinition(factory)), node)
    Left(node)
  }

  override def update(uuid: String, user: User, json: JsValue): Either[NODE,String] = {
    val nodeAdd = json.as[NodeAddRequest]
    val (discourse, nodes) = discourseNodes(factory, uuid)
    if (nodes.isEmpty)
      return Right(s"Cannot find ContentNode with uuid '$uuid' and label '${factory.label}'")

    val node = nodes.head
    node.title = nodeAdd.title
    node.description = nodeAdd.description
    val contribution = Contributes.local(user, node)
    discourse.add(contribution)
    db.persistChanges(discourse.graph)
    Broadcaster.broadcastEdit(factory, node)
    Broadcaster.broadcastConnect(user, RelationDefinition(ConcreteFactoryNodeDefinition(User), Contributes, ConcreteFactoryNodeDefinition(factory)), node)
    Left(node)
  }
}
