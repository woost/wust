package controllers

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import modules.auth.HeaderEnvironmentModule
import play.api.libs.json._
import play.api.mvc.Action
import play.api.mvc.Controller

import services.UserServiceDB

import scala.concurrent.Await
import scala.concurrent.duration._

object Users extends ResourceRouter with DefaultResourceController with HeaderEnvironmentModule {
  implicit val restFormat = formatters.json.UserFormats.RestFormat

  override def show(id: String) = Action {
    //TODO: not viable
    Await.result(userService.retrieve(id), Duration(5, SECONDS)) match {
      case Some(user) => Ok(Json.toJson(user))
      case None       => BadRequest(s"User with id '$id' not found.")
    }
  }

  override def index() = Action {
    Ok(Json.toJson(Await.result(userService.retrieve, Duration(5, SECONDS))))
  }
}
