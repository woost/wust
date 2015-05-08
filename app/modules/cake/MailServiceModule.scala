package modules.cake

import model.users._
import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import services._

trait MailServiceModule {

  class SimpleMailService extends MailService[User] {

    def sendWelcomeEmail(user: User)(implicit request: RequestHeader, lang: Lang) = {
      val html = views.html.authentication.mails.welcomeEmail(user)(request, lang)
      val txtAndHtml = (None, Some(html))
      sendEmail("Welcome!!!!", user.email.get, txtAndHtml)
    }

  }

  lazy val mailService = new SimpleMailService

}