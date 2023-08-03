package org.silkframework.plugins.dataset.rdf.executors

import org.silkframework.config.{Prefixes, Task, TaskSpec}
import org.silkframework.dataset.DataSource
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.local._
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
    val reportUpdater = SparqlUpdateExecutionReportUpdater(task, context)

    // Generate SPARQL Update queries for input entities
    def executeOnInput[U](batchEmitter: BatchSparqlUpdateEmitter[U], expectedProperties: IndexedSeq[String], input: LocalEntities): Unit = {
      val inputProperties = getInputProperties(input.entitySchema).distinct
      val taskProperties = createTaskProperties(Some(input.task), output.task, pluginContext.resources)
      checkInputSchema(expectedProperties, inputProperties.toSet)
      for (entity <- input.entities;
           values = expectedSchema.typedPaths.map(tp => entity.valueOfPath(tp.toUntypedPath)) if values.forall(_.nonEmpty)) {
        val it = CrossProductIterator(values, expectedProperties)
        while (it.hasNext) {
          batchEmitter.update(updateTask.generate(it.next(), taskProperties))
          reportUpdater.increaseEntityCounter()
        }
      }
    }

    val traversable = new TraversableIterator[Entity] {
      override def foreach[U](f: Entity => U): Unit = {
        val batchEmitter = BatchSparqlUpdateEmitter(f, updateTask.batchSize)
        val expectedProperties = getInputProperties(expectedSchema)
        if (updateTask.isStaticTemplate) {
          // Static template needs to be executed exactly once
          executeTemplate(batchEmitter, reportUpdater, updateTask, pluginContext.resources, outputTask = output.task)
        } else {
          for (input <- inputs) {
            if(expectedProperties.isEmpty) {
              // Template without input path placeholders should be executed once per input, e.g. it uses input task properties
              executeTemplate(batchEmitter, reportUpdater, updateTask, pluginContext.resources, inputTask = Some(input.task), outputTask = output.task)
            } else {
              executeOnInput(batchEmitter, expectedProperties, input)
            }
          }
        }
        reportUpdater.executionDone()
        batchEmitter.close()
      }
    }
    Some(new SparqlUpdateEntityTable(traversable, task))
  }

  private def executeTemplate[U](batchEmitter: BatchSparqlUpdateEmitter[U],
                                 reportUpdater: SparqlUpdateExecutionReportUpdater,
                                 updateTask: SparqlUpdateCustomTask,
                                 projectResources: ResourceManager,
                                 inputTask: Option[Task[_ <: TaskSpec]] = None,
                                 outputTask: Option[Task[_ <: TaskSpec]] = None): Unit = {
    val taskProperties = createTaskProperties(inputTask = inputTask, outputTask = outputTask, projectResources)
    batchEmitter.update(updateTask.generate(Map.empty, taskProperties))
    reportUpdater.increaseEntityCounter()
  }

  private def createTaskProperties(inputTask: Option[Task[_ <: TaskSpec]],
                                   outputTask: Option[Task[_ <: TaskSpec]],
                                   projectResources: ResourceManager): TaskProperties = {
    // It's obligatory to have empty prefixes here, since we do not want to have prefixed URIs for URI parameters
    implicit val pluginContext: PluginContext = PluginContext(prefixes= Prefixes.empty, resources = projectResources)
    val inputProperties = inputTask.toSeq.flatMap(_.properties).toMap
    val outputProperties = outputTask.toSeq.flatMap(_.properties).toMap
    TaskProperties(inputProperties, outputProperties)
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

case class CrossProductIterator(values: IndexedSeq[Seq[String]],
                                properties: IndexedSeq[String]) extends Iterator[Map[String, String]] {
  assert(values.nonEmpty)
  private val sizes = values.map(_.size).toArray
  // Holds the current index combination
  private val indexes = new Array[Int](values.size)
  private val firstNonEmptyIdx = sizes.zipWithIndex.filter(_._1 > 0).map(_._2).headOption.getOrElse(-1) // -1 if all are empty
  private val lastIndex = values.size - 1
  private var first: Boolean = true // This makes sure that at least one assignment is always generated

  override def hasNext: Boolean = first || firstNonEmptyIdx > -1 && (indexes(firstNonEmptyIdx) < sizes(firstNonEmptyIdx))

  override def next(): Map[String, String] = {
    if(!hasNext) {
      throw new IllegalStateException("Iterator is fully consumed and has no more values!")
    }
    val nextAssignment = indexes.zipWithIndex.collect {
      case (valueIdx, propertyIndex) if sizes(propertyIndex) > 0 => properties(propertyIndex) -> values(propertyIndex)(valueIdx)
    }.toMap
    setNextIndexCombinations()
    first = false
    nextAssignment
  }

  private def setNextIndexCombinations(): Unit = {
    var idx = lastIndex
    while(idx > -1) {
      indexes(idx) += 1
      if(indexes(idx) >= sizes(idx) && idx != firstNonEmptyIdx) { // Do not reset the first index, because of hasNext check
        indexes(idx) = 0
        idx -= 1
      } else if(idx > 0) {
        for(i <- (idx + 1) to lastIndex) { // null all index values after this index
          indexes(i) = 0
        }
        idx = -1
      } else {
        idx = -1
      }
    }
  }
}

case class BatchSparqlUpdateEmitter[U](f: Entity => U, batchSize: Int) {
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
    f(Entity(DataSource.URN_NID_PREFIX + entityCount, values = IndexedSeq(Seq(sparqlUpdateQueries.toString)), schema = SparqlUpdateEntitySchema.schema))
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