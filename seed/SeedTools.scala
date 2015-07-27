package tasks

import model.WustSchema._
import renesca.parameter.implicits._

trait SeedTools {
  val maxTitleLength = 140

  def mergeTag(title: String, description: Option[String] = None, isType: Boolean = false) = {
    Tag.merge(title = title, description = description, isType = isType, merge = Set("title"))
  }

  def mergeScope(title: String, description: Option[String] = None) = {
    Scope.merge(title = title, description = description, merge = Set("title"))
  }

  def tag(item: Taggable, tag: Tag) = {
    Categorizes.create(tag, item)
  }

  def belongsTo(item: ScopeChild, scope: Scope) = {
    BelongsTo.create(item, scope)
  }

  def shorten(str: String, maxlength: Int = maxTitleLength) = {
    if(str.length <= maxlength)
      str
    else
      str.take(maxlength - 3) + "..."
  }

  def createPost(title: String, description: String) = {
    Post.create(title = shorten(title), description = if(description.nonEmpty) Some(description) else None)
  }

  def createPost(content: String) = {
    Post.create(title = shorten(content), description = if(content.length <= maxTitleLength) None else Some(content))
  }
}

