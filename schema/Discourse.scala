package model

import renesca.schema._

@macros.GraphSchema
object WustSchema {
  @Schema class Discourse {
    // TODO: generate Sets for all node traits
    def nodes: Set[DiscourseNode]
  }

  trait DiscourseNodeFactory[T <: DiscourseNode] extends SchemaNodeFactory[T] {
    override def local = UUID.applyTo(super.local)
  }

  trait ContentNodeFactory[T <: ContentNode] extends DiscourseNodeFactory[T] {
    def local(title: String): T = {
      val node = super.local
      node.title = title
      node
    }
  }

  @Node trait DiscourseNode extends SchemaNode {
    //TODO: title only belongs to ContentNode
    var title: String
    val uuid: String
  }
  //TODO: infer factory name
  @Node trait ContentNode extends DiscourseNode {def factory: ContentNodeFactory }

  //TODO: generate indirect neighbour-accessors based on hypernodes
  //TODO: named node/relation groups (based on nodeTraits?)


  @Node
  class Goal extends ContentNode {
    //    def ideas = IdeaToReaches <-- ReachesToGoal
  }
  @Node
  class Problem extends ContentNode {
    //    def ideas = IdeaToSolves <-- SolvesToProblem
  }
  @Node
  class Idea extends ContentNode {
    //    def problems = IdeaToSolves --> SolvesToProblem
    //    def goals = IdeaToReaches --> ReachesToGoal
  }
  @Node class ProArgument extends ContentNode
  @Node class ConArgument extends ContentNode

  @HyperRelation class Solves(startNode: Idea, endNode: Problem) extends DiscourseNode
  @HyperRelation class Achieves(startNode: Idea, endNode: Goal) extends DiscourseNode

  @Relation class SubIdea(startNode: Idea, endNode: Idea)
  @Relation class SubGoal(startNode: Goal, endNode: Goal)
  @Relation class Causes(startNode: Problem, endNode: Problem)
  @Relation class Prevents(startNode: Problem, endNode: Goal)
  @Relation class SupportsSolution(startNode: ProArgument, endNode: Solves)
  @Relation class OpposesSolution(startNode: ConArgument, endNode: Solves)
  @Relation class SupportsAchievement(startNode: ProArgument, endNode: Achieves)
  @Relation class OpposesAchievement(startNode: ConArgument, endNode: Achieves)
}

