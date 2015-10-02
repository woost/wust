package modules.db.helpers

import model.WustSchema._

object RequestHelper {
  def conflictingAddTags(cr: AddTags, existingAddTags: Seq[AddTags]) = {
    existingAddTags.filter { addTag =>
      cr != addTag && addTag.proposesTags.head == cr.proposesTags.head && addTag.proposesClassifys.toSet.subsetOf(cr.proposesClassifys.toSet)
    }
  }

  def conflictingRemoveTags(cr: RemoveTags, existingAddTags: Seq[RemoveTags]) = {
    existingAddTags.filter { remTag =>
      cr != remTag && remTag.proposesTags.head == cr.proposesTags.head && (cr.proposesClassifys.isEmpty || !remTag.proposesClassifys.isEmpty && remTag.proposesClassifys.toSet.subsetOf(cr.proposesClassifys.toSet))
    }
  }
}
