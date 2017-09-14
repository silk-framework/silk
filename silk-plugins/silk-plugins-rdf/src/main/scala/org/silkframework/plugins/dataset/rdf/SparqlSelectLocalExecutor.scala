package org.silkframework.plugins.dataset.rdf

import org.silkframework.config.Task
import org.silkframework.dataset.rdf.{SparqlEndpointEntityTable, SparqlResults}
import org.silkframework.entity.{Entity, EntitySchema, Path}
import org.silkframework.execution.local.{EntityTable, GenericEntityTable, LocalExecution, LocalExecutor}
import org.silkframework.execution.{ExecutionReport, TaskException}
import org.silkframework.runtime.activity.ActivityContext

/**
  * Local executor for [[SparqlSelectCustomTask]].
  */
case class SparqlSelectLocalExecutor() extends LocalExecutor[SparqlSelectCustomTask] {
  override def execute(task: Task[SparqlSelectCustomTask],
                       inputs: Seq[EntityTable],
                       outputSchema: Option[EntitySchema],
                       execution: LocalExecution,
                       context: ActivityContext[ExecutionReport]): Option[EntityTable] = {
    val taskData = task.data

    inputs match {
      case Seq(sparql: SparqlEndpointEntityTable) =>
        val results = sparql.select(taskData.selectQuery, taskData.intLimit.getOrElse(Integer.MAX_VALUE))
        val vars: IndexedSeq[String] = getSparqlVars(taskData)
        val entities: Traversable[Entity] = createEntities(taskData, results, vars)
        Some(GenericEntityTable(entities, entitySchema = taskData.outputSchema, task))
      case _ =>
        throw TaskException("SPARQL select executor did not receive a SPARQL endpoint as requested!")
    }
  }

  private def getSparqlVars(taskData: SparqlSelectCustomTask) = {
    val vars = taskData.outputSchema.typedPaths map { v =>
      v.propertyUri match {
        case Some(prop) =>
          prop.uri
        case _ =>
          throw TaskException("Path in input schema of SPARQL select operator is not a simple forward property: " + v.path.serializeSimplified)
      }
    }
    vars
  }

  private def createEntities(taskData: SparqlSelectCustomTask,
                             results: SparqlResults,
                             vars: IndexedSeq[String]): Traversable[Entity] = {
    var count = 0
    val entities: Traversable[Entity] = results.bindings map { binding =>
      count += 1
      val values = vars map { v =>
        binding.get(v).toSeq.map(_.value)
      }
      new Entity(s"urn:entity:$count", values = values, desc = taskData.outputSchema)
    }
    entities
  }
}