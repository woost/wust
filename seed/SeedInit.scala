package tasks

import common.ConfigString._
import model.WustSchema._
import modules.db.Database
import play.core.StaticApplication
import renesca._
import renesca.parameter.implicits._
import java.io.File

object SeedInit extends Task {
  println("Seeding Database...")

  dbContext { implicit db =>
    setupDbConstraints(db)
    //TODO: allow uniqueness constraints in subclass (modeling)
    db.query("CREATE CONSTRAINT ON (n:TAG) ASSERT n.title IS UNIQUE");

    modifyDiscourse { implicit discourse =>
      mergeTag("Problem", isType = true)
      mergeTag("Goal", isType = true)
      mergeTag("Idea", isType = true)
      mergeTag("Pro", isType = true)
      mergeTag("Con", isType = true)

      mergeTag("HN-Story")
      mergeTag("HN-Ask")
      mergeTag("HN-Show")

      discourse.add(Connects.create(Post.create("hello"), Post.create("moon")))
   }
  }

  def mergeTag(title: String, description: Option[String] = None, isType: Boolean = false)(implicit discourse: Discourse): Unit = {
    discourse.add(Tag.merge(title = title, description = description, isType = isType, merge = Set("title", "isType")))
  }
}
