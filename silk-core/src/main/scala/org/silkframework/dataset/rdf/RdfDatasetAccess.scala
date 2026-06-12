package org.silkframework.dataset.rdf

import org.silkframework.config.Task
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.{DataSource, Dataset, DatasetAccess, DatasetSpec, DatasetSpecAccess, EntitySink, LinkSink, SafeModeDataSource, SafeModeSink}
import org.silkframework.execution.{ExecutionType, ExecutorRegistry}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.validation.ValidationException

/**
  * A [[DatasetAccess]] for an RDF dataset that additionally exposes a SPARQL endpoint.
  *
  * The endpoint returned here is bound to the execution that produced this access. This is the
  * relevant difference to accessing a SPARQL endpoint directly off the shared dataset plugin, which
  * may be a volatile reference that a concurrently running execution sharing the same dataset task
  * has overwritten (e.g. the in-memory dataset in workflow-scoped mode).
  */
trait RdfDatasetAccess extends DatasetAccess {

  def sparqlEndpoint: SparqlEndpoint

}

object RdfDatasetAccess {

  /**
    * Resolves the RDF access of a dataset task for a specific execution.
    */
  def forExecution[DatasetType <: Dataset](task: Task[DatasetSpec[DatasetType]], execution: ExecutionType): RdfDatasetAccess = {
    forExecutionOption(task, execution).getOrElse(noRdfAccess(task))
  }

  /**
    * Resolves the RDF access of a dataset task for the configured (default) execution.
    */
  def forExecution[DatasetType <: Dataset](task: Task[DatasetSpec[DatasetType]]): RdfDatasetAccess = {
    forExecutionOption(task).getOrElse(noRdfAccess(task))
  }

  /**
    * Resolves the RDF access of an arbitrary dataset task for a specific execution, returning `None`
    * if the dataset's executor does not provide RDF access.
    */
  def forExecutionOption[DatasetType <: Dataset](task: Task[DatasetSpec[DatasetType]],
                                                 execution: ExecutionType): Option[RdfDatasetAccess] = {
    rdfAccessOf(ExecutorRegistry.access(task, execution))
  }

  /**
    * Resolves the RDF access of an arbitrary dataset task for the configured (default) execution,
    * returning `None` if the dataset's executor does not provide RDF access.
    */
  def forExecutionOption[DatasetType <: Dataset](task: Task[DatasetSpec[DatasetType]]): Option[RdfDatasetAccess] = {
    rdfAccessOf(ExecutorRegistry.access(task))
  }

  /** Returns the access as an [[RdfDatasetAccess]] if the executor produced RDF access, else `None`. */
  private def rdfAccessOf(access: DatasetAccess): Option[RdfDatasetAccess] = {
    access match {
      case rdf: RdfDatasetAccess => Some(rdf)
      case _ => None
    }
  }

  private def noRdfAccess[DatasetType <: Dataset](task: Task[DatasetSpec[DatasetType]]): Nothing = {
    throw new ValidationException(s"Dataset task '${task.id}' of type ${task.data.plugin.pluginSpec.label} " +
      s"does not provide RDF data access.")
  }
}

/**
  * Like [[org.silkframework.dataset.DatasetSpecAccess]], but for RDF datasets: in addition to the
  * DatasetSpec behaviour it exposes the execution-scoped SPARQL endpoint of the wrapped access.
  */
case class RdfDatasetSpecAccess(datasetSpec: GenericDatasetSpec, datasetAccess: RdfDatasetAccess) extends RdfDatasetAccess {

  override def sparqlEndpoint: SparqlEndpoint = datasetAccess.sparqlEndpoint

  override def source(implicit userContext: UserContext): DataSource = {
    DatasetSpecAccess.safeAccess(DatasetSpec.DataSourceWrapper(datasetAccess.source, datasetSpec), SafeModeDataSource, datasetSpec)
  }

  override def entitySink(implicit userContext: UserContext): EntitySink = {
    DatasetSpecAccess.safeAccess(DatasetSpec.EntitySinkWrapper(datasetAccess.entitySink, datasetSpec), SafeModeSink, datasetSpec)
  }

  override def linkSink(implicit userContext: UserContext): LinkSink = {
    DatasetSpec.checkDatasetAllowsWriteAccess(None, datasetSpec.readOnly)
    DatasetSpecAccess.safeAccess(DatasetSpec.LinkSinkWrapper(datasetAccess.linkSink, datasetSpec), SafeModeSink, datasetSpec)
  }
}
