package controllers

import modules.requests._
import renesca._
import model.WustSchema._

object ConArguments extends ContentNodes[ConArgument] {
  override def nodeSchema = NodeSchema("cons", ConArgument, Map())
}
