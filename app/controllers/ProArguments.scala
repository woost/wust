package controllers

import modules.requests._
import renesca._
import model.WustSchema._

object ProArguments extends ContentNodes[ProArgument] {
  override def nodeSchema = NodeSchema("pros", ProArgument, Map())
}
