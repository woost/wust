package controllers

import model.{Idea, Problem, Goal, Discourse}
import modules.json.GraphFormat._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import renesca.Query
import renesca.graph.Label
import renesca.parameter.implicits._

object Search extends Controller with DatabaseController {
  def search(term: String, labelOpt:Option[Label]) = Action {
    val regexTerm = ".*" + term.replace(" ", ". *") + ".*"
    val nodeMatch = labelOpt match {
      case Some(label) => s"n:`$label`"
      case None => "n"
    }
    val discourse = Discourse(db.queryGraph(Query(s"match ($nodeMatch) where n.title =~ {term} return n limit 15", Map("term" -> regexTerm))))

    Ok(Json.toJson(discourse.nodes))
  }

  def index(term: String) = search(term, None)
  def indexGoals(term: String) = search(term, Some(Goal.label))
  def indexProblems(term: String) = search(term, Some(Problem.label))
  def indexIdeas(term: String) = search(term, Some(Idea.label))
}

