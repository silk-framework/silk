package org.silkframework.plugins.dataset.rdf.tasks

import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.rdf.RdfDataset
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{AutoCompletionResult, ParamValue, PluginContext, PluginParameterAutoCompletionProvider}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.workspace.WorkspaceReadTrait

/**
  * Auto-completion provider that only fetches SPARQL enabled datasets.
  */
case class SparqlEndpointDatasetAutoCompletionProvider() extends PluginParameterAutoCompletionProvider {
  override def autoComplete(searchQuery: String, dependOnParameterValues: Seq[ParamValue], workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Traversable[AutoCompletionResult] = {
    implicit val userContext: UserContext = context.user
    val projectId = context.projectId.getOrElse(throw new ValidationException("Project not provided"))
    val allResults = workspace.project(projectId).tasks[GenericDatasetSpec]
        .filter(datasetSpec => datasetSpec.data.plugin.isInstanceOf[RdfDataset])
        .map(datasetSpec => AutoCompletionResult(datasetSpec.id, datasetSpec.metaData.label))
    filterResults(searchQuery, allResults)
  }

  override def valueToLabel(value: String, dependOnParameterValues: Seq[ParamValue], workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Option[String] = {
    implicit val userContext: UserContext = context.user
    val projectId = context.projectId.getOrElse(throw new ValidationException("Project not provided"))
    workspace.project(projectId).taskOption[GenericDatasetSpec](value).flatMap(_.metaData.label)
  }
}
