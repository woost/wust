package controllers

import controllers.api._
import formatters.json.ApiSchemaFormat._
import modules.requests._
import play.api.libs.json._
import play.api.mvc._
import play.api.Play.current
import common.ConfigString._

object Application extends Controller {
  //TODO create embedding type RequestSchema(ApiDefinition, Seq[NodeSchema])
  val apiDefinition = ApiDefinition("/api/v1", "/live/v1");
  val nodeSchemas = Seq(Posts, Scopes, Users, Connectables, ConnectsCtrl, ChangeRequests).map(_.nodeSchema)

  private val apiDefinitionJson = Json.toJson(apiDefinition);
  private val nodeSchemasJson = Json.toJson(JsArray(nodeSchemas.map(Json.toJson(_))));

  val scratchpadEnabled = "ui.scratchpad.enabled".configOrElse(true)
  val brandingText = "ui.branding.text".configOrElse("")
  val brandingLogo = "ui.branding.logo".configOrElse("")
  val brandingColor = "ui.branding.color".configOrElse("")
  val tutorialEnabled = "ui.tutorial.enabled".configOrElse(false)
  val registrationEnabled = "ui.registration.enabled".configOrElse(true)
  val publicReadingEnabled = "ui.publicReading.enabled".configOrElse(true)
  val inspectletId = "tracking.inspectlet.id".configOrElse("")
  val googleanalyticsId = "tracking.googleanalytics.id".configOrElse("")

  println(s"scratchpadEnabled: $scratchpadEnabled")
  println(s"brandingText: $brandingText")
  println(s"brandingLogo: $brandingLogo")
  println(s"brandingColor: $brandingColor")
  println(s"tutorialEnabled: $tutorialEnabled")
  println(s"registrationEnabled: $registrationEnabled")
  println(s"publicReadingEnabled: $publicReadingEnabled")
  println(s"inspectletId: $inspectletId")
  println(s"googleanalyticsId: $googleanalyticsId")

  def index(any: String) = Action {
    Ok(views.html.index(
      JsObject(Seq(
        ("api", apiDefinitionJson),
        ("models", nodeSchemasJson)
      )),
      JsObject(Seq(
        ("scratchpadEnabled", JsBoolean(scratchpadEnabled)),
        ("brandingText", JsString(brandingText)),
        ("brandingLogo", JsString(brandingLogo)),
        ("brandingColor", JsString(brandingColor)),
        ("tutorialEnabled", JsBoolean(tutorialEnabled)),
        ("registrationEnabled", JsBoolean(registrationEnabled)),
        ("publicReadingEnabled", JsBoolean(publicReadingEnabled))
      ))
    ))
  }
}
