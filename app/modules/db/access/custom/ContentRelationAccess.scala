package modules.db.access.custom

import formatters.json.RequestFormat._
import model.WustSchema._
import modules.db.Database._
import modules.db.access.{StartRelationReadDelete, EndRelationReadDelete}
import modules.db.{HyperNodeDefinitionBase, RelationDefinition}
import modules.requests.ConnectRequest
import play.api.libs.json.JsValue
import renesca.parameter.implicits._
import renesca.schema._

trait ContentRelationHelper {
  def persistRelation[T](discourse: Discourse, result: T): Either[T, String] = {
    db.transaction(_.persistChanges(discourse)).map(err =>
      Right(s"Cannot create ContentRelation: $err'")
    ).getOrElse(Left(result))
  }
}

//TODO: track what the user did here
class StartContentRelationAccess[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](
  override val factory: ContentRelationFactory[START, RELATION, END],
  nodeFactory: UuidNodeFactory[END],
  baseFactory: UuidNodeFactory[START]) extends StartRelationReadDelete(factory, nodeFactory) with ContentRelationHelper {

  private def fail(uuid: String) = Right(s"Cannot connect Nodes with uuid '$uuid' at StartContentRelation")

  override def create(uuid: String, user: User, json: JsValue) = {
    val connect = json.as[ConnectRequest]
    val discourse = Discourse.empty
    val base = baseFactory.matchesUuidNode(uuid = Some(uuid), Set("uuid"))
    val node = nodeFactory.matchesUuidNode(uuid = Some(connect.uuid), Set("uuid"))
    discourse.add(factory.mergeContentRelation(base, node))
    persistRelation(discourse, node)
  }

  //TODO: use renesca like in create
  override def createHyper(baseDef: HyperNodeDefinitionBase[START with AbstractRelation[_, _]], user: User, json: JsValue) = {
    val connect = json.as[ConnectRequest]
    val relationDefinition = RelationDefinition(baseDef, factory, toNodeDefinition(connect.uuid))
    val resultOpt = startConnectHyperNodes(relationDefinition)
    resultOpt match {
      case Some((_, end)) => Left(end)
      case None           => fail(connect.uuid)
    }
  }
}

object StartContentRelationAccess {
  def apply[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](factory: ContentRelationFactory[START, RELATION, END], nodeFactory: UuidNodeFactory[END]): UuidNodeFactory[START] => StartContentRelationAccess[START, RELATION, END] = {
    baseFactory => new StartContentRelationAccess(factory, nodeFactory, baseFactory)
  }
}

class EndContentRelationAccess[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](
  override val factory: ContentRelationFactory[START, RELATION, END],
  nodeFactory: UuidNodeFactory[START],
  baseFactory: UuidNodeFactory[END]) extends EndRelationReadDelete(factory, nodeFactory) with ContentRelationHelper {

  private def fail(uuid: String) = Right(s"Cannot connect Nodes with uuid '$uuid' at EndContentRelation")

  override def create(uuid: String, user: User, json: JsValue) = {
    val connect = json.as[ConnectRequest]
    val discourse = Discourse.empty
    val base = baseFactory.matchesUuidNode(uuid = Some(uuid), Set("uuid"))
    val node = nodeFactory.matchesUuidNode(uuid = Some(connect.uuid), Set("uuid"))
    discourse.add(factory.mergeContentRelation(node, base))
    persistRelation(discourse, node)
  }

  //TODO: use renesca like in create
  override def createHyper(baseDef: HyperNodeDefinitionBase[END with AbstractRelation[_, _]], user: User, json: JsValue) = {
    val connect = json.as[ConnectRequest]
    val relationDefinition = RelationDefinition(toNodeDefinition(connect.uuid), factory, baseDef)
    val resultOpt = endConnectHyperNodes(relationDefinition)
    resultOpt match {
      case Some((start, _)) => Left(start)
      case None             => fail(connect.uuid)
    }
  }
}

object EndContentRelationAccess {
  def apply[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](factory: ContentRelationFactory[START, RELATION, END], nodeFactory: UuidNodeFactory[START]): UuidNodeFactory[END] => EndContentRelationAccess[START, RELATION, END] = {
    baseFactory => new EndContentRelationAccess(factory, nodeFactory, baseFactory)
  }
}
