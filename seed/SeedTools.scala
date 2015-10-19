package tasks

import model.WustSchema._
import renesca.parameter.implicits._
import org.unbescape.html.HtmlEscape.unescapeHtml
import wust.Shared.tagTitleColor

trait SeedTools {
  val maxTitleLength = 140

  def mergeClassification(title: String, description: Option[String] = None, color:Option[Long] = None, symbol: String = "", precedence: Long = 0) = {
    Classification.merge(title = title, description = description, color = color.getOrElse(tagTitleColor(title)), symbol = symbol, precedence = precedence, merge = Set("title"), onMatch = Set("description", "color", "symbol", "precedence"))
  }

  def matchClassification(title: String) = {
    Classification.matches(title = Some(title), matches = Set("title"))
  }

  def mergeScope(title: String, description: Option[String] = None, color:Option[Long] = None) = {
    Scope.merge(title = title, color = color.getOrElse(tagTitleColor(title)), description = description, merge = Set("title"))
  }

  def tag(item: Post, tag: Scope) = {
    Tags.create(tag, item)
  }

  def classify(item: Reference, tag: Classification) = {
    Classifies.create(tag, item)
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

  def createPost(title: String, description: Option[String], timestamp: Long) = {
    Post.create(
      title = shorten(unescapeHtml(title)),
      description = description,
      timestamp = timestamp
    )
  }

  def createPost(rawContent: String, timestamp: Option[Long] = None) = {
    val content = unescapeHtml(rawContent)
    val title = shorten(content)
    val description = if(content.length <= maxTitleLength) None else Some(content)
    timestamp.map(t => Post.create(title = title, description = description, timestamp = t)).getOrElse(Post.create(title = title, description = description))
  }
}

