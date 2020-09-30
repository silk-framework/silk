package controllers.workspaceApi.search

import controllers.workspaceApi.search.SearchApiModel.Facets
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.ProjectTask

/**
  * Facet collector for the Transformation task.
  */
case class TransformFacetCollector() extends ItemTypeFacetCollector[TransformSpec] {
  override val facetCollectors: Seq[FacetCollector[TransformSpec]] = Seq(
    TransformInputResourceFacetCollector()
  )
}

/** Collect file resources from input datasets. */
case class TransformInputResourceFacetCollector() extends NoLabelKeyboardFacetCollector[TransformSpec] {
  private val fileFacetCollector = DatasetFileFacetCollector()

  override def extractKeywordIds(projectTask: ProjectTask[TransformSpec]): Set[String] = {
    implicit val internalUser: UserContext = UserContext.INTERNAL_USER
    val inputTaskId = projectTask.data.selection.inputId
    projectTask.project.taskOption[GenericDatasetSpec](inputTaskId).toSet flatMap { projectDatasetSpec: ProjectTask[GenericDatasetSpec] =>
      fileFacetCollector.extractKeywordIds(projectDatasetSpec)
    }
  }

  override def appliesForFacet: SearchApiModel.Facet = Facets.transformInputResource
}