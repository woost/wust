package wust

import scala.scalajs.js.annotation.{JSExport, JSExportAll}

trait ModerationBase {
    // http://stackoverflow.com/questions/3305059/how-do-you-calculate-log-base-2-in-java-for-integers
    def log2(x:Long):Long = (Math.log(x)/Math.log(2)+1e-10).toLong
    def log(x:Long):Long = Math.log(x).round
    def sqrt(x:Long):Long = Math.sqrt(x).round

    val authorKarmaBoost:Long = postChangeThreshold(viewCount = 1024) // author can change its own post until 1024 views

    //TODO: negative karmasum: reject changerequests automatically?
    // equal to: if (votersKarma < 4) 1 else log2(votersKarma)
    def voteWeight(votersKarma: Long):Long = if (votersKarma > 0) log2(votersKarma) max 1 else 1 // log weight from paper WikiTrust

    def postChangeThreshold(viewCount: Long):Long = sqrt(viewCount) + 1 // +1, because viewcount will be at least 2 (author, editor). One needs min 4 karma to do instant edits on new posts

    def rejectPostChangeThreshold(applyThreshold: Long):Long = (-applyThreshold / 2) min -1

    def canApply(approvalSum: Long, applyThreshold: Long):Boolean = approvalSum >= applyThreshold
    def canReject(approvalSum: Long, rejectThreshold: Long):Boolean = approvalSum <= rejectThreshold
}

@JSExport
object Moderation extends ModerationBase {
  //TODO: conversion to js types are somehow broken here
  //return as double to js
  @JSExport("canApply") def canApplyJs(approvalSum: Double, applyThreshold: Double):Boolean = canApply(approvalSum.toLong, applyThreshold.toLong)
  @JSExport("canReject") def canRejectJs(approvalSum: Double, rejectThreshold: Double):Boolean = canReject(approvalSum.toLong, rejectThreshold.toLong)
  @JSExport("authorKarmaBoost") def authorKarmaBoostJs:Double = authorKarmaBoost
  @JSExport("voteWeight") def voteWeightJs(votersKarma: Double):Double = voteWeight(votersKarma.toLong)
}
