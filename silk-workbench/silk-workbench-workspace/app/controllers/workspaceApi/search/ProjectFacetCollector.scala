package controllers.workspaceApi.search

import controllers.workspaceApi.search.SearchApiModel.{Facet, Facets}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.Project

import scala.collection.immutable.ListMap

case class ProjectFacetCollector() extends ItemTypeFacetCollector[Project] {
  override val facetCollectors: Seq[FacetCollector[Project]] = Seq(
    ProjectGroupFacetCollector()
  )
}

/** Collects values for the dataset type facet. */
case class ProjectGroupFacetCollector() extends KeywordFacetCollector[Project] {
  private var projectGroups = new ListMap[String, Int]()
  private val globalGroup = "_global"
  private val globalGroupLabel = "Global"

  /** Collect facet values of a single facet. */
  override def collect(project: Project)
                      (implicit user: UserContext): Unit = {
    val groups = project.accessControl.getGroups
    if(groups.nonEmpty) {
      groups.foreach(group => {
        val entry = (group, projectGroups.getOrElse(group, 0) + 1)
        projectGroups = projectGroups + entry
      })
    } else {
      val entry = (globalGroup, projectGroups.getOrElse(globalGroup, 0) + 1)
      projectGroups = projectGroups + entry
    }
  }

  override def appliesForFacet: Facet = Facets.groups

  override def extractKeywordIds(project: Project)
                                (implicit user: UserContext): Set[String] = {
    val groups = project.accessControl.getGroups
    if(groups.nonEmpty) {
      groups
    } else {
      Set(globalGroup)
    }
  }

  override def keywordStats: Seq[(String, String, Int)] = {
    projectGroups.toSeq.map(st => (st._1, if(st._1 == globalGroup) globalGroupLabel else st._1, st._2))
  }
}