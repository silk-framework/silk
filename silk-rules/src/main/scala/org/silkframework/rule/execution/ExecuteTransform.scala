package org.silkframework.rule.execution

import org.silkframework.dataset.{DataSource, EntitySink, TypedProperty}
import org.silkframework.entity._
import org.silkframework.execution.ExecutionReport
import org.silkframework.rule.TransformSpec.RuleSchemata
import org.silkframework.rule._
import org.silkframework.rule.execution.local.TransformedEntities
import org.silkframework.runtime.activity.{Activity, ActivityContext}

/**
  * Executes a set of transformation rules.
  */
class ExecuteTransform(input: DataSource, transform: TransformSpec, output: EntitySink) extends Activity[TransformReport] {

  require(transform.rules.count(_.target.isEmpty) <= 1, "Only one rule with empty target property (subject rule) allowed.")

  private val subjectRule = transform.rules.find(_.target.isEmpty)

  private val propertyRules = transform.rules.filter(_.target.isDefined)

  @volatile
  private var isCanceled: Boolean = false

  override val initialValue = Some(TransformReport())

  def run(context: ActivityContext[TransformReport]): Unit = {
    isCanceled = false

    // Clear outputs before writing
    output.clear()

    try {
      for ((ruleSchemata, index) <- transform.ruleSchemata.zipWithIndex) {
        transformEntities(ruleSchemata, context)
        context.status.updateProgress((index + 1.0) / transform.ruleSchemata.size)
      }
    } finally {
      output.close()
    }
  }

  private def transformEntities(rule: RuleSchemata, context: ActivityContext[TransformReport]): Unit = {
    output.openTable(rule.outputSchema.typeUri, rule.outputSchema.typedPaths.map(_.property.get))

    val entities = input.retrieve(rule.inputSchema)
    val transformedEntities = new TransformedEntities(entities, rule.transformRule.rules, rule.outputSchema, context.asInstanceOf[ActivityContext[ExecutionReport]])
    for (entity <- transformedEntities) {
      output.writeEntity(entity.uri, entity.values)
      if (isCanceled) {
        return
      }
    }

    output.closeTable()
  }

  override def cancelExecution(): Unit = {
    isCanceled = true
  }
}