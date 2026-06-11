package org.silkframework.dataset.rdf

import org.silkframework.config.Task
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.{DataSource, Dataset, DatasetAccess, DatasetSpec, EntitySink, LinkSink}
import org.silkframework.execution.ExecutorRegistry
import org.silkframework.execution.local.LocalExecution
import org.silkframework.runtime.activity.UserContext

/**
  * A [[DatasetAccess]] for an RDF dataset that additionally exposes a SPARQL endpoint.
  *
  * The endpoint returned here is bound to the execution that produced this access. This is the
  * relevant difference to [[RdfDataset.sparqlEndpoint]], which lives on the shared dataset plugin
  * and may be a volatile reference that a concurrently running execution sharing the same dataset
  * task has overwritten (e.g. the in-memory dataset in workflow-scoped mode).
  */
trait RdfDatasetAccess extends DatasetAccess {

  def sparqlEndpoint: SparqlEndpoint

}

object RdfDatasetAccess {

  /**
    * Resolves the RDF access of a dataset task for a specific execution.
    */
  def forExecution(task: Task[DatasetSpec[RdfDataset]], execution: LocalExecution): RdfDatasetAccess = {
    forExecutionOption(task, execution).getOrElse(task.data.plugin)
  }

  /**
    * Resolves the RDF access of an arbitrary dataset task for a specific execution, returning `None`
    * if the task is not an RDF dataset.
    */
  def forExecutionOption[DatasetType <: Dataset](task: Task[DatasetSpec[DatasetType]],
                                                 execution: LocalExecution): Option[RdfDatasetAccess] = {
    task.data.plugin match {
      case rdfDataset: RdfDataset =>
        Some(ExecutorRegistry.access(task, execution) match {
          case rdf: RdfDatasetAccess => rdf
          case _ => rdfDataset
        })
      case _ =>
        None
    }
  }
}

/**
  * Like [[org.silkframework.dataset.DatasetSpecAccess]], but for RDF datasets: in addition to the
  * DatasetSpec behaviour it exposes the execution-scoped SPARQL endpoint of the wrapped access.
  */
case class RdfDatasetSpecAccess(datasetSpec: GenericDatasetSpec, datasetAccess: RdfDatasetAccess) extends RdfDatasetAccess {

  override def sparqlEndpoint: SparqlEndpoint = datasetAccess.sparqlEndpoint

  override def linkSink(implicit userContext: UserContext): LinkSink = DatasetSpec.LinkSinkWrapper(datasetAccess.linkSink, datasetSpec)

  override def entitySink(implicit userContext: UserContext): EntitySink = DatasetSpec.EntitySinkWrapper(datasetAccess.entitySink, datasetSpec)

  override def source(implicit userContext: UserContext): DataSource = DatasetSpec.DataSourceWrapper(datasetAccess.source, datasetSpec)
}
