package wust

import scala.scalajs.js.JSApp
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.scalajs.js
import scala.scalajs.js.Any.{ fromBoolean, fromFunction1, fromString, jsArrayOps, wrapArray }
import scala.scalajs.js.Dynamic.literal
import scala.scalajs.js.JSConverters.JSRichGenTraversableOnce
import scala.scalajs.js.UndefOr
import scala.scalajs.js.UndefOr.undefOr2ops
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.annotation.JSExportAll
import scala.util.{ Failure, Success }

import js.Dynamic.{ global => g }

@JSExport
object Wust extends JSApp {
  def main() {
    // g.alert("Hello from ScalaJs!")
    println("Hello from ScalaJs!")
  }
}
