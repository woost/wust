package controllers.api.nodes

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import controllers.api.router.{DefaultNestedResourceController, NestedResourceRouter}
import model.WustSchema.{User, UuidNode}
import modules.auth.HeaderEnvironmentModule
import modules.requests.{NodeSchema, ConnectSchema, HyperConnectSchema}
import play.api.mvc.Result
import renesca.schema.Node
import renesca.parameter.implicits._

trait NodesBase extends NestedResourceRouter with DefaultNestedResourceController with Silhouette[User, JWTAuthenticator] with HeaderEnvironmentModule {
  protected def unauthorized = Unauthorized("Only logged-in users allowed")
  protected def pathNotFound = NotFound("No defined path")

  protected def getSchema[NODE <: UuidNode](schemas: Map[String,_ <: ConnectSchema[NODE]], path: String)(handler: ConnectSchema[NODE] => Result) = {
    schemas.get(path) match {
      case Some(schema) => handler(schema)
      case None         => pathNotFound
    }
  }

  protected def getSchemaInHyper(schemas: Map[String,_ <: ConnectSchema[_]], path: String)(handler: ConnectSchema[_] => Result) = {
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

  protected def getNestedSchema[NODE <: UuidNode](schemas: Map[String,_ <: ConnectSchema[NODE]], path: String, nestedPath: String)(handler: (ConnectSchema[NODE], ConnectSchema[_]) => Result) = {
    getHyperSchema(schemas, path) (c =>
      getSchemaInHyper(c.connectSchemas, nestedPath)(handler(c,_))
    )
  }

  protected def getResult[T](result: Either[T,String])(handler: T => Result): Result = {
    result match {
      case Left(value) => handler(value)
      case Right(msg)  => BadRequest(msg)
    }
  }

  protected def getUser(identity: Option[User])(handler: User => Result): Result = {
    identity match {
      case Some(user) => handler(user)
      //case None => unauthorized
      //TODO: devel: just the devel user
      case None =>
        if (play.api.Play.isDev(play.api.Play.current)) {
          val user = User.merge(name = "devel", merge = Set("name"))
          handler(user)
        } else
          unauthorized

    }
  }

  protected def validateConnect(uuid: String, otherUuid: String)(handler: () => Result): Result = {
    if (uuid == otherUuid)
      BadRequest("Self loops are not allowed")
    else
      handler()
  }
}

trait Nodes[NODE <: UuidNode] extends ReadableNodes[NODE] with DeletableNodes[NODE] with WritableNodes[NODE] {
  val node: (String) => NodeSchema[NODE]
  // needs to be lazy, otherwise routePath might be not set by the router
  lazy val nodeSchema = node(routePath)
}
