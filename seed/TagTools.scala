package tasks

import model.WustSchema._
import renesca.parameter.implicits._

trait TagTools {
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
}

