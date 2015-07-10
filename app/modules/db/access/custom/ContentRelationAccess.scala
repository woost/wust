package modules.db.access.custom

import formatters.json.RequestFormat._
import model.WustSchema._
import modules.db.Database.db
import modules.db.HyperNodeDefinitionBase
import modules.db.access.{NodeAccess, EndRelationReadDelete, StartRelationReadDelete}
import modules.requests.{NodeAddRequest}
import org.atmosphere.config.service.Post
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
}

//TODO: track what the user did here
class StartContentRelationAccess[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](
  override val factory: ContentRelationFactory[START, RELATION, END],
  nodeFactory: UuidNodeMatchesFactory[END],
  baseFactory: UuidNodeMatchesFactory[START]) extends StartRelationReadDelete(factory, nodeFactory) with ContentRelationHelper {

  override def create(uuid: String, user: User, otherUuid: String) = {
      val discourse = Discourse.empty
      val base = baseFactory.matchesOnUuid(uuid)
      val node = nodeFactory.matchesOnUuid(otherUuid)
      discourse.add(factory.mergeContentRelation(base, node))
      persistRelation(discourse, node)
  }
}

object StartContentRelationAccess {
  def apply[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](factory: ContentRelationFactory[START, RELATION, END], nodeFactory: UuidNodeMatchesFactory[END], nodeAccess: Option[NodeAccess[END]] = None): UuidNodeMatchesFactory[START] => StartContentRelationAccess[START, RELATION, END] = {
    baseFactory => {
      val access = new StartContentRelationAccess(factory, nodeFactory, baseFactory)
      nodeAccess.foreach(access.withCreate(_))
      access
    }
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
  nodeFactory: UuidNodeMatchesFactory[END],
  startFactory: UuidNodeMatchesFactory[ISTART],
  baseFactory: HyperConnectionFactory[ISTART, START with AbstractRelation[ISTART, IEND], IEND] with UuidNodeFactory[START],
  endFactory: UuidNodeMatchesFactory[IEND]
  ) extends StartRelationReadDelete(factory, nodeFactory) with ContentRelationHelper {

  override def createHyper(startUuid: String, endUuid: String, user: User, nestedUuid: String) = {
      val discourse = Discourse.empty
      val start = startFactory.matchesOnUuid(startUuid)
      val end = endFactory.matchesOnUuid(endUuid)
      val base = baseFactory.matchesHyperConnection(start, end)
      val node = nodeFactory.matchesOnUuid(nestedUuid)
      val relation = factory.mergeContentRelation(base, node)
      discourse.add(base, relation)
      persistRelation(discourse, node)
  }
}

object StartContentRelationHyperAccess {
  def apply[
  ISTART <: UuidNode,
  IEND <: UuidNode,
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](factory: ContentRelationFactory[START, RELATION, END], nodeFactory: UuidNodeMatchesFactory[END], startFactory: UuidNodeMatchesFactory[ISTART], endFactory: UuidNodeMatchesFactory[IEND], nodeAccess: Option[NodeAccess[END]] = None): (HyperConnectionFactory[ISTART, START with AbstractRelation[ISTART, IEND], IEND] with UuidNodeFactory[START]) => StartContentRelationHyperAccess[ISTART, IEND, START, RELATION, END] = {
    baseFactory => {
      val access = new StartContentRelationHyperAccess(factory, nodeFactory, startFactory, baseFactory, endFactory)
      nodeAccess.foreach(access.withCreate(_))
      access
    }
  }
}

class EndContentRelationAccess[
START <: UuidNode,
RELATION <: AbstractRelation[START, END],
END <: UuidNode
](
  override val factory: ContentRelationFactory[START, RELATION, END],
  nodeFactory: UuidNodeMatchesFactory[START],
  baseFactory: UuidNodeMatchesFactory[END]) extends EndRelationReadDelete(factory, nodeFactory) with ContentRelationHelper {

  override def create(uuid: String, user: User, otherUuid: String) = {
    val discourse = Discourse.empty
    val base = baseFactory.matchesOnUuid(uuid)
    val node = nodeFactory.matchesOnUuid(otherUuid)
    discourse.add(factory.mergeContentRelation(node, base))
    persistRelation(discourse, node)
  }
}

object EndContentRelationAccess {
  def apply[
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](factory: ContentRelationFactory[START, RELATION, END], nodeFactory: UuidNodeMatchesFactory[START], nodeAccess: Option[NodeAccess[START]] = None): UuidNodeMatchesFactory[END] => EndContentRelationAccess[START, RELATION, END] = {
    baseFactory => {
      val access = new EndContentRelationAccess(factory, nodeFactory, baseFactory)
      nodeAccess.foreach(access.withCreate(_))
      access
    }
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
  nodeFactory: UuidNodeMatchesFactory[START],
  startFactory: UuidNodeMatchesFactory[ISTART],
  baseFactory: HyperConnectionFactory[ISTART, END with AbstractRelation[ISTART, IEND], IEND] with UuidNodeFactory[END],
  endFactory: UuidNodeMatchesFactory[IEND]
  ) extends EndRelationReadDelete(factory, nodeFactory) with ContentRelationHelper {

  override def createHyper(startUuid: String, endUuid: String, user: User, nestedUuid: String) = {
      val discourse = Discourse.empty
      val start = startFactory.matchesOnUuid(startUuid)
      val end = endFactory.matchesOnUuid(endUuid)
      val base = baseFactory.matchesHyperConnection(start, end)
      val node = nodeFactory.matchesOnUuid(nestedUuid)
      val relation = factory.mergeContentRelation(node, base)
      discourse.add(base, relation)
      persistRelation(discourse, node)
  }
}

object EndContentRelationHyperAccess {
  def apply[
  ISTART <: UuidNode,
  IEND <: UuidNode,
  START <: UuidNode,
  RELATION <: AbstractRelation[START, END],
  END <: UuidNode
  ](factory: ContentRelationFactory[START, RELATION, END], nodeFactory: UuidNodeMatchesFactory[START], startFactory: UuidNodeMatchesFactory[ISTART], endFactory: UuidNodeMatchesFactory[IEND], nodeAccess: Option[NodeAccess[START]] = None): (HyperConnectionFactory[ISTART, END with AbstractRelation[ISTART, IEND], IEND] with UuidNodeFactory[END]) => EndContentRelationHyperAccess[ISTART, IEND, START, RELATION, END] = {
    baseFactory => {
      val access = new EndContentRelationHyperAccess(factory, nodeFactory, startFactory, baseFactory, endFactory)
      nodeAccess.foreach(access.withCreate(_))
      access
    }
  }
}
