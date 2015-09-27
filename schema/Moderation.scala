package moderation


// from paper:
// 2011 User-Rating based Ranking of Items from an Axiomatic Perspective
// Dirichlet Prior Smoothing: (up + u*p) / (down + up + u)
// p: probability for an upvote (needs to be calculated from all votes in the system)
// u: influence of our prior

import model.WustSchema._

object Moderation {
  // http://stackoverflow.com/questions/3305059/how-do-you-calculate-log-base-2-in-java-for-integers
  def log2(x:Long):Long = (Math.log(x)/Math.log(2)+1e-10).toLong
  def log(x:Long):Long = Math.log(x).round
  def sqrt(x:Long):Long = Math.sqrt(x).round
  //TODO: is upvote / downvote ratio linear? Or can we expect for example sqrt(views)=upvotes ?
  // Check a HN/Reddit dump to find out
  val votes_p = 0.1 // this is also the default weight, if we have up=0 and down=0
  val votes_u = 10

  //TODO: rate limiting for low karma users
  val initialKarma:Long = 0

  val authorKarmaBoost:Long = postChangeThreshold(viewCount = 1024) // author can change its own post until 1024 views

  def postVoteKarma:Long = 1

  def voteWeightFromScopes(scopes: Seq[Scope]):Long = voteWeight(karmaSum(scopes))
  //TODO: negative karmasum: reject changerequests automatically?
  private def voteWeight(votersKarma: Long):Long = if (votersKarma > 0) log2(votersKarma) max 1 else 1 // log weight from paper WikiTrust
  private def karmaSum(scopes: Seq[Scope]):Long = {
    scopes.map { tag =>
      val l:Long = tag.inRelationsAs(HasKarma).headOption.map(_.karma).getOrElse(0)
      l
    }.sum
  }

  def postChangeThreshold(viewCount: Long):Long = sqrt(viewCount) max 8 // One needs min 256 karma to do instant edits on new posts

  def rejectPostChangeThreshold(applyThreshold: Long):Long = (-applyThreshold / 2) min -1

  def postQuality(upVotes:Long, downVotes:Long):Double = (upVotes + votes_u*votes_p) / (downVotes + upVotes + votes_u)

  //TODO: user für votestream-votes belohnen? -> nein, votes sollen aus intrinsischer motivation kommen

  // reject threshold
  // When do change requests disappear?
  // - I don't want to see the request anymore, when I'm done thinking about it
  //   - After voting
  //   - It can reappear, when it is revised (like pull requests on github)
  //   - On merge conflicts (which need to be revised)
  // Reverting votes on change requests
  // - When do I want to revert a vote on a change request?
  //   - When I clicked the wrong button (undo)
  //   - When I see that another request is better and I want to apply that instead
  //   - When I rethink the request and change my mind
  // How long do change-actions stay in the vote-stream?
  //   - depending on how much more karma the mod had than the threshold of the post
  //   - If I voted, it hides for me
  //   - It doesn't appear for other when it was
  //     (depending on the quality of the post)
  //     - A good change:
  //     - A bad change:
  //     - Controversal?
  //       - Ability to comment/answer on changes?
}
