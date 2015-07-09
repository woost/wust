package model.auth

import org.joda.time.DateTime
import play.api.libs.json._

case class Token(token: String, expiresOn: DateTime, userId: String)

object Token {

  implicit val jodaDateWrites: Writes[org.joda.time.DateTime] = new Writes[org.joda.time.DateTime] {
    def writes(d: org.joda.time.DateTime): JsValue = JsString(d.toString)
  }

  implicit val restFormat = Json.format[Token]

}
