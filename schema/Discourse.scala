package model

import renesca.schema._

@macros.GraphSchema
object WustSchema {
  @Group trait Discourse {List(Problem, Idea, Goal, ProArgument, ConArgument) }


  @Node trait UuidNode {
    val uuid: String
  }
  @Node trait ContentNode extends UuidNode {
    var title: String
    var description: String
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


  trait UUIDNodeFactory[T <: UuidNode] extends SchemaNodeFactory[T] {
    override def local = UUID.applyTo(super.local)
  }

  trait ContentNodeFactory[T <: ContentNode] extends UUIDNodeFactory[T] {
    def local(title: String): T = {
      val node = super.local
      node.title = title
      node
    }
  }

}
