package tasks

import model.WustSchema._
import renesca.parameter.implicits._
import org.unbescape.html.HtmlEscape.unescapeHtml

trait SeedTools {
  val maxTitleLength = 140

  def mergeClassification(title: String, description: Option[String] = None) = {
    Classification.merge(title = title, description = description, merge = Set("title"))
  }

  def mergeCategorization(title: String, description: Option[String] = None) = {
    Categorization.merge(title = title, description = description, merge = Set("title"))
  }

  def mergeStaticTag(title: String, description: Option[String] = None) = {
    StaticTag.merge(title = title, description = description, merge = Set("title"))
  }

  def mergeScope(title: String, description: Option[String] = None) = {
    Scope.merge(title = title, description = description, merge = Set("title"))
  }

  def tag(item: Taggable, tag: TagLike) = {
    Tags.create(tag, item)
  }

  def shorten(str: String, maxlength: Int = maxTitleLength) = {
    if(str.length <= maxlength)
      str
    else
      str.take(maxlength - 3) + "..."
  }

  def createPost(title: String, description: String, url: Option[String]) = {
    Post.create(
      title = shorten(unescapeHtml(title)),
      description = if(description.nonEmpty) Some(url.map(_ + "\n\n").getOrElse("") + unescapeHtml(description)) else url
    )
  }

  def createPost(rawContent: String) = {
    val content = unescapeHtml(rawContent)
    Post.create(
      title = shorten(content),
      description = if(content.length <= maxTitleLength) None else Some(content)
    )
  }
}

