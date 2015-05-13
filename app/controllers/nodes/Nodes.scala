package controllers.nodes

import controllers.router.NestedResourceRouter
import model.WustSchema.UuidNode
import modules.auth.HeaderEnvironmentModule

trait Nodes[NODE <: UuidNode] extends ReadableNodes[NODE] with DeletableNodes[NODE] with WritableNodes[NODE]
