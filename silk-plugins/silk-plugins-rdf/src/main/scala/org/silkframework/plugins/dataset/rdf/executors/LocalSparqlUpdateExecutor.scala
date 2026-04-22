package org.silkframework.plugins.dataset.rdf.executors

import org.silkframework.config.{Prefixes, Task, TaskSpec}
import org.silkframework.dataset.DataSource
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.local._
import org.silkframework.execution.report.{EntitySample, SampleEntitiesSchema}
import org.silkframework.execution.typed.SparqlUpdateEntitySchema
import org.silkframework.execution.{ExecutionReport, ExecutionReportUpdater, ExecutorOutput}
import org.silkframework.plugins.dataset.rdf.tasks.SparqlUpdateCustomTask
import org.silkframework.plugins.dataset.rdf.tasks.templating.TaskProperties
import org.silkframework.runtime.activity.ActivityContext
import org.silkframework.runtime.iterator.TraversableIterator
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.validation.ValidationException

/**
  * Local executor for [[SparqlUpdateCustomTask]].
  */
case class LocalSparqlUpdateExecutor() extends LocalExecutor[SparqlUpdateCustomTask] {
  override def execute(task: Task[SparqlUpdateCustomTask],
                       inputs: Seq[LocalEntities],
                       output: ExecutorOutput,
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport])
                      (implicit pluginContext: PluginContext): Option[LocalEntities] = {
    val updateTask = task.data
    val expectedSchema = updateTask.expectedInputSchema

    val templateVariables = pluginContext.templateVariables.all.variables

    // Generate SPARQL Update queries for input entities
    def executeOnInput[U](batchEmitter: BatchSparqlUpdateEmitter[U], expectedProperties: IndexedSeq[String], input: LocalEntities): Unit = {
      val inputProperties = getInputProperties(input.entitySchema).distinct
      val taskProperties = TaskProperties.create(Some(input.task), output.task, pluginContext)
      checkInputSchema(expectedProperties, inputProperties.toSet)
      for (entity <- input.entities;
           values = expectedSchema.typedPaths.map(tp => entity.valueOfPath(tp.toUntypedPath)) if values.forall(_.nonEmpty)) {
        val projected = Entity(entity.uri, values, expectedSchema)
        for (query <- updateTask.compiledTemplate.generate(Some(projected), taskProperties, templateVariables)) {
          batchEmitter.update(query)
        }
      }
    }

    val traversable = new TraversableIterator[String] {
      override def foreach[U](f: String => U): Unit = {
        val reportUpdater = SparqlUpdateExecutionReportUpdater(task, context)
        val batchEmitter = BatchSparqlUpdateEmitter(f, updateTask.batchSize, reportUpdater)
        val expectedProperties = getInputProperties(expectedSchema)
        reportUpdater.startNewOutputSamples(SampleEntitiesSchema("", "", IndexedSeq("Sparql Update query")))
        if (updateTask.isStaticTemplate) {
          // Static template needs to be executed exactly once
          executeTemplate(batchEmitter, updateTask, outputTask = output.task)
        } else {
          for (input <- inputs) {
            if(expectedProperties.isEmpty) {
              // Template without input path placeholders should be executed once per input, e.g. it uses input task properties
              executeTemplate(batchEmitter, updateTask, inputTask = Some(input.task), outputTask = output.task)
            } else {
              executeOnInput(batchEmitter, expectedProperties, input)
            }
          }
        }
        reportUpdater.executionDone()
        batchEmitter.close()
      }
    }
    Some(SparqlUpdateEntitySchema.create(traversable, task))
  }

  private def executeTemplate[U](batchEmitter: BatchSparqlUpdateEmitter[U],
                                 updateTask: SparqlUpdateCustomTask,
                                 inputTask: Option[Task[_ <: TaskSpec]] = None,
                                 outputTask: Option[Task[_ <: TaskSpec]] = None)
                                (implicit pluginContext: PluginContext): Unit = {
    val taskProperties = TaskProperties.create(inputTask = inputTask, outputTask = outputTask, pluginContext = pluginContext)
    val templateVariables = pluginContext.templateVariables.all.variables
    updateTask.compiledTemplate.generate(None, taskProperties, templateVariables).foreach(batchEmitter.update)
  }

  // Check that expected schema is subset of input schema
  private def checkInputSchema(expectedProperties: Seq[String], inputProperties: Set[String]): Unit = {
    if (expectedProperties.exists(p => !inputProperties.contains(p))) {
      val missingProperties = expectedProperties.filterNot(inputProperties.contains)
      throw new ValidationException("SPARQL Update executor: The input schema does not match the expected schema. Missing properties: " +
          missingProperties.mkString(", "))
    }
  }

  private def getInputProperties(entitySchema: EntitySchema): IndexedSeq[String] = {
    entitySchema.typedPaths.flatMap(_.property).map(_.propertyUri)
  }
}

case class SparqlUpdateExecutionReportUpdater(task: Task[TaskSpec],
                                              context: ActivityContext[ExecutionReport]) extends ExecutionReportUpdater {

  override def operationLabel: Option[String] = Some("generate queries")

  override def entityLabelSingle: String = "Query"

  override def entityLabelPlural: String = "Queries"

  override def entityProcessVerb: String = "generated"

  override def minEntitiesBetweenUpdates: Int = 1
}

case class BatchSparqlUpdateEmitter[U](f: String => U, batchSize: Int, reportUpdater: SparqlUpdateExecutionReportUpdater) {
  private var sparqlUpdateQueries = new StringBuffer()
  private var queryCount = 0

  def update(query: String): Unit = {
    if(queryCount > 0) {
      sparqlUpdateQueries.append("\n")
    }
    queryCount += 1

    sparqlUpdateQueries.append(query)
    if(queryCount >= batchSize) {
      emitEntity()
    }
  }

  private var entityCount = 0
  private def emitEntity(): Unit = {
    f(sparqlUpdateQueries.toString)
    reportUpdater.addSampleEntity(EntitySample(sparqlUpdateQueries.toString))
    reportUpdater.increaseEntityCounter()
    sparqlUpdateQueries = new StringBuffer()
    queryCount = 0
    entityCount += 1
  }

  def close(): Unit = {
    if(queryCount > 0) {
      emitEntity()
    }
  }
}