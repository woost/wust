package controllers.api.nodes

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import controllers.api.router.{DefaultNestedResourceController, NestedResourceRouter}
import model.WustSchema._
import modules.auth.HeaderEnvironmentModule
import modules.requests.{ConnectSchema, HyperConnectSchema, NodeSchema}
import play.api.libs.json.{JsResult, JsValue}
import play.api.mvc.{Controller, AnyContent, Result}
import play.api.mvc.Results._
import renesca.parameter.implicits._
import controllers.Application
import renesca.schema._

case class RequestContext(controller: NodesBase with Controller, user: Option[User], json: Option[JsValue], query: Map[String, String]) {
  def page = query.get("page").map(_.toInt)
  def size = query.get("size").map(_.toInt)
  def limit = size.getOrElse(15)
  def countView = query.get("countView").map(_.toBoolean).getOrElse(false)

  def jsonAs[T](implicit rds : play.api.libs.json.Reads[T]) = {
    json.flatMap(_.validate[T].asOpt)
  }

  def withJson[S,T](handler: S => Either[Result,T])(implicit rds : play.api.libs.json.Reads[S]) = {
    jsonAs[S].map(handler(_)).getOrElse(Left(UnprocessableEntity("Cannot parse json body")))
  }

  def withUser[T](handler: User => Either[Result,T]): Either[Result,T] = {
    user.map(handler(_)).getOrElse(Left(onlyUsers))
  }

  def withUser[T](handler: => Either[Result,T]): Either[Result,T] = {
    user.map(_ => handler).getOrElse(Left(onlyUsers))
  }

  private def onlyUsers = Forbidden("Only for users")
}

case class ConnectParameter[+BASE <: UuidNode](
  baseFactory: UuidNodeMatchesFactory[BASE],
  baseUuid: String
)

case class HyperConnectParameter[START <: UuidNode, +BASE <: UuidNode with AbstractRelation[START,END], END <: UuidNode](
  startFactory: UuidNodeMatchesFactory[START],
  startUuid: String,
  baseFactory: HyperConnectionFactory[START, BASE, END] with UuidNodeMatchesFactory[BASE],
  endFactory: UuidNodeMatchesFactory[END],
  endUuid: String
)

object SchemaWrapper extends RootNodeTraitFactory[UuidNode] {
  def wrapNode[N <: Node](node: N) = {
    val discourse = Discourse(node.graph)
    val newNode = super.wrap(node.rawItem)
    discourse.add(newNode)
    newNode
  }
}

trait NodesBase extends NestedResourceRouter with DefaultNestedResourceController with Silhouette[User, JWTAuthenticator] with HeaderEnvironmentModule {

  protected def context(request: UserAwareRequest[AnyContent]) = {
    RequestContext(this, request.identity, request.body.asJson, request.queryString.flatMap { case (k,v) => v.headOption.map((k, _)) })
  }

  protected def unauthorized = Unauthorized("Only logged-in users allowed")
  protected def pathNotFound = NotFound("No defined path")

  protected def getSchema[NODE <: UuidNode](schemas: Map[String,_ <: ConnectSchema[NODE]], path: String)(handler: ConnectSchema[NODE] => Result) = {
    schemas.get(path) match {
      case Some(schema) => handler(schema)
      case None         => pathNotFound
    }
  }

  protected def getHyperSchema[NODE <: UuidNode](schemas: Map[String,_ <: ConnectSchema[NODE]], path: String)(handler: HyperConnectSchema[NODE] => Result) = {
    schemas.get(path) match {
      case Some(schema) => schema match {
        case c: HyperConnectSchema[NODE] => handler(c)
        case _ => pathNotFound
      }
      case None         => pathNotFound
    }
  }

  protected def getResult[T](result: Either[Result, T])(handler: T => Result): Result = {
    result match {
      case Left(msg)  => msg
      case Right(value) => handler(value)
    }
  }

  protected def validateConnect(uuid: String, otherUuid: String)(handler: () => Result): Result = {
    if (uuid == otherUuid)
      BadRequest("Self loops are not allowed")
    else
      handler()
  }

  protected def validateHyperConnect(uuid: String, otherUuid: String, nestedUuid: String)(handler: () => Result): Result = validateConnect(uuid, otherUuid) { () =>
    if (uuid == nestedUuid || otherUuid == nestedUuid)
      BadRequest("Incident loops are not allowed")
    else
      handler()
  }
}

trait Nodes[NODE <: UuidNode] extends ReadableNodes[NODE] with DeletableNodes[NODE] with WritableNodes[NODE] {
  val node: (String) => NodeSchema[NODE]
  // needs to be lazy, otherwise prefix might not be set by the router
  lazy val nodeSchema = node(prefix.stripPrefix(Application.apiDefinition.restRoot))
}
