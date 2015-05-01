package model

import renesca.schema._

@macros.GraphSchema
object WustSchema {
  //TODO: Type aliases for several HyperRelation combinations
  @Group trait Discourse {List(Problem, Idea, Goal, ProArgument, ConArgument) }


  @Node trait UuidNode {
    val uuid: String = java.util.UUID.randomUUID.toString
  }
  @Node trait ContentNode extends UuidNode {
    var title: String
    var description: Option[String]
  }

  //TODO: generate indirect neighbour-accessors based on hyperrelations
  @Node class Goal extends ContentNode
  @Node class Problem extends ContentNode
  @Node class Idea extends ContentNode
  @Node class ProArgument extends ContentNode
  @Node class ConArgument extends ContentNode

  @Relation class SubIdea(startNode: Idea, endNode: Idea)
  @Relation class SubGoal(startNode: Goal, endNode: Goal)
  @Relation class Causes(startNode: Problem, endNode: Problem)

  @Relation class Prevents(startNode: Problem, endNode: Goal)
  @HyperRelation class Solves(startNode: Idea, endNode: Problem) extends UuidNode
  @HyperRelation class Achieves(startNode: Idea, endNode: Goal) extends UuidNode

  @Relation class SupportsSolution(startNode: ProArgument, endNode: Solves)
  @Relation class OpposesSolution(startNode: ConArgument, endNode: Solves)
  @Relation class SupportsAchievement(startNode: ProArgument, endNode: Achieves)
  @Relation class OpposesAchievement(startNode: ConArgument, endNode: Achieves)
}
