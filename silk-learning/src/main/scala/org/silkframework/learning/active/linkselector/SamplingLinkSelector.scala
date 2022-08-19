package org.silkframework.learning.active.linkselector

import org.silkframework.learning.active.{ActiveLearningReferenceData, LinkCandidate}
import org.silkframework.util.SampleUtil

import scala.util.Random

case class SamplingLinkSelector(baseSelector: LinkSelector, linkSampleSize: Option[Int], ruleSampleSize: Option[Int]) extends LinkSelector {

  private implicit val random: Random = Random

  def apply(rules: Seq[WeightedLinkageRule], referenceData: ActiveLearningReferenceData)(implicit random: Random): Seq[LinkCandidate] = {
    val sampledLinks = linkSampleSize match {
      case Some(sampleSize) => SampleUtil.sample(referenceData.linkCandidates, sampleSize, None)
      case None => referenceData.linkCandidates
    }

    val sampledRules = ruleSampleSize match {
      case Some(sampleSize) => SampleUtil.sample(rules, sampleSize, None)
      case None => rules
    }

    baseSelector.apply(sampledRules, referenceData.copy(linkCandidates = sampledLinks))
  }

}
