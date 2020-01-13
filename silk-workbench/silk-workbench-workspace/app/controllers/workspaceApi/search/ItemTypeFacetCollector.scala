package controllers.workspaceApi.search

import controllers.workspaceApi.search.SearchApiModel.{Facet, Facets, ItemType}
import org.silkframework.config.TaskSpec
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.workspace.ProjectTask

import scala.collection.immutable.ListMap
import scala.collection.mutable

/**
  * Collects facets and their values for the respective project task type.
  */
trait ItemTypeFacetCollector[T <: TaskSpec] {
  /** Add values of project task to collector. */
  def collect(task: ProjectTask[T]): Unit

  /** Return aggregated facet results. */
  def result: Seq[FacetResult]

  /** The collectors for each facet */
  def facetCollectors: Seq[FacetCollector[T]]

  /** Conversion and check function */
  def convertProjectTask(projectTask: ProjectTask[_ <: TaskSpec]): ProjectTask[T]

  /** Type agnostic collect method. */
  def collectProjectTask(projectTask: ProjectTask[_ <: TaskSpec]): Unit = {
    collect(convertProjectTask(projectTask))
  }
}

/**
  * Collects facet values for a specific facet for a specific item type.
  */
trait FacetCollector[T <: TaskSpec] {
  /** Collect facet values of a single facet. */
  def collect(datasetTask: ProjectTask[T]): Unit

  /** The facet results if there exists at least one value. */
  def facetValues: Option[FacetValues]

  /** The facet result. */
  def result: Option[FacetResult] = {
    facetValues map { values =>
      FacetResult(appliesForFacet.id, appliesForFacet.label, appliesForFacet.description, appliesForFacet.facetType.toString, values)
    }
  }

  /** The facet this collector applies for. */
  def appliesForFacet: Facet
}

/** Collects values for all facets of all types. */
case class OverallFacetCollector() {
  private val itemTypeFacetCollectors = ListMap[ItemType, Seq[ItemTypeFacetCollector[_]]](
    ItemType.project -> Seq(),
    ItemType.dataset -> Seq(DatasetFacetCollector()),
    ItemType.transform -> Seq(),
    ItemType.linking -> Seq(),
    ItemType.workflow -> Seq(),
    ItemType.task -> Seq()
  )

  def collect(itemType: ItemType, datasetTask: ProjectTask[_ <: TaskSpec]): Unit = {
    itemTypeFacetCollectors(itemType).foreach(_.collectProjectTask(datasetTask))
  }

  def results: Iterable[FacetResult] = {
    for((_, collectors) <-itemTypeFacetCollectors;
        collector <- collectors;
        result <- collector.result) yield {
      result
    }
  }
}

/** A single facet of the search results. */
case class FacetResult(id: String,
                       label: String,
                       description: String,
                       `type`: String,
                       values: FacetValues)

/** Values of a single facet. */
sealed trait FacetValues

/** Values of a keyword based facet. */
case class KeywordFacetValues(values: Seq[KeywordFacetValue]) extends FacetValues

/** A single value of a keyword facet. */
case class KeywordFacetValue(id: String,
                             label: String,
                             count: Option[Int])