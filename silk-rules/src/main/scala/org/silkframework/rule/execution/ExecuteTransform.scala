package org.silkframework.rule.execution

import org.silkframework.dataset.{DataSource, EntitySink}
import org.silkframework.execution.ExecutionReport
import org.silkframework.rule.TransformSpec.RuleSchemata
import org.silkframework.rule._
import org.silkframework.rule.execution.local.TransformedEntities
import org.silkframework.runtime.activity.{Activity, ActivityContext}
import scala.util.control.Breaks._

/**
  * Executes a set of transformation rules.
  */
class ExecuteTransform(input: => DataSource, transform: TransformSpec, output: => EntitySink, limit: Option[Int] = None) extends Activity[TransformReport] {

  require(transform.rules.count(_.target.isEmpty) <= 1, "Only one rule with empty target property (subject rule) allowed.")

  @volatile
  private var isCanceled: Boolean = false

  override val initialValue = Some(TransformReport())

  def run(context: ActivityContext[TransformReport]): Unit = {
    isCanceled = false
    // Get fresh data source and entity sink
    val dataSource = input
    val entitySink = output

    // Clear outputs before writing
    entitySink.clear()

    try {
      for ((ruleSchemata, index) <- transform.ruleSchemata.zipWithIndex) {
        transformEntities(dataSource, ruleSchemata, entitySink, context)
        context.status.updateProgress((index + 1.0) / transform.ruleSchemata.size)
      }
    } finally {
      entitySink.close()
    }
  }

  private def transformEntities(dataSource: DataSource,
                                rule: RuleSchemata,
                                entitySink: EntitySink,
                                context: ActivityContext[TransformReport]): Unit = {
    entitySink.openTable(rule.outputSchema.typeUri, rule.outputSchema.typedPaths.map(_.property.get))

    val entities = dataSource.retrieve(rule.inputSchema)
    val transformedEntities = new TransformedEntities(entities, rule.transformRule.rules, rule.outputSchema, context.asInstanceOf[ActivityContext[ExecutionReport]])
    var count = 0
    breakable {
      for (entity <- transformedEntities) {
        entitySink.writeEntity(entity.uri, entity.values)
        count += 1
        if (isCanceled || limit.exists(_ <= count)) {
          break
        }
      }
    }
    entitySink.closeTable()
  }

  override def cancelExecution(): Unit = {
    isCanceled = true
  }
}