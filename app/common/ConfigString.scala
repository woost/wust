package common

import play.api.Application

object ConfigString {
  // https://coderwall.com/p/essqgw/easier-access-to-configuration-values-in-play-application

  implicit class ConfigStr(s: String) {
    def configOrElse(default: Long)(implicit app: Application): Long =
      app.configuration.getLong(s).getOrElse(default)

    def configOrElse(default: Double)(implicit app: Application): Double =
      app.configuration.getDouble(s).getOrElse(default)

    def configOrElse(default: String)(implicit app: Application): String =
      app.configuration.getString(s).getOrElse(default)

    def configOrElse(default: Boolean)(implicit app: Application): Boolean =
      app.configuration.getBoolean(s).getOrElse(default)
  }
}
