package org.silkframework.plugins.dataset.rdf.executors

import org.silkframework.config.Task
import org.silkframework.dataset.DataSource
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.local._
import org.silkframework.execution.{ExecutionReport, ExecutionReportUpdater, ExecutorOutput}
import org.silkframework.plugins.dataset.rdf.tasks.SparqlUpdateCustomTask
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
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
                      (implicit userContext: UserContext): Option[LocalEntities] = {
    val updateTask = task.data
    val expectedSchema = updateTask.expectedInputSchema
    val reportUpdater = new SparqlUpdateExecutionReportUpdater(task.taskLabel(), context)

    // Generate SPARQL Update queries for input entities
    def executeOnInput[U](batchEmitter: BatchSparqlUpdateEmitter[U], expectedProperties: IndexedSeq[String], input: LocalEntities): Unit = {
      val inputProperties = getInputProperties(input.entitySchema).distinct
      checkInputSchema(expectedProperties, inputProperties.toSet)
      for (entity <- input.entities;
           values = expectedSchema.typedPaths.map(tp => entity.valueOfPath(tp.toUntypedPath)) if values.forall(_.nonEmpty)) {
        val it = CrossProductIterator(values, expectedProperties)
        while (it.hasNext) {
          batchEmitter.update(updateTask.generate(it.next()))
          reportUpdater.increaseEntityCounter()
          reportUpdater.update()
        }
      }
    }

    val traversable = new Traversable[Entity] {
      override def foreach[U](f: Entity => U): Unit = {
        val batchEmitter = BatchSparqlUpdateEmitter(f, updateTask.batchSize)
        val expectedProperties = getInputProperties(expectedSchema)
        if (expectedProperties.isEmpty) {
          // Static template needs only to be executed once
          batchEmitter.update(updateTask.generate(Map.empty))
          reportUpdater.increaseEntityCounter()
        } else {
          for (input <- inputs) {
            executeOnInput(batchEmitter, expectedProperties, input)
          }
        }
        reportUpdater.update(force = true, addEndTime = true)
        batchEmitter.close()
      }
    }
    Some(new SparqlUpdateEntityTable(traversable, task))
  }

  // Check that expected schema is subset of input schema
  private def checkInputSchema[U](expectedProperties: Seq[String], inputProperties: Set[String]): Unit = {
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

case class SparqlUpdateExecutionReportUpdater(taskLabel: String,
                                              context: ActivityContext[ExecutionReport]) extends ExecutionReportUpdater {
  override def entityLabelSingle: String = "Query"

  override def entityLabelPlural: String = "Queries"

  override def entityProcessVerb: String = "generated"
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