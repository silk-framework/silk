package controllers.workspaceApi.search

import controllers.workspaceApi.search.SearchApiModel.{Facet, FacetSetting, ItemType, KeywordFacetSetting}
import org.silkframework.config.{CustomTask, TaskSpec}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.workspace.{Project, ProjectTask}
import org.silkframework.workspace.activity.workflow.Workflow

import scala.collection.immutable.ListMap
import scala.collection.mutable
import scala.reflect.ClassTag

/** Combination of multiple item type facet collectors */
case class ItemTypeFacetCollectors[T <: TaskSpec](itemTypeFacetCollectors: Seq[ItemTypeFacetCollector[T]]) {
  val facetCollectors: Seq[FacetCollector[T]] = itemTypeFacetCollectors.flatMap(_.facetCollectors)

  /** Return aggregated facet results. */
  def result: Seq[FacetResult] = {
    facetCollectors.flatMap(_.result)
  }

  def filterAndCollect(projectTask: ProjectTask[_ <: TaskSpec],
                       facetSettings: Seq[FacetSetting])
                      (implicit classTag: ClassTag[T]): Boolean = {
    ItemTypeFacetCollector.filterAndCollect[T](itemTypeFacetCollectors, facetSettings, projectTask)
  }
}

/**
  * Collects facets and their values for the respective project task type.
  */
trait ItemTypeFacetCollector[T  <: TaskSpec] {
  /** The collectors for each facet */
  val facetCollectors: Seq[FacetCollector[T]]

  /** Return aggregated facet results. */
  def result: Seq[FacetResult] = {
    facetCollectors.flatMap(_.result)
  }
}

object ItemTypeFacetCollector {
  /** Returns true if the project task should be in the result set according to the facet setting.
    * It collects facet values after filtering.
    * Values are collected for each facet, when no other facet filters out a project task. */
  def filterAndCollect[T <: TaskSpec](itemFacetCollectors: Seq[ItemTypeFacetCollector[T]],
                                      facetSettings: Seq[FacetSetting],
                                      projectTask: ProjectTask[_ <: TaskSpec])
                                     (implicit classTag: ClassTag[T]): Boolean = {
    assert(itemFacetCollectors.nonEmpty, "Trying to collect facet values without facet collectors!")
    val facetCollectors = itemFacetCollectors.flatMap(_.facetCollectors)
    val facetCollectorMap: Map[String, FacetCollector[T]] = {
      facetCollectors.map(fc => (fc.appliesForFacet.id, fc)).toMap
    }
    val enabledFacets = facetSettings.map(_.facetId).toSet
    val matchingFacets = mutable.Set[String]()
    facetSettings foreach { facetSetting =>
      facetCollectorMap.get(facetSetting.facetId) match {
        case Some(facetCollector) =>
          val facetMatches = facetCollector.convertAndFilter(facetSetting, projectTask)
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
        facetCollector.convertAndCollect(projectTask)
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

  /** Conversion and check function */
  def convertProjectTask(projectTask: ProjectTask[_ <: TaskSpec])(implicit classTag: ClassTag[T]): ProjectTask[T] = {
    if (classTag.runtimeClass.isAssignableFrom(projectTask.data.getClass)) {
      projectTask.asInstanceOf[ProjectTask[T]]
    } else {
      throw new IllegalArgumentException(s"Task '${projectTask.taskLabel()}' has not correct type for facet collector ${this.getClass.getSimpleName}.")
    }
  }

  /** Same as filter, but takes a generic project task. */
  def convertAndFilter(facetSetting: FacetSetting, projectTask: ProjectTask[_ <: TaskSpec])(implicit classTag: ClassTag[T]): Boolean = {
    filter(facetSetting, convertProjectTask(projectTask))
  }

  /** Same as collect, but takes a generic project task. */
  def convertAndCollect(projectTask: ProjectTask[_ <: TaskSpec])(implicit classTag: ClassTag[T]): Unit = {
    collect(convertProjectTask(projectTask))
  }
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

/** Trait that can be used for keyword facets where the keyword ID is the same as the keyword label. */
trait NoLabelKeyboardFacetCollector[T <: TaskSpec] extends KeywordFacetCollector[T] {
  private val keywordNames = new mutable.ListMap[String, Int]()

  override def collect(projectTask: ProjectTask[T]): Unit = {
    extractKeywordIds(projectTask) foreach { keywordName =>
      keywordNames.put(keywordName, keywordNames.getOrElseUpdate(keywordName, 0) + 1)
    }
  }

  override def keywordStats: Seq[(String, String, Int)] = {
    keywordNames.toSeq map (rn => (rn._1, rn._1, rn._2))
  }
}

/** Collects values for all facets of all types. */
case class OverallFacetCollector() {
  // Item type specific facet collectors
  private val itemTypeFacetCollectors = ListMap[ItemType, ItemTypeFacetCollectors[_ <: TaskSpec]](
    ItemType.project -> ItemTypeFacetCollectors(Seq()), // This is never used, but still listed for completeness
    ItemType.dataset -> ItemTypeFacetCollectors(Seq(DatasetFacetCollector(), TaskSpecFacetCollector[GenericDatasetSpec]())),
    ItemType.transform -> ItemTypeFacetCollectors(Seq(TransformFacetCollector(), TaskSpecFacetCollector[TransformSpec]())),
    ItemType.linking -> ItemTypeFacetCollectors(Seq(TaskSpecFacetCollector[LinkSpec]())),
    ItemType.workflow -> ItemTypeFacetCollectors(Seq(WorkflowFacetCollector(), TaskSpecFacetCollector[Workflow]())),
    ItemType.task -> ItemTypeFacetCollectors(Seq(TaskFacetCollector(), TaskSpecFacetCollector[CustomTask]()))
  )
  // Generic item type collectors
  private val genericItemTypeFacetCollectors = ItemTypeFacetCollectors(Seq(TaskSpecFacetCollector[TaskSpec]()))

  def filterAndCollectByItemType(itemType: ItemType,
                                 projectTask: ProjectTask[_ <: TaskSpec],
                                 facetSettings: Seq[FacetSetting]): Boolean = {
    itemTypeFacetCollectors(itemType).filterAndCollect(projectTask, facetSettings)
  }

  def filterAndCollectAllItems(projectTask: ProjectTask[_ <: TaskSpec],
                               facetSettings: Seq[FacetSetting]): Boolean = {
    genericItemTypeFacetCollectors.filterAndCollect(projectTask, facetSettings)
  }

  def filterAndCollectProjects(project: Project,
                               facetSettings: Seq[FacetSetting]): Boolean = {
    // Since projects are not TaskSpecs, we have to do an unclean workaround. Only OK because we know what the TaskSpecFacetCollector will do and its tested.
    genericItemTypeFacetCollectors.filterAndCollect(new ProjectTask[TaskSpec](project.name, Workflow(Seq.empty, Seq.empty), project.config.metaData, null), facetSettings)
  }

  def results: Iterable[FacetResult] = {
    for(collector <- Seq(genericItemTypeFacetCollectors) ++ itemTypeFacetCollectors.values;
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