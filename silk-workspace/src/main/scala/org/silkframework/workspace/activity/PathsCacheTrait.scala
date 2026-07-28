package org.silkframework.workspace.activity

import org.silkframework.config.{DefaultConfig, FixedSchemaPort, Prefixes, Task, TaskSpec}
import org.silkframework.dataset.{DataSource, Dataset, DatasetSpec, SparqlRestrictionDataSource}
import org.silkframework.execution.ExecutorRegistry
import org.silkframework.entity.Restriction.CustomOperator
import org.silkframework.entity.paths.TypedPath
import org.silkframework.entity.rdf.{SparqlEntitySchema, SparqlRestriction}
import org.silkframework.rule.{DatasetSelection, TransformSpec}
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.util.{Identifier, Uri}
import org.silkframework.workspace.Project
import org.silkframework.workspace.activity.PathsCacheTrait.useFullRestrictions

/**
  * Defines methods useful to all paths caches.
  */
trait PathsCacheTrait {

  protected def maxDepth: Int = 1

  protected def maxPaths: Option[Int] = None

  protected def retrievePathsOfInput(inputTaskId: Identifier,
                                     dataSelection: Option[DatasetSelection],
                                     project: Project,
                                     context: ActivityContext[_])
                                    (implicit userContext: UserContext, prefixes: Prefixes): IndexedSeq[TypedPath] = {
    val inputTask = project.anyTask(inputTaskId)
    inputTask.data match {
      case _: DatasetSpec[Dataset] =>
        val datasetTask = inputTask.asInstanceOf[Task[DatasetSpec[Dataset]]]
        context.status.update("Retrieving frequent paths", 0.0)
        dataSelection match {
          case Some(selection) =>
            retrievePaths(ExecutorRegistry.access(datasetTask).source, selection)
          case None => IndexedSeq()
        }
      case transformSpec: TransformSpec =>
        // A transform input delivers the entities of the rule that generates the selected type, not those of its root rule
        transformSpec.outputSchemaForTargetType(dataSelection.map(_.typeUri).getOrElse(Uri(""))).typedPaths
      case task: TaskSpec =>
        task.outputPort match {
          case Some(FixedSchemaPort(schema)) =>
            schema.typedPaths
          case _ =>
            IndexedSeq()
        }
    }
  }

  private def retrievePaths(dataSource: DataSource, datasetSelection: DatasetSelection)
                           (implicit userContext: UserContext, prefixes: Prefixes): IndexedSeq[TypedPath] = {
    dataSource match {
      case DatasetSpec.DataSourceWrapper(ds: SparqlRestrictionDataSource, _) if useFullRestrictions || datasetSelection.typeUri.isEmpty =>
        val typeRestriction = SparqlRestriction.forType(datasetSelection.typeUri)
        val sparqlRestriction = datasetSelection.restriction.operator match {
          case Some(CustomOperator(sparqlExpression)) =>
            SparqlRestriction.fromSparql(SparqlEntitySchema.variable, sparqlExpression).merge(typeRestriction)
          case _ =>
            typeRestriction
        }
        ds.retrievePathsSparqlRestriction(sparqlRestriction, maxPaths)
      case source: DataSource =>
        // Retrieve most frequent paths
        source.retrievePaths(datasetSelection.typeUri, maxDepth, maxPaths)
    }
  }
}

object PathsCacheTrait {

  private val useFullRestrictions: Boolean = {
    val cfg = DefaultConfig.instance()
    val key = "caches.paths.useFullRestrictions"
    if(cfg.hasPath(key)) {
      cfg.getBoolean(key)
    } else {
      true
    }
  }
}
