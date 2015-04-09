package controllers

import play.api.mvc._
import play.api.libs.json._

object Application extends Controller {
  def index(any: String) = Action {
    Ok(views.html.index(Json.parse("""{
      "models": [
        {
          "name": "Goal",
          "path": "goals",
          "label": "GOAL",
          "subs": [
            {
              "path": "goals",
              "type": "hasMany"
            },
            {
              "path": "problems",
              "type": "hasMany"
            },
            {
              "path": "ideas",
              "type": "hasMany"
            }
          ]
        },
        {
          "name": "Problem",
          "path": "problems",
          "label": "PROBLEM",
          "subs": [
            {
              "path": "goals",
              "type": "hasMany"
            },
            {
              "path": "problems",
              "type": "hasMany"
            },
            {
              "path": "ideas",
              "type": "hasMany"
            }
          ]
        },
        {
          "name": "Idea",
          "path": "ideas",
          "label": "IDEA",
          "subs": [
            {
              "path": "goals",
              "type": "hasMany"
            },
            {
              "path": "problems",
              "type": "hasMany"
            },
            {
              "path": "ideas",
              "type": "hasMany"
            }
          ]
        }
      ]
    }""")))
  }
}
