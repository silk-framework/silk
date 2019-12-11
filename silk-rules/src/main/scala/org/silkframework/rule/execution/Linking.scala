package org.silkframework.rule.execution

import org.silkframework.entity.Link
import org.silkframework.execution.ExecutionReport
import org.silkframework.rule.LinkageRule
import org.silkframework.util.DPair

import scala.collection.mutable

/**
  * Set of links.
  */
case class Linking(label: String, rule: LinkageRule, links : Seq[Link] = Seq.empty, statistics: LinkingStatistics = LinkingStatistics()) extends ExecutionReport {

  lazy val summary: Seq[(String, String)] = {
    Seq(
      "number of source entities" -> statistics.entityCount.source.toString,
      "number of target entities" -> statistics.entityCount.target.toString,
      "number of links" -> links.size.toString
    )
  }

  def warnings: Seq[String] = {
    var warnings = mutable.Buffer[String]()
    if(statistics.entityCount.source == 0) {
      warnings += "No source entities have been loaded."
    }
    if(statistics.entityCount.target == 0) {
      warnings += "No target entities have been loaded."
    }
    if(links.isEmpty) {
      warnings += "No links have been generated."
    }
    warnings
  }

}

case class LinkingStatistics(entityCount: DPair[Int] = DPair.fill(0))
