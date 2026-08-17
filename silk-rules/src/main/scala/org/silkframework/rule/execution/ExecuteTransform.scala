package org.silkframework.rule.execution

import org.silkframework.config.{Prefixes, Task, TaskSpec}
import org.silkframework.dataset.{DataSource, EntitySink}
import org.silkframework.execution.local.ErrorOutputWriter
import org.silkframework.rule.TransformSpec.RuleSchemataExecution
import org.silkframework.rule._
import org.silkframework.rule.execution.local.TransformedEntities
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.runtime.plugin.{PluginContext, TaskResolver}

import scala.util.control.Breaks._
import scala.util.control.NonFatal

/**
  * Executes a set of transformation rules.
  */
class ExecuteTransform(task: Task[TransformSpec],
                       inputTask: UserContext => Option[Task[_ <: TaskSpec]],
                       input: UserContext => DataSource,
                       output: UserContext => EntitySink,
                       errorOutput: UserContext => Option[EntitySink] = _ => None,
                       pluginContext: UserContext => PluginContext,
                       limit: Option[Int] = None) extends Activity[TransformReport] {

  private def transform = task.data

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
    val ruleSchemata = transform.ruleSchemataWithoutEmptyObjectRules
    val report = new TransformReportBuilder(task, context, outputTableCount = ruleSchemata.size)
    report.setExecutionContext(TransformReportExecutionContext(entitySink))
    implicit val pluginContextWithUser: PluginContext = pluginContext(userContext)
    val taskContext = TaskContext(inputTask(userContext).toSeq, pluginContextWithUser)

    // Clear outputs before writing
    context.status.updateMessage("Clearing output")
    entitySink.clear()
    errorEntitySink.foreach(_.clear())

    context.status.updateMessage("Retrieving entities")
    try {
      for ((ruleSchema, index) <- ruleSchemata.zipWithIndex) {
        transformEntities(dataSource, ruleSchema.execution(taskContext), entitySink, errorEntitySink, report, context)
        context.status.updateProgress((index + 1.0) / ruleSchemata.size)
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
      dataSource.retrieve(rule.inputSchema)
    } catch {
      case NonFatal(ex) =>
        throw new RuntimeException("Failed to retrieve input entities from data source.", ex)
    }
    val transformedEntities = new TransformedEntities(task, entityTable.entities, rule.transformRule.label(), rule.transformRuleExecution, rule.outputSchema,
      isRequestedSchema = false, abortIfErrorsOccur = task.data.abortIfErrorsOccur, report = reportBuilder).iterator
    var count = 0
    try {
      breakable {
        // No for loop, since CloseableIterator.foreach would close the iterator before the catch below runs
        while (transformedEntities.hasNext) {
          val entity = transformedEntities.next()
          entitySink.writeEntity(entity.uri, entity.values)
          if(entity.hasFailed) {
            errorEntitySink.foreach(_.writeEntity(entity.uri, entity.values :+ Seq(entity.failure.get.message.getOrElse("Unknown error"))))
          }
          count += 1
          if (cancelled || limit.exists(_ <= count)) {
            break()
          }
        }
      }
    } catch {
      case NonFatal(ex) =>
        // Fail the report (e.g. on sink errors) before the finally-close counts this table as completed.
        reportBuilder.executionFailed(ex.getMessage)
        throw ex
    } finally {
      // Completes this output table in the report and closes the input iterator, also on cancellation or limit.
      transformedEntities.close()
    }
    entitySink.closeTable()
    errorEntitySink.foreach(_.closeTable())

    context.value() = context.value().copy(globalErrors = context.value().globalErrors ++ entityTable.globalErrors)
  }
}
