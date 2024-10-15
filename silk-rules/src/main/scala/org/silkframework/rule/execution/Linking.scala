package org.silkframework.rule.execution

import org.silkframework.config.Task
import org.silkframework.entity.Link
import org.silkframework.execution.ExecutionReport
import org.silkframework.execution.report.{EntitySample, SampleEntities}
import org.silkframework.rule.{LinkSpec, LinkageRule}
import org.silkframework.util.DPair

import scala.collection.mutable

/**
  * Set of links.
  */
case class Linking(task: Task[LinkSpec],
                   rule: LinkageRule,
                   links : Seq[Link] = Seq.empty,
                   statistics: LinkingStatistics = LinkingStatistics(),
                   matcherWarnings: Seq[String] = Seq.empty,
                   isDone: Boolean = false,
                   override val sampleOutputEntities: Seq[SampleEntities] = Seq.empty) extends ExecutionReport {

  lazy val summary: Seq[(String, String)] = {
    Seq(
      "number of source entities" -> statistics.entityCount.source.toString,
      "number of target entities" -> statistics.entityCount.target.toString,
      "number of links" -> links.size.toString
    )
  }

  def warnings: Seq[String] = {
    val warnings = mutable.Buffer[String]()
    if(statistics.entityCount.source == 0) {
      warnings += "No source entities have been loaded."
    }
    if(statistics.entityCount.target == 0) {
      warnings += "No target entities have been loaded."
    }
    if(links.isEmpty) {
      warnings += "No links have been generated."
    }
    warnings ++= matcherWarnings
    warnings.toSeq
  }

  override def entityCount: Int = links.size

  /**
    * Short description of the operation (plural, past tense).
    */
  override def operationDesc: String = "links generated"

  /**
    * Returns a done version of this report.
    */
  def asDone(): ExecutionReport = copy(isDone = true)

  /** Updates the execution report with some sample entities. */
  override def withSampleOutputEntities(sampleEntities: SampleEntities): ExecutionReport = this.copy(sampleOutputEntities = Seq(sampleEntities))
}

case class LinkingStatistics(entityCount: DPair[Int] = DPair.fill(0))
