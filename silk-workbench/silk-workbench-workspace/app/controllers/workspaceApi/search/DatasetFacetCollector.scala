package controllers.workspaceApi.search

import controllers.workspaceApi.search.SearchApiModel.{Facet, Facets}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.ResourceBasedDataset
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.ProjectTask

import scala.collection.mutable

/** Collects facet data of dataset tasks. */
case class DatasetFacetCollector() extends ItemTypeFacetCollector[ProjectTask[GenericDatasetSpec]] {
  override val facetCollectors: Seq[FacetCollector[ProjectTask[GenericDatasetSpec]]] = Seq(
    DatasetTypeFacetCollector(),
    DatasetFileFacetCollector(),
    DatasetReadOnlyFacetCollector()
  )
}

/** Collects values for the dataset type facet. */
case class DatasetTypeFacetCollector() extends KeywordFacetCollector[ProjectTask[GenericDatasetSpec]] {
  private val datasetTypes = new mutable.ListMap[String, Int]()
  private val datasetTypeLabel = new mutable.ListMap[String, String]()

  /** Collect facet values of a single facet. */
  override def collect(datasetTask: ProjectTask[GenericDatasetSpec])
                      (implicit user: UserContext): Unit = {
    val pluginSpec = datasetTask.plugin.pluginSpec
    val id = pluginSpec.id
    val label = pluginSpec.label
    datasetTypes.put(id, datasetTypes.getOrElseUpdate(id, 0) + 1)
    datasetTypeLabel.put(id, label)
  }

  override def appliesForFacet: Facet = Facets.datasetType

  override def extractKeywordIds(datasetTask: ProjectTask[GenericDatasetSpec])
                                (implicit user: UserContext): Set[String] = {
    val pluginSpec = datasetTask.plugin.pluginSpec
    Set(pluginSpec.id)
  }

  override def keywordStats: Seq[(String, String, Int)] = {
    datasetTypes.toSeq.map(st => (st._1, datasetTypeLabel(st._1), st._2))
  }
}

/** File resources used by the datasets. */
case class DatasetFileFacetCollector() extends NoLabelKeywordFacetCollector[ProjectTask[GenericDatasetSpec]] {
  override def appliesForFacet: Facet = Facets.fileResource

  override def extractKeywordIds(datasetTask: ProjectTask[GenericDatasetSpec])
                                (implicit user: UserContext): Set[String] = {
    val data = datasetTask.data.plugin
    data match {
      case dataset: ResourceBasedDataset =>
        Set(dataset.file.name)
      case _ =>
        Set()
    }
  }
}

/** "Filters datasets based on their access permissions. */
case class DatasetReadOnlyFacetCollector() extends NoLabelKeywordFacetCollector[ProjectTask[GenericDatasetSpec]] {
  override def appliesForFacet: Facet = Facets.readOnly

  override def extractKeywordIds(datasetTask: ProjectTask[GenericDatasetSpec])
                                (implicit user: UserContext): Set[String] = {
    if(datasetTask.data.readOnly) {
      Set("Read-only")
    } else {
      Set("Read-write")
    }
  }
}