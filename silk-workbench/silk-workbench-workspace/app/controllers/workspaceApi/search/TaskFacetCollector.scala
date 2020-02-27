package controllers.workspaceApi.search

import controllers.workspaceApi.search.SearchApiModel.{Facet, Facets}
import org.silkframework.config.{CustomTask, TaskSpec}
import org.silkframework.workspace.ProjectTask

import scala.collection.mutable

/**
  * Facet value collector of the 'Task'/CustomTask type.
  */
case class TaskFacetCollector() extends ItemTypeFacetCollector[CustomTask] {
  override val facetCollectors: Seq[FacetCollector[CustomTask]] = Seq(
    TaskTypeFacetCollector()
  )
}

/** Collects values for the task type facet. */
case class TaskTypeFacetCollector() extends KeywordFacetCollector[CustomTask] {
  private val taskTypes = new mutable.ListMap[String, Int]()
  private val taskTypeLabel = new mutable.ListMap[String, String]()

  /** Collect facet values of a single facet. */
  override def collect(customTask: ProjectTask[CustomTask]): Unit = {
    val pluginSpec = customTask.pluginSpec
    val id = pluginSpec.id
    val label = pluginSpec.label
    taskTypes.put(id, taskTypes.getOrElseUpdate(id, 0) + 1)
    taskTypeLabel.put(id, label)
  }

  override def appliesForFacet: Facet = Facets.taskType

  override def extractKeywordIds(customTask: ProjectTask[CustomTask]): Set[String] = {
    val pluginSpec = customTask.pluginSpec
    Set(pluginSpec.id)
  }

  override def keywordStats: Seq[(String, String, Int)] = {
    taskTypes.toSeq.map(st => (st._1, taskTypeLabel(st._1), st._2))
  }
}