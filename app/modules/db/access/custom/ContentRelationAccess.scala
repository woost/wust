package modules.db.access.custom

import formatters.json.RequestFormat._
import model.WustSchema._
import modules.db.Database.db
import modules.db.HyperNodeDefinitionBase
import modules.db.access.{EndRelationReadDelete, StartRelationReadDelete}
import modules.requests.ConnectRequest
import play.api.libs.json.JsValue
import renesca.parameter.implicits._
import renesca.schema._

trait ContentRelationHelper {
  protected def persistRelation[T](discourse: Discourse, result: T): Either[T, String] = {
    db.transaction(_.persistChanges(discourse)).map(err =>
      Right(s"Cannot create ContentRelation: $err'")
    ).getOrElse(Left(result))
  }

  protected def fail(uuid: String) = Right(s"Cannot connect Nodes with uuid '$uuid'")

  protected def parseConnect[T](js: JsValue)(handler: ConnectRequest => Either[T, String]): Either[T, String] = {
    js.validate[ConnectRequest].map(handler).getOrElse(Right("Cannot parse connect request"))
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

  override def create(uuid: String, user: User, json: JsValue) = {
    parseConnect(json)(request => {
      val discourse = Discourse.empty
      val base = baseFactory.matchesUuidNode(uuid = Some(uuid), matches = Set("uuid"))
      val node = nodeFactory.matchesUuidNode(uuid = Some(request.uuid), matches = Set("uuid"))
      discourse.add(factory.mergeContentRelation(base, node))
      persistRelation(discourse, node)
    })
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

class StartContentRelationHyperAccess[
ISTART <: UuidNode,
IEND <: UuidNode,
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](
  override val factory: ContentRelationFactory[START, RELATION, END],
  nodeFactory: UuidNodeFactory[END],
  startFactory: UuidNodeFactory[ISTART],
  baseFactory: HyperConnectionFactory[ISTART,START with AbstractRelation[ISTART,IEND],IEND] with UuidNodeFactory[START],
  endFactory: UuidNodeFactory[IEND]
  ) extends StartRelationReadDelete(factory, nodeFactory) with ContentRelationHelper {

  override def createHyper(startUuid: String, endUuid: String, user: User, json: JsValue) = {
    parseConnect(json)(request => {
      val discourse = Discourse.empty
      val start = startFactory.matchesUuidNode(uuid = Some(startUuid), matches = Set("uuid"))
      val end = endFactory.matchesUuidNode(uuid = Some(endUuid), matches = Set("uuid"))
      val base = baseFactory.matchesHyperConnection(start, end)
      val node = nodeFactory.matchesUuidNode(uuid = Some(request.uuid), matches = Set("uuid"))
      val relation = factory.mergeContentRelation(base, node)
      discourse.add(base, relation)
      persistRelation(discourse, node)
    })
  }
}

object StartContentRelationHyperAccess {
  def apply[
  ISTART <: UuidNode,
  IEND <: UuidNode,
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](factory: ContentRelationFactory[START, RELATION, END], nodeFactory: UuidNodeFactory[END]): (UuidNodeFactory[ISTART], HyperConnectionFactory[ISTART,START with AbstractRelation[ISTART,IEND],IEND] with UuidNodeFactory[START], UuidNodeFactory[IEND]) => StartContentRelationHyperAccess[ISTART,IEND,START, RELATION, END] = {
    (startFactory, baseFactory, endFactory) => new StartContentRelationHyperAccess(factory, nodeFactory, startFactory, baseFactory, endFactory)
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

  override def create(uuid: String, user: User, json: JsValue) = {
    parseConnect(json)(request => {
      val discourse = Discourse.empty
      val base = baseFactory.matchesUuidNode(uuid = Some(uuid), matches = Set("uuid"))
      val node = nodeFactory.matchesUuidNode(uuid = Some(request.uuid), matches = Set("uuid"))
      discourse.add(factory.mergeContentRelation(node, base))
      persistRelation(discourse, node)
    })
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

class EndContentRelationHyperAccess[
ISTART <: UuidNode,
IEND <: UuidNode,
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](
  override val factory: ContentRelationFactory[START, RELATION, END],
  nodeFactory: UuidNodeFactory[START],
  startFactory: UuidNodeFactory[ISTART],
  baseFactory: HyperConnectionFactory[ISTART,END with AbstractRelation[ISTART,IEND],IEND] with UuidNodeFactory[END],
  endFactory: UuidNodeFactory[IEND]
  ) extends EndRelationReadDelete(factory, nodeFactory) with ContentRelationHelper {

  override def createHyper(startUuid: String, endUuid: String, user: User, json: JsValue) = {
    parseConnect(json)(request => {
      val discourse = Discourse.empty
      val start = startFactory.matchesUuidNode(uuid = Some(startUuid), matches = Set("uuid"))
      val end = endFactory.matchesUuidNode(uuid = Some(endUuid), matches = Set("uuid"))
      val base = baseFactory.matchesHyperConnection(start, end)
      val node = nodeFactory.matchesUuidNode(uuid = Some(request.uuid), Set("uuid"))
      val relation = factory.mergeContentRelation(node, base)
      discourse.add(base, relation)
      persistRelation(discourse, node)
    })
  }
}

object EndContentRelationHyperAccess {
  def apply[
  ISTART <: UuidNode,
  IEND <: UuidNode,
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](factory: ContentRelationFactory[START, RELATION, END], nodeFactory: UuidNodeFactory[START]): (UuidNodeFactory[ISTART], HyperConnectionFactory[ISTART,END with AbstractRelation[ISTART,IEND],IEND] with UuidNodeFactory[END], UuidNodeFactory[IEND]) => EndContentRelationHyperAccess[ISTART, IEND, START, RELATION, END] = {
    (startFactory, baseFactory, endFactory) => new EndContentRelationHyperAccess(factory, nodeFactory, startFactory, baseFactory, endFactory)
  }
}
