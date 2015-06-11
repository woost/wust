package modules.db.access

import formatters.json.RequestFormat._
import model.WustSchema._
import modules.db.Database._
import modules.db.types.UuidHyperNodeDefinitionBase
import modules.db.{UuidNodeDefinition, FactoryUuidNodeDefinition, ConcreteNodeDefinition, RelationDefinition}
import modules.live.Broadcaster
import modules.requests.ConnectRequest
import play.api.libs.json.JsValue
import renesca.schema._

//TODO: relations should be created unique
//      afaik this can only be defined with db constraints,
//      otherwise we'll get race coniditions
//TODO: track what the user did here
class StartContentRelationAccess[
  START <: UuidNode,
  RELATION <: AbstractRelation[START,END],
  END <: UuidNode
](
  override val factory: ContentRelationFactory[START,RELATION,END],
  override val nodeFactory: NodeFactory[END]
) extends StartRelationReadDelete(factory, nodeFactory) {

  private def fail(uuid: String) = Right(s"Cannot connect Nodes with uuid '$uuid' at StartContentRelation")

  override def create(baseDef: UuidNodeDefinition[START], user: User, json: JsValue) = {
    val connect = json.as[ConnectRequest]
    val relationDefinition = RelationDefinition(baseDef, factory, toNodeDefinition(connect.uuid))
    val resultOpt = connectUuidNodes(relationDefinition)
    resultOpt match {
      case Some((start,end)) => {
        Broadcaster.broadcastConnect(start, relationDefinition, end)
        Left(end)
      }
      case None              => fail(connect.uuid)
    }
  }

  override def createHyper(baseDef: UuidHyperNodeDefinitionBase[START with AbstractRelation[_,_]], user: User, json: JsValue) = {
    val connect = json.as[ConnectRequest]
    val relationDefinition = RelationDefinition(baseDef, factory, toNodeDefinition(connect.uuid))
    val resultOpt = startConnectHyperNodes(relationDefinition)
    resultOpt match {
      case Some((_,end)) => {
        Broadcaster.broadcastStartHyperConnect(relationDefinition, end)
        Left(end)
      }
      case None              => fail(connect.uuid)
    }
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

  private def fail(uuid: String) = Right(s"Cannot connect Nodes with uuid '$uuid' at EndContentRelation")

  override def create(baseDef: UuidNodeDefinition[END], user: User, json: JsValue) = {
    val connect = json.as[ConnectRequest]
    val relationDefinition = RelationDefinition(toNodeDefinition(connect.uuid), factory, baseDef)
    val resultOpt = connectUuidNodes(relationDefinition)
    resultOpt match {
      case Some((start,end)) => {
        Broadcaster.broadcastConnect(start, relationDefinition, end)
        Left(start)
      }
      case None              => fail(connect.uuid)
    }
  }

  override def createHyper(baseDef: UuidHyperNodeDefinitionBase[END with AbstractRelation[_,_]], user: User, json: JsValue) = {
    val connect = json.as[ConnectRequest]
    val relationDefinition = RelationDefinition(toNodeDefinition(connect.uuid), factory, baseDef)
    val resultOpt = endConnectHyperNodes(relationDefinition)
    resultOpt match {
      case Some((start,_)) => {
        Broadcaster.broadcastEndHyperConnect(relationDefinition, start)
        Left(start)
      }
      case None              => fail(connect.uuid)
    }
  }
}

class VotesAccess(
  override val factory: ContentRelationFactory[User,AbstractRelation[User,Categorizes],Categorizes]
) extends EndRelationRead(factory, User) {
  override def createHyper(baseDef: UuidHyperNodeDefinitionBase[Categorizes with AbstractRelation[_,_]], user: User, json: JsValue) = {
    val relationDefinition = RelationDefinition(toNodeDefinition(user.uuid), factory, baseDef)
    val resultOpt = endConnectHyperNodes(relationDefinition)
    resultOpt match {
      case Some((start,_)) => {
        Broadcaster.broadcastEndHyperConnect(relationDefinition, start)
        Left(start)
      }
      case None              => Right("Cannot vote")
    }
  }
}

