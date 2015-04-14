package formatters.json

import play.api.libs.json._
import play.api.libs.functional.syntax._
import com.mohiva.play.silhouette.api.util.Credentials

/**
 *
 */
object CredentialFormat {

  implicit val restFormat = (
    (__ \ "identifier").format[String] ~
    (__ \ "password").format[String])(Credentials.apply, unlift(Credentials.unapply))

}