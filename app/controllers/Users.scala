package controllers

import play.api.libs.json._
import play.api.mvc.Action
import play.api.mvc.Controller

import services.UserServiceImpl.users

object Users extends ResourceRouter with DefaultResourceController {
  implicit val restFormat = formatters.json.UserFormats.RestFormat

  override def show(id: String) = Action {
    //TODO: not viable
    users.values.find(_.id == id) match {
      case Some(user) => Ok(Json.toJson(user))
      case None       => BadRequest(s"User with id '$id' not found.")
    }
  }

  override def index() = Action {
    Ok(Json.toJson(users.values))
  }
}
