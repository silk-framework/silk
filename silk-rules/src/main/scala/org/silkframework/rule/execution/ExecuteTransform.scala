package org.silkframework.rule.execution

import org.silkframework.config.{Prefixes, Task, TaskSpec}
import org.silkframework.dataset.{DataSource, EntitySink}
import org.silkframework.execution.local.ErrorOutputWriter
import org.silkframework.rule.TransformSpec.RuleSchemata
import org.silkframework.rule._
import org.silkframework.rule.execution.local.TransformedEntities
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.EmptyResourceManager
import org.silkframework.runtime.templating.{TemplateVariables, TemplateVariablesReader}
import org.silkframework.workspace.ProjectTrait

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
                       limit: Option[Int] = None)(implicit prefixes: Prefixes, templateVariables: TemplateVariablesReader) extends Activity[TransformReport] {

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
    val report = new TransformReportBuilder(task, context)
    report.setExecutionContext(TransformReportExecutionContext(entitySink))
    val pluginContext: PluginContext = PluginContext(prefixes, EmptyResourceManager(), userContext, templateVariables = templateVariables)
    val taskContext = TaskContext(Seq(inputTask(userContext)), pluginContext)

    // Clear outputs before writing
    context.status.updateMessage("Clearing output")
    entitySink.clear()
    errorEntitySink.foreach(_.clear())

    context.status.updateMessage("Retrieving entities")
    try {
      for ((ruleSchemata, index) <- transform.ruleSchemataWithoutEmptyObjectRules.zipWithIndex) {
        transformEntities(dataSource, ruleSchemata.withContext(taskContext), entitySink, errorEntitySink, report, context)
        context.status.updateProgress((index + 1.0) / transform.ruleSchemataWithoutEmptyObjectRules.size)
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
                                reportBuilder: TransformReportBuilder,
                                context: ActivityContext[TransformReport])
                               (implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    val singleEntity = rule.transformRule.target.exists(_.isAttribute)
    entitySink.openTable(rule.outputSchema.typeUri, rule.outputSchema.typedPaths.map(_.property.get), singleEntity)
    errorEntitySink.foreach(_.openTable(rule.outputSchema.typeUri, rule.outputSchema.typedPaths.map(_.property.get) :+ ErrorOutputWriter.errorProperty, singleEntity))

    val entityTable = try {
      dataSource.retrieve(rule.inputSchema)
    } catch {
      case NonFatal(ex) =>
        throw new RuntimeException("Failed to retrieve input entities from data source.", ex)
    }
    val transformedEntities = new TransformedEntities(task, entityTable.entities, rule.transformRule.label(), rule.transformRule, rule.outputSchema,
      isRequestedSchema = false, abortIfErrorsOccur = task.data.abortIfErrorsOccur, report = reportBuilder).iterator
    var count = 0
    breakable {
      for (entity <- transformedEntities) {
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
    entitySink.closeTable()
    errorEntitySink.foreach(_.closeTable())

    context.value() = context.value().copy(globalErrors = context.value().globalErrors ++ entityTable.globalErrors)
  }
}
