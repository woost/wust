package controllers

import modules.requests._
import renesca._
import model.WustSchema._

object ConArguments extends ContentNodesController[ConArgument] {
  override def nodeSchema = NodeSchema("cons", ConArgument, Map())
}
