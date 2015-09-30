package moderation


// from paper:
// 2011 User-Rating based Ranking of Items from an Axiomatic Perspective
// Dirichlet Prior Smoothing: (up + u*p) / (down + up + u)
// p: probability for an upvote (needs to be calculated from all votes in the system)
// u: influence of our prior

import model.WustSchema._
import wust.ModerationBase

object Moderation extends ModerationBase {

  //TODO: is upvote / downvote ratio linear? Or can we expect for example sqrt(views)=upvotes ?
  // Check a HN/Reddit dump to find out
  val votes_p = 0.1 // this is also the default weight, if we have up=0 and down=0
  val votes_u = 10
  def postQuality(upVotes:Long, downVotes:Long):Double = (upVotes + votes_u*votes_p) / (downVotes + upVotes + votes_u)

  //TODO: rate limiting for low karma users
  val initialKarma:Long = 0

  def postVoteKarma:Long = 1

  def voteWeightFromScopes(scopes: Seq[Scope]):Long = voteWeight(karmaSum(scopes))
  def karmaSum(scopes: Seq[Scope]):Long = {
    scopes.map { tag =>
      val l:Long = tag.inRelationsAs(HasKarma).headOption.map(_.karma).getOrElse(0)
      l
    }.sum
  }

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
