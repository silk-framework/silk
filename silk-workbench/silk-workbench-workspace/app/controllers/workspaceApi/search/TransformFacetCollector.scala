package controllers.workspaceApi.search

import controllers.workspaceApi.search.SearchApiModel.Facets
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.ProjectTask

import scala.collection.mutable

/**
  * Facet collector for the Transformation task.
  */
case class TransformFacetCollector() extends ItemTypeFacetCollector[TransformSpec] {
  override val facetCollectors: Seq[FacetCollector[TransformSpec]] = Seq(
    TransformInputResourceFacetCollector()
  )
}

case class TransformInputResourceFacetCollector() extends KeywordFacetCollector[TransformSpec] {
  private val fileFacetCollector = DatasetFileFacetCollector()
  private val resourceNames = new mutable.ListMap[String, Int]()

  override def extractKeywordIds(projectTask: ProjectTask[TransformSpec]): Set[String] = {
    implicit val internalUser: UserContext = UserContext.INTERNAL_USER
    val inputTaskId = projectTask.data.selection.inputId
    projectTask.project.taskOption[GenericDatasetSpec](inputTaskId).toSet flatMap { projectDatasetSpec: ProjectTask[GenericDatasetSpec] =>
      fileFacetCollector.extractKeywordIds(projectDatasetSpec)
    }
  }

  override def keywordStats: Seq[(String, String, Int)] = {
    resourceNames.toSeq map (rn => (rn._1, rn._1, rn._2))
  }

  override def collect(transformTask: ProjectTask[TransformSpec]): Unit = {
    extractKeywordIds(transformTask).foreach { resourceName =>
      resourceNames.put(resourceName, resourceNames.getOrElseUpdate(resourceName, 0) + 1)
    }
  }

  override def appliesForFacet: SearchApiModel.Facet = Facets.transformInputResource
}