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
}
