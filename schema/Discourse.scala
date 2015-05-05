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

  @Relation trait ContentRelation

  //TODO: generate indirect neighbour-accessors based on hyperrelations
  @Node class Goal extends ContentNode
  @Node class Problem extends ContentNode
  @Node class Idea extends ContentNode
  @Node class ProArgument extends ContentNode
  @Node class ConArgument extends ContentNode

  @Relation class SubIdea(startNode: Idea, endNode: Idea) extends ContentRelation
  @Relation class SubGoal(startNode: Goal, endNode: Goal) extends ContentRelation
  @Relation class Causes(startNode: Problem, endNode: Problem) extends ContentRelation

  @Relation class Prevents(startNode: Problem, endNode: Goal) extends ContentRelation
  @HyperRelation class Solves(startNode: Idea, endNode: Problem) extends ContentRelation with UuidNode
  @HyperRelation class Achieves(startNode: Idea, endNode: Goal) extends ContentRelation with UuidNode

  @Relation class SupportsSolution(startNode: ProArgument, endNode: Solves) extends ContentRelation
  @Relation class OpposesSolution(startNode: ConArgument, endNode: Solves) extends ContentRelation
  @Relation class SupportsAchievement(startNode: ProArgument, endNode: Achieves) extends ContentRelation
  @Relation class OpposesAchievement(startNode: ConArgument, endNode: Achieves) extends ContentRelation
}
