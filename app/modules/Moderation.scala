package modules.db


// from paper:
// 2011 User-Rating based Ranking of Items from an Axiomatic Perspective
// Dirichlet Prior Smoothing: (up + u*p) / (down + up + u)
// p: probability for an upvote (needs to be calculated from all votes in the system, I set this to 0.5)
// u: influence of our prior (I set this to 10)

object Moderation {
  def log(x:Long):Long = Math.log(x).round
  val votes_p = 0.5 // this is also the default weight, if we have up=0 and down=0
  val votes_u = 10


  val initialKarma = 0
  //TODO: reason why vote weight should be logarithmic
  def creatorsKarmaAfterPostVote(creatorsKarma:Long, votersKarma:Long, vote:Long) = creatorsKarma + log(votersKarma) * vote
  def postQuality(upVotes:Long, downVotes:Long) = (upVotes + votes_u*votes_p) / (downVotes + upVotes + votes_u)
  def postChangeThreshold(creatorsKarma:Long, votersKarma:Long, vote:Long, postUpVotes:Long, postDownVotes:Long) = {
    postQuality(postUpVotes, postDownVotes)
    ???
  }

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
