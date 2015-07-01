package modules.db.access.custom

import formatters.json.RequestFormat._
import model.WustSchema._
import modules.db.Database.db
import modules.db.HyperNodeDefinitionBase
import modules.db.access.{EndRelationReadDelete, StartRelationReadDelete}
import modules.requests.{NodeAddRequest, ConnectRequest}
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
    //TODO: workaround create should NOT! be done here :D
    js.validate[NodeAddRequest].map(request => {
      val node = Post.create(description = request.description, title = request.title)
      val failure = db.persistChanges(node)
      if(failure.isDefined)
        Right("Cannot create post")
      else
        handler(ConnectRequest(node.uuid))
    }).getOrElse(js.validate[ConnectRequest].map(t => handler(t)).getOrElse(Right("Cannot parse connect request")))
  }
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

  override def create(uuid: String, user: User, json: JsValue) = {
    parseConnect(json)(request => {
      val discourse = Discourse.empty
      val base = baseFactory.matchesOnUuid(uuid)
      val node = nodeFactory.matchesOnUuid(request.uuid)
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
  ](factory: ContentRelationFactory[START, RELATION, END], nodeFactory: UuidNodeMatchesFactory[END]): UuidNodeMatchesFactory[START] => StartContentRelationAccess[START, RELATION, END] = {
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
  nodeFactory: UuidNodeMatchesFactory[END],
  startFactory: UuidNodeMatchesFactory[ISTART],
  baseFactory: HyperConnectionFactory[ISTART, START with AbstractRelation[ISTART, IEND], IEND] with NodeFactory[START],
  endFactory: UuidNodeMatchesFactory[IEND]
  ) extends StartRelationReadDelete(factory, nodeFactory) with ContentRelationHelper {

  override def createHyper(startUuid: String, endUuid: String, user: User, json: JsValue) = {
    parseConnect(json)(request => {
      val discourse = Discourse.empty
      val start = startFactory.matchesOnUuid(startUuid)
      val end = endFactory.matchesOnUuid(endUuid)
      val base = baseFactory.matchesHyperConnection(start, end)
      val node = nodeFactory.matchesOnUuid(request.uuid)
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
  ](factory: ContentRelationFactory[START, RELATION, END], nodeFactory: UuidNodeMatchesFactory[END]): (UuidNodeMatchesFactory[ISTART], HyperConnectionFactory[ISTART, START with AbstractRelation[ISTART, IEND], IEND] with NodeFactory[START], UuidNodeMatchesFactory[IEND]) => StartContentRelationHyperAccess[ISTART, IEND, START, RELATION, END] = {
    (startFactory, baseFactory, endFactory) => new StartContentRelationHyperAccess(factory, nodeFactory, startFactory, baseFactory, endFactory)
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

  override def create(uuid: String, user: User, json: JsValue) = {
    parseConnect(json)(request => {
      val discourse = Discourse.empty
      val base = baseFactory.matchesOnUuid(uuid)
      val node = nodeFactory.matchesOnUuid(request.uuid)
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
  ](factory: ContentRelationFactory[START, RELATION, END], nodeFactory: UuidNodeMatchesFactory[START]): UuidNodeMatchesFactory[END] => EndContentRelationAccess[START, RELATION, END] = {
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
  nodeFactory: UuidNodeMatchesFactory[START],
  startFactory: UuidNodeMatchesFactory[ISTART],
  baseFactory: HyperConnectionFactory[ISTART, END with AbstractRelation[ISTART, IEND], IEND] with NodeFactory[END],
  endFactory: UuidNodeMatchesFactory[IEND]
  ) extends EndRelationReadDelete(factory, nodeFactory) with ContentRelationHelper {

  override def createHyper(startUuid: String, endUuid: String, user: User, json: JsValue) = {
    parseConnect(json)(request => {
      val discourse = Discourse.empty
      val start = startFactory.matchesOnUuid(startUuid)
      val end = endFactory.matchesOnUuid(endUuid)
      val base = baseFactory.matchesHyperConnection(start, end)
      val node = nodeFactory.matchesOnUuid(request.uuid)
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
  ](factory: ContentRelationFactory[START, RELATION, END], nodeFactory: UuidNodeMatchesFactory[START]): (UuidNodeMatchesFactory[ISTART], HyperConnectionFactory[ISTART, END with AbstractRelation[ISTART, IEND], IEND] with NodeFactory[END], UuidNodeMatchesFactory[IEND]) => EndContentRelationHyperAccess[ISTART, IEND, START, RELATION, END] = {
    (startFactory, baseFactory, endFactory) => new EndContentRelationHyperAccess(factory, nodeFactory, startFactory, baseFactory, endFactory)
  }
}
