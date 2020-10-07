package org.silkframework.rule.execution

import org.silkframework.config.Prefixes
import org.silkframework.dataset.{DataSource, EntitySink, TypedProperty}
import org.silkframework.entity.ValueType
import org.silkframework.entity.paths.TypedPath
import org.silkframework.execution.{AbortExecutionException, ExecutionReport}
import org.silkframework.rule.TransformSpec.RuleSchemata
import org.silkframework.rule._
import org.silkframework.rule.execution.local.TransformedEntities
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.util.Identifier

import scala.util.control.Breaks._

/**
  * Executes a set of transformation rules.
  */
class ExecuteTransform(taskId: Identifier,
                       taskLabel: String,
                       input: UserContext => DataSource,
                       transform: TransformSpec,
                       output: UserContext => EntitySink,
                       errorOutput: UserContext => Option[EntitySink] = _ => None,
                       limit: Option[Int] = None)(implicit prefixes: Prefixes) extends Activity[TransformReport] {

  require(transform.rules.count(_.target.isEmpty) <= 1, "Only one rule with empty target property (subject rule) allowed.")

  override val initialValue = Some(TransformReport(taskLabel))

  def run(context: ActivityContext[TransformReport])
         (implicit userContext: UserContext): Unit = {
    cancelled = false
    // Get fresh data source and entity sink
    val dataSource = input(userContext)
    val entitySink = output(userContext)
    val errorEntitySink = errorOutput(userContext)

    // Clear outputs before writing
    entitySink.clear()
    errorEntitySink.foreach(_.clear())

    try {
      for ((ruleSchemata, index) <- transform.ruleSchemata.zipWithIndex) {
        transformEntities(dataSource, ruleSchemata, entitySink, errorEntitySink, context)
        context.status.updateProgress((index + 1.0) / transform.ruleSchemata.size)
      }
    } finally {
      entitySink.close()
      errorEntitySink.foreach(_.close())
    }
  }

  private def transformEntities(dataSource: DataSource,
                                rule: RuleSchemata,
                                entitySink: EntitySink,
                                errorEntitySink: Option[EntitySink],
                                context: ActivityContext[TransformReport])
                               (implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    entitySink.openTable(rule.outputSchema.typeUri, rule.outputSchema.typedPaths.map(_.property.get))
    errorEntitySink.foreach(_.openTable(rule.outputSchema.typeUri, rule.outputSchema.typedPaths.map(_.property.get) :+ TypedProperty("error", ValueType.STRING, false)))

    val entityTable = dataSource.retrieve(rule.inputSchema)
    val transformedEntities = new TransformedEntities(taskId, taskLabel, entityTable.entities, rule.transformRule.rules, rule.outputSchema,
      isRequestedSchema = false, context = context)
    var count = 0
    breakable {
      for (entity <- transformedEntities) {
        if(!entity.hasFailed) {
          entitySink.writeEntity(entity.uri, entity.values)
        } else {
          errorEntitySink.foreach(_.writeEntity(entity.uri, entity.values :+ Seq(entity.failure.get.message.getOrElse("Unknown error"))))
        }
        count += 1
        if (cancelled || limit.exists(_ <= count)) {
          break
        }
      }
    }
    entitySink.closeTable()
    errorEntitySink.foreach(_.closeTable())

    context.value() = context.value().copy(globalErrors = context.value().globalErrors ++ entityTable.globalErrors)
  }
}