package org.silkframework.plugins.dataset.json

import org.silkframework.config.{FixedSchemaPort, PlainTask, Prefixes, Task, TaskSpec}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.entity.{Entity, EntitySchema, MultiEntitySchema}
import org.silkframework.entity.paths.{ForwardOperator, UntypedPath}
import org.silkframework.execution._
import org.silkframework.execution.local._
import org.silkframework.runtime.activity.{ActivityContext, ActivityMonitor}
import org.silkframework.runtime.iterator.{CloseableIterator, RepeatedIterator, RewindableEntityIterator}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.util.Uri


/**
  * Only considers the first input and checks for the property path defined in the [[JsonParserTask]] specification.
  * It will only read a property value of the first entity, following entities are ignored.
  */
case class LocalJsonParserTaskExecutor() extends LocalExecutor[JsonParserTask] {

  override def execute(task: Task[JsonParserTask],
                       inputs: Seq[LocalEntities],
                       output: ExecutorOutput,
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport] = new ActivityMonitor(getClass.getSimpleName))
                      (implicit pluginContext: PluginContext): Option[LocalEntities] = {
    if (inputs.size != 1) throw TaskException("JsonParserTask takes exactly one input!")
    val entityTable = inputs.head
    val entities = RewindableEntityIterator.load(entityTable.entities, entityTable.entitySchema)

    val pathIndex = task.data.parsedInputPath match {
      case Some(path) => entityTable.entitySchema.indexOfPath(path)
      case None => 0 // Take the value of the first path
    }

    output.requestedSchema
      .orElse(output.task.flatMap(_ => inferSchema(task, entities.newIterator(), pathIndex)))
      .map(executeWithSchema(task, _, entities, output, execution, context, pathIndex))
  }

  /** Runs the parse once a schema is known, dispatching on single vs. multi-entity schema. */
  private def executeWithSchema(task: Task[JsonParserTask],
                                requestedSchema: EntitySchema,
                                entities: RewindableEntityIterator,
                                output: ExecutorOutput,
                                execution: LocalExecution,
                                context: ActivityContext[ExecutionReport],
                                pathIndex: Int)
                               (implicit pluginContext: PluginContext): LocalEntities = {
    if (!entities.hasNext) throw TaskException("No input entity for 'JSON Parser Operator' found! There must be at least one entity in the input.")
    requestedSchema match {
      case mt: MultiEntitySchema =>
        val rootEntities = entityIterator(requestedSchema, entities, task, output, execution, context, pathIndex)
        val subEntities = mt.subSchemata.map { subSchema =>
          GenericEntityTable(entityIterator(subSchema, entities, task, output, execution, context, pathIndex, createNewIterator = true), subSchema, task)
        }
        MultiEntityTable(rootEntities, requestedSchema, task, subEntities)
      case _ =>
        GenericEntityTable(entityIterator(requestedSchema, entities, task, output, execution, context, pathIndex), requestedSchema, task)
    }
  }

  /** Infers an entity schema from the JSON content of the first input entity when no schema is prescribed downstream. */
  private def inferSchema(task: Task[JsonParserTask],
                          iterator: CloseableIterator[Entity],
                          pathIndex: Int): Option[EntitySchema] = {
    iterator.nextOption().flatMap { entity =>
      entity.values.lift(pathIndex).flatMap(_.headOption).map { jsonString =>
        val jsonSource = JsonSourceInMemory.fromString(
          taskId = task.id,
          str = jsonString,
          basePath = task.data.basePath,
          uriPattern = "",
          navigateIntoArrays = task.data.navigateIntoArrays
        )
        val typedPaths = jsonSource.collectPaths(limit = Int.MaxValue)
          .filter(_.nonEmpty)
          .map(segments => UntypedPath(segments.map(seg => ForwardOperator(Uri(seg)))).asStringTypedPath)
          .toIndexedSeq
        EntitySchema("", typedPaths)
      }
    }
  }

  /** Parses entities for one schema and wraps the result in a reporting iterator. */
  private def entityIterator(schema: EntitySchema,
                            entities: RewindableEntityIterator,
                            task: Task[JsonParserTask],
                            output: ExecutorOutput,
                            execution: LocalExecution,
                            context: ActivityContext[ExecutionReport],
                            pathIndex: Int,
                            createNewIterator: Boolean = false)
                           (implicit pluginContext: PluginContext): CloseableIterator[Entity] = {
    val entityParser = new EntityParser(task, ExecutorOutput(output.task, Some(FixedSchemaPort(schema))), execution, pathIndex)
    val entityIterator = if (createNewIterator) entities.newIterator() else entities
    implicit val reportUpdater: ExecutionReportUpdater = JsonParserReportUpdater(task, context)
    implicit val prefixes: Prefixes = Prefixes.empty
    ReportingIterator(new RepeatedIterator(() => entityIterator.nextOption().map(entityParser)).thenClose(entityIterator))
  }

  /**
    * Parses individual entities.
    */
  private class EntityParser(task: Task[JsonParserTask], output: ExecutorOutput, execution: LocalExecution, pathIndex: Int)
                            (implicit pluginContext: PluginContext) extends ((Entity) => CloseableIterator[Entity]) {

    private val spec = task.data

    /**
      * Takes a single entity and returns the parsed output entities.
      */
    def apply(entity: Entity): CloseableIterator[Entity] = {
      val values = entity.values
      if (values.size <= pathIndex) {
        throw TaskException("Unexpected error: No input value for path with index " + pathIndex + " found for 'JSON Parser Operator'!")
      } else {
        values(pathIndex).headOption match {
          case Some(jsonInputString) =>
            val resource = InMemoryResourceManager().get("temp")
            resource.writeBytes(jsonInputString.getBytes)
            val dataset = JsonDataset(resource, basePath = spec.basePath, uriPattern = entity.uri.toString + spec.uriSuffixPattern, streaming = false, navigateIntoArrays = spec.navigateIntoArrays)
            ExecutorRegistry.execute(PlainTask(task.id, DatasetSpec(dataset, readOnly = true)), Seq.empty, output, execution) match {
              case Some(result) => result.entities
              case None => CloseableIterator.empty
            }
          case None =>
            throw TaskException("No value found for input path!")
        }
      }
    }
  }

}

case class JsonParserReportUpdater(task: Task[TaskSpec], context: ActivityContext[ExecutionReport]) extends ExecutionReportUpdater {

  override def entityProcessVerb: String = "parsed"
}
