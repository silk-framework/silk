package controllers.projectApi.requests

import org.silkframework.config.Task
import org.silkframework.rule.{RuleBlockPort, RuleBlockSpec}
import play.api.libs.json.{Format, Json}

/** Summary information about a reusable rule block that is sufficient for rule editors. */
case class RuleBlockTaskSummary(id: String,
                                label: String,
                                description: Option[String],
                                ports: Seq[RuleBlockPortSummary])

/** Summary information about a reusable rule block input port. */
case class RuleBlockPortSummary(id: String,
                                label: String,
                                description: String,
                                displayOrder: Int,
                                deprecated: Boolean)

object RuleBlockTaskSummary {
  def fromTask(task: Task[RuleBlockSpec]): RuleBlockTaskSummary = {
    RuleBlockTaskSummary(
      id = task.id.toString,
      label = task.fullLabel,
      description = task.metaData.description,
      ports = task.data.ports
        .sortBy(port => (port.displayOrder, port.id.toString))
        .map(RuleBlockPortSummary.fromRuleBlockPort)
    )
  }

  implicit val ruleBlockTaskSummaryFormat: Format[RuleBlockTaskSummary] = Json.format[RuleBlockTaskSummary]
}

object RuleBlockPortSummary {
  def fromRuleBlockPort(port: RuleBlockPort): RuleBlockPortSummary = {
    RuleBlockPortSummary(
      id = port.id.toString,
      label = port.label,
      description = port.description,
      displayOrder = port.displayOrder,
      deprecated = port.deprecated
    )
  }

  implicit val ruleBlockPortSummaryFormat: Format[RuleBlockPortSummary] = Json.format[RuleBlockPortSummary]
}
