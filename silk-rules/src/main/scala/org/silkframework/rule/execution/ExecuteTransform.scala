package org.silkframework.rule.execution

import org.silkframework.config.Prefixes
import org.silkframework.dataset.{DataSource, EntitySink}
import org.silkframework.execution.{AbortExecutionException, ExecutionReport}
import org.silkframework.rule.TransformSpec.RuleSchemata
import org.silkframework.rule._
import org.silkframework.rule.execution.local.TransformedEntities
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}

import scala.util.control.Breaks._

/**
  * Executes a set of transformation rules.
  */
class ExecuteTransform(taskLabel: String,
                       input: UserContext => DataSource,
                       transform: TransformSpec,
                       output: UserContext => EntitySink,
                       limit: Option[Int] = None)(implicit prefixes: Prefixes) extends Activity[TransformReport] {

  require(transform.rules.count(_.target.isEmpty) <= 1, "Only one rule with empty target property (subject rule) allowed.")

  override val initialValue = Some(TransformReport(taskLabel))

  def run(context: ActivityContext[TransformReport])
         (implicit userContext: UserContext): Unit = {
    cancelled = false
    // Get fresh data source and entity sink
    val dataSource = input(userContext)
    val entitySink = output(userContext)

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
                                context: ActivityContext[TransformReport])
                               (implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    entitySink.openTable(rule.outputSchema.typeUri, rule.outputSchema.typedPaths.map(_.property.get))

    val entities = dataSource.retrieve(rule.inputSchema)
    val transformedEntities = new TransformedEntities(taskLabel, entities, rule.transformRule.rules, rule.outputSchema,
      isRequestedSchema = false, context = context)
    var count = 0
    breakable {
      for (entity <- transformedEntities) {
        entitySink.writeEntity(entity.uri, entity.values)
        count += 1
        if (cancelled || limit.exists(_ <= count)) {
          break
        }
      }
    }
    entitySink.closeTable()

    context.value() = context.value().copy(globalErrors = context.value().globalErrors ++ entityTable.globalErrors)
  }
}