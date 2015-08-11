package modules.db.access.custom

import controllers.api.nodes.RequestContext
import formatters.json.RequestFormat._
import model.WustSchema.{Created => SchemaCreated, _}
import modules.db.Database.db
import modules.db.access.{NodeRead, NodeReadDelete}
import modules.requests._
import play.api.libs.json.JsValue
import renesca.parameter.implicits._
import play.api.mvc.Results._

class UserAccess extends NodeRead(User) {
  override def update(context: RequestContext, uuid: String) = {
    context.withRealUser { user =>
      context.withJson { (request: UserUpdateRequest) =>
        //TODO: sanity check + welcome mail
        if (request.email.isDefined)
          user.email = request.email

        db.transaction(_.persistChanges(user)) match {
          case Some(err) => Left(BadRequest(s"Cannot update User: $err'"))
          case _         => Right(user)
        }
      }
    }
  }
}

object UserAccess {
  def apply = new UserAccess
}
