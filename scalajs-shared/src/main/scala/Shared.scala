package wust

import scala.scalajs.js.annotation.{JSExport, JSExportAll}

@JSExport
object Shared {
  def hashColor(obj:Any) = ((obj.hashCode % 360) + 360) % 360
  @JSExport def tagTitleColor(title:String) = hashColor(title.toLowerCase)
}
