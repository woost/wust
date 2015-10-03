package controllers.api.nodes

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import controllers.Application
import controllers.api.router.{DefaultNestedResourceController, NestedResourceRouter}
import model.WustSchema._
import modules.auth.HeaderEnvironmentModule
import modules.requests.{ConnectSchema, HyperConnectSchema, NodeSchema}
import play.api.libs.json.JsValue
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Controller, Result}
import renesca.schema._

trait NodesBase extends NestedResourceRouter with DefaultNestedResourceController with Silhouette[User, JWTAuthenticator] with HeaderEnvironmentModule {

  protected def context(request: UserAwareRequest[AnyContent]) = {
    RequestContext(this, request.identity, request.body.asJson, request.queryString.flatMap { case (k, v) => v.headOption.map((k, _)) })
  }

  protected def unauthorized = Unauthorized("Only logged-in users allowed")
  protected def pathNotFound = NotFound("No defined path")

  protected def getSchema[NODE <: UuidNode](schemas: Map[String, _ <: ConnectSchema[NODE]], path: String)(handler: ConnectSchema[NODE] => Result) = {
    schemas.get(path) match {
      case Some(schema) => handler(schema)
      case None         => pathNotFound
    }
  }

  protected def getHyperSchema[NODE <: UuidNode](schemas: Map[String, _ <: ConnectSchema[NODE]], path: String)(handler: HyperConnectSchema[NODE] => Result) = {
    schemas.get(path) match {
      case Some(schema) => schema match {
        case c: HyperConnectSchema[NODE] => handler(c)
        case _                           => pathNotFound
      }
      case None         => pathNotFound
    }
  }

  protected def validateConnect(uuid: String, otherUuid: String)(handler: () => Result): Result = {
    if(uuid == otherUuid)
      BadRequest("Self loops are not allowed")
    else
      handler()
  }

  protected def validateHyperConnect(uuid: String, otherUuid: String, nestedUuid: String)(handler: () => Result): Result = validateConnect(uuid, otherUuid) { () =>
    if(uuid == nestedUuid || otherUuid == nestedUuid)
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
