package org.silkframework.rule.execution

import org.silkframework.entity.Link
import org.silkframework.execution.ExecutionReport
import org.silkframework.rule.LinkageRule
import org.silkframework.util.DPair

/**
  * Set of links.
  */
case class Linking(rule: LinkageRule, links : Seq[Link] = Seq.empty, statistics: LinkingStatistics = LinkingStatistics()) extends ExecutionReport {

  lazy val summary: Seq[(String, String)] = {
    Seq(
      "number of source entities" -> statistics.entityCount.source.toString,
      "number of target entities" -> statistics.entityCount.target.toString,
      "number of links" -> links.size.toString
    )
  }

}

case class LinkingStatistics(entityCount: DPair[Int] = DPair.fill(0))
