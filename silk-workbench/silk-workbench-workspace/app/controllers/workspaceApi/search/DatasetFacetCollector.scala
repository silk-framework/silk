package controllers.workspaceApi.search

import controllers.workspaceApi.search.SearchApiModel.{Facet, Facets}
import org.silkframework.config.TaskSpec
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.WritableResourceDataset
import org.silkframework.workspace.ProjectTask

import scala.collection.mutable

/** Collects facet data of dataset tasks. */
case class DatasetFacetCollector() extends ItemTypeFacetCollector[GenericDatasetSpec] {
  override val facetCollectors: Seq[FacetCollector[GenericDatasetSpec]] = Seq(
    DatasetTypeFacetCollector(),
    DatasetFileFacetCollector()
  )

  /** Collect facet values of a single dataset */
  override def collect(datasetTask: ProjectTask[GenericDatasetSpec]): Unit = {
    for(facetCollector <- facetCollectors) {
      facetCollector.collect(datasetTask)
    }
  }

  /** Results of all facets of the dataset type */
  override def result: Seq[FacetResult] = {
    facetCollectors.flatMap(_.result)
  }

  override def convertProjectTask(projectTask: ProjectTask[_ <: TaskSpec]): ProjectTask[GenericDatasetSpec] = {
    if(projectTask.data.isInstanceOf[GenericDatasetSpec]) {
      projectTask.asInstanceOf[ProjectTask[GenericDatasetSpec]]
    } else {
      throw new IllegalArgumentException(s"Task '${projectTask.taskLabel()}' if not of type Dataset.")
    }
  }
}

/** Collects values for the dataset type facet. */
case class DatasetTypeFacetCollector() extends FacetCollector[GenericDatasetSpec] {
  private val datasetTypes = new mutable.ListMap[String, Int]()
  private val datasetTypeLabel = new mutable.ListMap[String, String]()

  /** Collect facet values of a single facet. */
  override def collect(datasetTask: ProjectTask[GenericDatasetSpec]): Unit = {
    val pluginSpec = datasetTask.plugin.pluginSpec
    val id = pluginSpec.id
    val label = pluginSpec.label
    datasetTypes.put(id, datasetTypes.getOrElseUpdate(id, 0) + 1)
    datasetTypeLabel.put(id, label)
  }

  override def appliesForFacet: Facet = Facets.datasetType

  override def facetValues: Option[FacetValues] = {
    if(datasetTypes.nonEmpty) {
      val sortedTypes = datasetTypes.toSeq.sortWith(_._2 > _._2)
      Some(KeywordFacetValues(sortedTypes map (st => KeywordFacetValue(st._1, datasetTypeLabel(st._1), Some(st._2)))))
    } else {
      None
    }
  }
}

/** File resources used by the datasets. */
case class DatasetFileFacetCollector() extends FacetCollector[GenericDatasetSpec] {
  private val resourceNames = new mutable.ListMap[String, Int]()

  override def collect(datasetTask: ProjectTask[GenericDatasetSpec]): Unit = {
    val data = datasetTask.data.plugin
    if(data.isInstanceOf[WritableResourceDataset]) {
      val resourceName = data.asInstanceOf[WritableResourceDataset].file.name
      resourceNames.put(resourceName, resourceNames.getOrElseUpdate(resourceName, 0) + 1)
    }
  }

  override def facetValues: Option[FacetValues] = {
    if(resourceNames.nonEmpty) {
      val sortedResourceNames = resourceNames.toSeq.sortWith(_._2 > _._2)
      Some(KeywordFacetValues(sortedResourceNames map (rn => KeywordFacetValue(rn._1, rn._1, Some(rn._2)))))
    } else {
      None
    }
  }

  override def appliesForFacet: Facet = Facets.fileResource
}