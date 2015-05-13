package modules.db

import formatters.json.GraphFormat._
import model.WustSchema._
import modules.db.Database._
import modules.requests.NodeAddRequest
import play.api.libs.json.JsValue
import renesca.schema._

trait NodeAccess[NODE <: UuidNode] {
  val factory: NodeFactory[NODE]
  val name = factory.getClass.getSimpleName.dropRight(1)

  def read: Option[Seq[NODE]] = None
  def read(uuid: String): Option[NODE] = None
  def create(user: User, json: JsValue): Option[NODE] = None
  def update(uuid: String, user: User, nodeAdd: JsValue): Option[NODE] = None
  def delete(uuid: String): Boolean = false

  def toNodeDefinition(uuid: String) = UuidNodeDefinition(factory, uuid)
}

class NodeRead[NODE <: UuidNode](val factory: NodeFactory[NODE]) extends NodeAccess[NODE] {
  override def read: Option[Seq[NODE]] = {
    Some(discourseNodes(factory)._2)
  }

  override def read(uuid: String): Option[NODE] = {
    discourseNodes(factory, uuid)._2.headOption
  }
}

class NodeReadDelete[NODE <: UuidNode](factory: NodeFactory[NODE]) extends NodeRead(factory) {
  override def delete(uuid: String): Boolean = {
    deleteNodes(UuidNodeDefinition(factory, uuid))
    true
  }
}

class ContentNodeAccess[NODE <: ContentNode](override val factory: ContentNodeFactory[NODE]) extends NodeReadDelete(factory) {
  override def create(user: User, json: JsValue): Option[NODE] = {
    val nodeAdd = json.as[NodeAddRequest]
    val discourse = Discourse.empty
    val contentNode = factory.local(nodeAdd.title, description = nodeAdd.description)
    val contribution = Contributes.local(user, contentNode)
    discourse.add(contentNode, contribution)
    db.persistChanges(discourse.graph)
    Some(contentNode)
  }

  override def update(uuid: String, user: User, json: JsValue): Option[NODE] = {
    val nodeAdd = json.as[NodeAddRequest]
    val (discourse, Seq(node)) = discourseNodes(factory, uuid)
    node.title = nodeAdd.title
    node.description = nodeAdd.description
    val contribution = Contributes.local(user, node)
    discourse.add(contribution)
    db.persistChanges(discourse.graph)
    //Broadcaster.broadcastEdit(nodeSchema.path, node)
    Some(node)
  }
}

trait RelationAccess[NODE <: UuidNode] {
  def read(baseDef: FixedNodeDefinition[NODE]): Option[Set[_ <: UuidNode]] = None
  def delete(baseDef: FixedNodeDefinition[NODE], uuid: String): Boolean = false
  def create(baseDef: UuidNodeDefinition[NODE], uuid: String): Option[_ <: UuidNode] = None
  def createHyper(baseDef: HyperNodeDefinitionBase[NODE with AbstractRelation[_,_]], uuid: String): Option[_ <: UuidNode] = None
}

trait StartRelationAccess[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
] extends RelationAccess[START] {
  val factory: AbstractRelationFactory[START, RELATION, END]

  def toNodeDefinition(uuid: String): FixedNodeDefinition[END]
}

trait EndRelationAccess[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
] extends RelationAccess[END] {
  val factory: AbstractRelationFactory[START, RELATION, END]

  def toNodeDefinition(uuid: String): FixedNodeDefinition[START]
}

class StartAnyRelation[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
] (
  val factory: AbstractRelationFactory[START, RELATION, END]
) extends StartRelationAccess[START,RELATION,END] {

  def toNodeDefinition(uuid: String) = AnyUuidNodeDefinition(uuid)

  override def read(baseDef: FixedNodeDefinition[START]): Option[Set[END]] = {
    Some(startConnectedDiscourseNodes(RelationDefinition(baseDef, factory, AnyNodeDefinition()))._2)
  }
}

class EndAnyRelation[
START <: UuidNode,
RELATION <: AbstractRelation[START,END],
END <: UuidNode
] (
  val factory: AbstractRelationFactory[START, RELATION, END]
  ) extends EndRelationAccess[START,RELATION,END] {

  def toNodeDefinition(uuid: String) = AnyUuidNodeDefinition(uuid)

  override def read(baseDef: FixedNodeDefinition[END]): Option[Set[START]] = {
    Some(endConnectedDiscourseNodes(RelationDefinition(AnyNodeDefinition(), factory, baseDef))._2)
  }
}

class StartRelationRead[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
] (
  val factory: AbstractRelationFactory[START, RELATION, END],
  val nodeFactory: NodeFactory[END]
) extends StartRelationAccess[START,RELATION,END] {

  def toNodeDefinition(uuid: String) = UuidNodeDefinition(nodeFactory, uuid)

  override def read(baseDef: FixedNodeDefinition[START]): Option[Set[END]] = {
    Some(startConnectedDiscourseNodes(RelationDefinition(baseDef, factory, LabelNodeDefinition(nodeFactory)))._2)
  }
}

class StartRelationReadDelete[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
](
  factory: AbstractRelationFactory[START, RELATION, END],
  nodeFactory: NodeFactory[END]
) extends StartRelationRead(factory, nodeFactory) {

  override def delete(baseDef: FixedNodeDefinition[START], uuid:String) = {
    val otherNode = UuidNodeDefinition(nodeFactory, uuid)
    disconnectNodes(RelationDefinition(baseDef, factory, otherNode))
    //Broadcaster.broadcastDisconnect(uuid, factory, otherUuid)
    true
  }
}

class EndRelationRead[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
](
  val factory: AbstractRelationFactory[START, RELATION, END],
  val nodeFactory: NodeFactory[START]
) extends EndRelationAccess[START,RELATION,END] {

  def toNodeDefinition(uuid: String) = UuidNodeDefinition(nodeFactory, uuid)

  override def read(baseDef: FixedNodeDefinition[END]): Option[Set[START]] = {
    Some(endConnectedDiscourseNodes(RelationDefinition(LabelNodeDefinition(nodeFactory), factory, baseDef))._2)
  }
}

class EndRelationReadDelete[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
 ](
   factory: AbstractRelationFactory[START, RELATION, END],
   nodeFactory: NodeFactory[START]
) extends EndRelationRead(factory, nodeFactory) {

  override def delete(baseDef: FixedNodeDefinition[END], uuid:String) = {
    val otherNode = UuidNodeDefinition(nodeFactory, uuid)
    disconnectNodes(RelationDefinition(otherNode, factory, baseDef))
    //Broadcaster.broadcastDisconnect(uuid, factory, otherUuid)
    true
  }
}

class StartContentRelationAccess[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
](
  override val factory: ContentRelationFactory[START,RELATION,END],
  override val nodeFactory: NodeFactory[END]
) extends StartRelationReadDelete(factory, nodeFactory) {

  override def create(baseDef: UuidNodeDefinition[START], uuid: String) = {
    val otherNode = UuidNodeDefinition(nodeFactory, uuid)
    val (start,end) = connectUuidNodes(RelationDefinition(baseDef, factory, otherNode))
    //Broadcaster.broadcastConnect(start, factory, end)
    Some(end)
  }

  override def createHyper(baseDef: HyperNodeDefinitionBase[START with AbstractRelation[_,_]], uuid: String) = {
    val otherNode = UuidNodeDefinition(nodeFactory, uuid)
    val (start,end) = startConnectHyperNodes(RelationDefinition(baseDef, factory, otherNode))
    //Broadcaster.broadcastConnect(start, factory, end)
    Some(end)
  }
}

class EndContentRelationAccess [
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
](
  override val factory: ContentRelationFactory[START,RELATION,END],
  override val nodeFactory: NodeFactory[START]
) extends EndRelationReadDelete(factory, nodeFactory) {
  override def create(baseDef: UuidNodeDefinition[END], uuid: String) = {
    val otherNode = UuidNodeDefinition(nodeFactory, uuid)
    val (start,end) = connectUuidNodes(RelationDefinition(otherNode, factory, baseDef))
    //Broadcaster.broadcastConnect(start, factory, end)
    Some(start)
  }

  override def createHyper(baseDef: HyperNodeDefinitionBase[END with AbstractRelation[_,_]], uuid: String) = {
    val otherNode = UuidNodeDefinition(nodeFactory, uuid)
    val (start,end) = endConnectHyperNodes(RelationDefinition(otherNode, factory, baseDef))
    //Broadcaster.broadcastConnect(start, factory, end)
    Some(start)
  }
}
