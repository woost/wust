package modules.db.access

import model.WustSchema._
import formatters.json.GraphFormat._
import modules.db.Database._
import modules.db.{HyperNodeDefinitionBase, RelationDefinition, UuidNodeDefinition}
import modules.requests.ConnectRequest
import play.api.libs.json.JsValue
import renesca.schema._

class StartContentRelationAccess[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
](
  override val factory: ContentRelationFactory[START,RELATION,END],
  override val nodeFactory: NodeFactory[END]
) extends StartRelationReadDelete(factory, nodeFactory) {

  private def createResult(resultOpt: Option[(START,END)], uuid: String) = resultOpt match {
    case Some((_,end)) => Left(end)
    case None          => Right(s"Cannot connect Nodes with uuid '$uuid' at StartContentRelation")
  }

  override def create(baseDef: UuidNodeDefinition[START], json: JsValue) = {
    val connect = json.as[ConnectRequest]
    val otherNode = UuidNodeDefinition(nodeFactory, connect.uuid)
    val resultOpt = connectUuidNodes(RelationDefinition(baseDef, factory, otherNode))
    //Broadcaster.broadcastConnect(start, factory, end)
    createResult(resultOpt, connect.uuid)
  }

  override def createHyper(baseDef: HyperNodeDefinitionBase[START with AbstractRelation[_,_]], json: JsValue) = {
    val connect = json.as[ConnectRequest]
    val otherNode = UuidNodeDefinition(nodeFactory, connect.uuid)
    val resultOpt = startConnectHyperNodes(RelationDefinition(baseDef, factory, otherNode))
    //Broadcaster.broadcastConnect(start, factory, end)
    createResult(resultOpt, connect.uuid)
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

  private def createResult(resultOpt: Option[(START,END)], uuid: String) = resultOpt match {
    case Some((start,_)) => Left(start)
    case None            => Right(s"Cannot connect Node with uuid '$uuid' at EndContentRelation")
  }

  override def create(baseDef: UuidNodeDefinition[END], json: JsValue) = {
    val connect = json.as[ConnectRequest]
    val otherNode = UuidNodeDefinition(nodeFactory, connect.uuid)
    val resultOpt = connectUuidNodes(RelationDefinition(otherNode, factory, baseDef))
    //Broadcaster.broadcastConnect(start, factory, end)
    createResult(resultOpt, connect.uuid)
  }

  override def createHyper(baseDef: HyperNodeDefinitionBase[END with AbstractRelation[_,_]], json: JsValue) = {
    val connect = json.as[ConnectRequest]
    val otherNode = UuidNodeDefinition(nodeFactory, connect.uuid)
    val resultOpt = endConnectHyperNodes(RelationDefinition(otherNode, factory, baseDef))
    //Broadcaster.broadcastConnect(start, factory, end)
    createResult(resultOpt, connect.uuid)
  }
}
