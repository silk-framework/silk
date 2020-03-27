package org.silkframework.plugins.dataset.rdf.tasks

import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.rdf.RdfDataset
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.AutoCompletionResult
import org.silkframework.workspace.WorkspacePluginParameterAutoCompletionProvider

/**
  * Auto-completion provider that only fetches SPARQL enabled datasets.
  */
case class SparqlEndpointDatasetAutoCompletionProvider() extends WorkspacePluginParameterAutoCompletionProvider {
  override protected def autoComplete(searchQuery: String, projectId: String, dependOnParameterValues: Seq[String])
                                     (implicit userContext: UserContext): Traversable[AutoCompletionResult] = {
    val allResults = project(projectId).tasks[GenericDatasetSpec]
        .filter(datasetSpec => datasetSpec.data.plugin.isInstanceOf[RdfDataset])
        .map(datasetSpec => AutoCompletionResult(datasetSpec.id, Some(datasetSpec.metaData.label)))
    filterResults(searchQuery, allResults)
  }

  override def valueToLabel(projectId: String, value: String, dependOnParameterValues: Seq[String])(implicit userContext: UserContext): Option[String] = {
    project(projectId).taskOption[GenericDatasetSpec](value).map(_.metaData.label)
  }
}
