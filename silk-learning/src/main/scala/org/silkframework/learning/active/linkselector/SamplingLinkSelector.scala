package org.silkframework.learning.active.linkselector

import org.silkframework.entity.Link
import org.silkframework.rule.evaluation.ReferenceEntities
import org.silkframework.util.SampleUtil

import scala.util.Random

case class SamplingLinkSelector(baseSelector: LinkSelector, linkSampleSize: Option[Int], ruleSampleSize: Option[Int]) extends LinkSelector {

  private implicit val random: Random = Random

  def apply(rules: Seq[WeightedLinkageRule], unlabeledLinks: Seq[Link], referenceEntities: ReferenceEntities)(implicit random: Random): Seq[Link] = {
    val sampledLinks = linkSampleSize match {
      case Some(sampleSize) => SampleUtil.sample(unlabeledLinks, sampleSize, None)
      case None => unlabeledLinks
    }

    val sampledRules = ruleSampleSize match {
      case Some(sampleSize) => SampleUtil.sample(rules, sampleSize, None)
      case None => rules
    }

    baseSelector.apply(sampledRules, sampledLinks, referenceEntities)
  }

}
