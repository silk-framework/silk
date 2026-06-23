package org.silkframework.rule.execution

import org.silkframework.config.{Prefixes, Task, TaskSpec}
import org.silkframework.dataset.{DataSource, EntitySink}
import org.silkframework.execution.local.ErrorOutputWriter
import org.silkframework.rule.TransformSpec.RuleSchemataExecution
import org.silkframework.rule._
import org.silkframework.rule.execution.local.TransformedEntities
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.runtime.plugin.PluginContext

import scala.util.control.Breaks._
import scala.util.control.NonFatal

/**
  * Executes a set of transformation rules.
  */
class ExecuteTransform(task: Task[TransformSpec],
                       inputTask: UserContext => Task[_ <: TaskSpec],
                       input: UserContext => DataSource,
                       output: UserContext => EntitySink,
                       errorOutput: UserContext => Option[EntitySink] = _ => None,
                       pluginContext: UserContext => PluginContext) extends Activity[TransformReport] {

  private def transform = task.data

  /** Optional limit on the number of input entities to transform, configured on the transform task. */
  private def limit: Option[Int] = transform.limit

  require(transform.rules.count(_.target.isEmpty) <= 1, "Only one rule with empty target property (subject rule) allowed.")

  override val initialValue = Some(TransformReport(task))

  def run(context: ActivityContext[TransformReport])
         (implicit userContext: UserContext): Unit = {
    cancelled = false
    // Reset transform report
    context.value() = TransformReport(task, context = Some(TransformReportExecutionContext(input(userContext).underlyingTask)))

    try {
      execute(context)
    } catch {
      case NonFatal(ex) =>
        context.value.updateWith(_.copy(error = Some(ex.getMessage)))
        throw ex
    }
  }

  private def execute(context: ActivityContext[TransformReport])
                     (implicit userContext: UserContext): Unit = {
    // Get fresh data source and entity sink
    val dataSource = input(userContext)
    val entitySink = output(userContext)
    val errorEntitySink = errorOutput(userContext)
    val report = new TransformReportBuilder(task, context)
    report.setExecutionContext(TransformReportExecutionContext(entitySink))
    implicit val pluginContextWithUser: PluginContext = pluginContext(userContext)
    val taskContext = TaskContext(Seq(inputTask(userContext)), pluginContextWithUser)

    // Clear outputs before writing
    context.status.updateMessage("Clearing output")
    entitySink.clear()
    errorEntitySink.foreach(_.clear())

    context.status.updateMessage("Retrieving entities")
    try {
      for ((ruleSchemata, index) <- transform.ruleSchemataWithoutEmptyObjectRules.zipWithIndex) {
        transformEntities(dataSource, ruleSchemata.execution(taskContext), entitySink, errorEntitySink, report, context)
        context.status.updateProgress((index + 1.0) / transform.ruleSchemataWithoutEmptyObjectRules.size)
      }
    } finally {
      entitySink.close()
      errorEntitySink.foreach(_.close())
    }
  }

  private def transformEntities(dataSource: DataSource,
                                rule: RuleSchemataExecution,
                                entitySink: EntitySink,
                                errorEntitySink: Option[EntitySink],
                                reportBuilder: TransformReportBuilder,
                                context: ActivityContext[TransformReport])
                               (implicit pluginContext: PluginContext): Unit = {
    implicit val prefixes: Prefixes = pluginContext.prefixes
    implicit val user: UserContext = pluginContext.user

    val singleEntity = rule.transformRule.target.exists(_.isAttribute)
    entitySink.openTable(rule.outputSchema.typeUri, rule.outputSchema.typedPaths.map(_.property.get), singleEntity)
    errorEntitySink.foreach(_.openTable(rule.outputSchema.typeUri, rule.outputSchema.typedPaths.map(_.property.get) :+ ErrorOutputWriter.errorProperty, singleEntity))

    val entityTable = try {
      // Push the limit down to the data source (best-effort; not every source honors it).
      dataSource.retrieve(rule.inputSchema, limit)
    } catch {
      case NonFatal(ex) =>
        throw new RuntimeException("Failed to retrieve input entities from data source.", ex)
    }
    // Enforce the limit client-side as well, since the push-down is best-effort.
    val inputEntities = limit.map(entityTable.entities.take).getOrElse(entityTable.entities)
    val transformedEntities = new TransformedEntities(task, inputEntities, rule.transformRule.label(), rule.transformRuleExecution, rule.outputSchema,
      isRequestedSchema = false, abortIfErrorsOccur = task.data.abortIfErrorsOccur, report = reportBuilder).iterator
    breakable {
      for (entity <- transformedEntities) {
        entitySink.writeEntity(entity.uri, entity.values)
        if(entity.hasFailed) {
          errorEntitySink.foreach(_.writeEntity(entity.uri, entity.values :+ Seq(entity.failure.get.message.getOrElse("Unknown error"))))
        }
        if (cancelled) {
          break()
        }
      }
    }
    entitySink.closeTable()
    errorEntitySink.foreach(_.closeTable())

    context.value() = context.value().copy(globalErrors = context.value().globalErrors ++ entityTable.globalErrors)
  }
}
