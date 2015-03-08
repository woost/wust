package modules.requests

trait UserRequest
trait NodeAddRequest extends UserRequest {
  def title: String
}
case class ProblemAddRequest(title: String) extends NodeAddRequest
case class IdeaAddRequest(title: String) extends NodeAddRequest
case class GoalAddRequest(title: String) extends NodeAddRequest
