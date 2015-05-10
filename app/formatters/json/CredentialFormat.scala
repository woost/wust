package formatters.json

import com.mohiva.play.silhouette.api.util.Credentials
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Contain all format for com.mohiva.play.silhouette.api.providers.Credentials type
 */
object CredentialFormat {

  implicit val restFormat = (
    (__ \ "identifier").format[String] ~
    (__ \ "password").format[String])(Credentials.apply, unlift(Credentials.unapply))

}