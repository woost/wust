package controllers

import modules.requests._
import renesca._
import model.WustSchema._

object ProArguments extends ContentNodesController[ConArgument] {
  override def nodeSchema = NodeSchema("pros", ConArgument, Map())
}
