package controllers.nodes

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import controllers.router.{DefaultNestedResourceController, NestedResourceRouter}
import model.WustSchema.{User, UuidNode}
import modules.auth.HeaderEnvironmentModule
import modules.requests.types.AccessibleConnectSchema
import modules.requests.{ConnectSchema, HyperConnectSchema}
import play.api.mvc.Result

trait NodesBase extends NestedResourceRouter with DefaultNestedResourceController with Silhouette[User, JWTAuthenticator] with HeaderEnvironmentModule {
  protected def pathNotFound = NotFound("No defined path")

  protected def getSchema[NODE <: UuidNode](schemas: Map[String,_ <: AccessibleConnectSchema[NODE]], path: String)(handler: ConnectSchema[NODE] with AccessibleConnectSchema[NODE] => Result) = {
    schemas.get(path) match {
      case Some(schema) => handler(schema)
      case None         => pathNotFound
    }
  }

  protected def getHyperSchema[NODE <: UuidNode](schemas: Map[String,_ <: AccessibleConnectSchema[NODE]], path: String)(handler: HyperConnectSchema[NODE] => Result) = {
    schemas.get(path) match {
      case Some(schema) => schema match {
        case c: HyperConnectSchema[NODE] => handler(c)
        case _ => pathNotFound
      }
      case None         => pathNotFound
    }
  }

  protected def getResult[T](result: Either[T,String])(handler: T => Result): Result = {
    result match {
      case Left(value) => handler(value)
      case Right(msg)  => BadRequest(msg)
    }
  }
}

trait Nodes[NODE <: UuidNode] extends ReadableNodes[NODE] with DeletableNodes[NODE] with WritableNodes[NODE]
