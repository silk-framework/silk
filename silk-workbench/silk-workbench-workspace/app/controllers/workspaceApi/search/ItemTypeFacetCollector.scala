package controllers.workspaceApi.search

import controllers.workspaceApi.search.SearchApiModel.{Facet, FacetSetting, ItemType, KeywordFacetSetting}
import org.silkframework.config.TaskSpec
import org.silkframework.workspace.ProjectTask

import scala.collection.immutable.ListMap
import scala.collection.mutable

/**
  * Collects facets and their values for the respective project task type.
  */
trait ItemTypeFacetCollector[T <: TaskSpec] {

  /** Return aggregated facet results. */
  def result: Seq[FacetResult]

  /** The collectors for each facet */
  def facetCollectors: Seq[FacetCollector[T]]

  /** Conversion and check function */
  def convertProjectTask(projectTask: ProjectTask[_ <: TaskSpec]): ProjectTask[T]

  private lazy val facetCollectorMap: Map[String, FacetCollector[T]] = {
    facetCollectors.map(fc => (fc.appliesForFacet.id, fc)).toMap
  }

  /** Returns true if the project task should be in the result set according to the facet setting.
    * It collects facet values after filtering.
    * Values are collected for each facet, when no other facet filters out a project task. */
  def filterAndCollect[U <: TaskSpec](facetSettings: Seq[FacetSetting],
                                      projectTask: ProjectTask[U]): Boolean = {
    val specificTask = convertProjectTask(projectTask)
    val enabledFacets = facetSettings.map(_.facetId).toSet
    val matchingFacets = mutable.Set[String]()
    facetSettings foreach { facetSetting =>
      facetCollectorMap.get(facetSetting.facetId) match {
        case Some(facetCollector) =>
          val facetMatches = facetCollector.filter(facetSetting, specificTask)
          if(facetMatches) {
            matchingFacets.add(facetSetting.facetId)
          }
        case None =>
          throw new IllegalArgumentException(s"Facet ID '${facetSetting.facetId}' is not available for facet collector '${this.getClass.getSimpleName}'.")
      }
    }
    val allMatch = facetSettings.size == matchingFacets.size // Only when all requested facet settings match, it's an overall match
    for(facetCollector <- facetCollectors) {
      if(allMatch || // count if the project task is in the result
          !matchingFacets.contains(facetCollector.appliesForFacet.id) // count if this specific does not match, but all other facets do
              && matchingFacets.size == facetSettings.size - 1
              && enabledFacets.contains(facetCollector.appliesForFacet.id)) {
        facetCollector.collect(specificTask)
      }
    }
    allMatch
  }
}

/**
  * Collects facet values for a specific facet for a specific item type.
  */
trait FacetCollector[T <: TaskSpec] {
  /** Collect facet values of a single facet. */
  def collect(projectTask: ProjectTask[T]): Unit

  /** The facet results if there exists at least one value. */
  def facetValues: Option[Seq[FacetValue]]

  /** The facet result. */
  def result: Option[FacetResult] = {
    facetValues map { values =>
      FacetResult(appliesForFacet.id, appliesForFacet.label, appliesForFacet.description, appliesForFacet.facetType.toString, values)
    }
  }

  /** Returns true if the task is filtered that is matched by the facet setting. */
  def filter(facetSetting: FacetSetting,
             projectTask: ProjectTask[T]): Boolean

  /** The facet this collector applies for. */
  def appliesForFacet: Facet
}

trait KeywordFacetCollector[T <: TaskSpec] extends FacetCollector[T] {
  /** Extracts the keywords for this facet from the given project task. */
  def extractKeywordIds(projectTask: ProjectTask[T]): Set[String]

  /** The collected keyword statistics: (id, label, count) */
  def keywordStats: Seq[(String, String, Int)]

  override def facetValues: Option[Seq[FacetValue]] = {
    val stats = keywordStats
    if(stats.nonEmpty) {
      val sortedKeywords = stats.sortBy(ks => (ks._3, ks._2))(Ordering.Tuple2(Ordering.Int.reverse, Ordering.String))
      Some(sortedKeywords map (st => KeywordFacetValue(st._1, st._2, Some(st._3))))
    } else {
      None
    }
  }

  override def filter(facetSetting: FacetSetting,
                      projectTask: ProjectTask[T]): Boolean = {
    facetSetting match {
      case KeywordFacetSetting(_, facetId, keywordIds) if facetId == appliesForFacet.id =>
        val keywords = extractKeywordIds(projectTask)
        // For now keyword facets are always disjunctive
        keywordIds.exists(keywords.contains)
      case _ =>
        false
    }
  }
}

/** Collects values for all facets of all types. */
case class OverallFacetCollector() {
  private val itemTypeFacetCollectors = ListMap[ItemType, Seq[ItemTypeFacetCollector[_]]](
    ItemType.project -> Seq(),
    ItemType.dataset -> Seq(DatasetFacetCollector()),
    ItemType.transform -> Seq(),
    ItemType.linking -> Seq(),
    ItemType.workflow -> Seq(),
    ItemType.task -> Seq(TaskFacetCollector())
  )

  def filterAndCollect(itemType: ItemType,
                       projectTask: ProjectTask[_ <: TaskSpec],
                       facetSettings: Seq[FacetSetting]): Boolean = {
    // TODO: Extend filterAndCollect beyond single item collectors
    itemTypeFacetCollectors(itemType).forall(_.filterAndCollect(facetSettings, projectTask))
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
                       values: Seq[FacetValue])

/** Single Value of a specific facet. */
sealed trait FacetValue

/** Single value of a keyword facet. */
case class KeywordFacetValue(id: String,
                             label: String,
                             count: Option[Int]) extends FacetValue