package controllers.workspaceApi.search.activity

import controllers.workspaceApi.search.SearchApiModel.Facets
import controllers.workspaceApi.search._
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.activity.WorkspaceActivity

/**
  * Collects facet data of activities.
  */
case class ActivityFacetCollector() extends ItemTypeFacetCollector[WorkspaceActivity[_]] {
  override val facetCollectors: Seq[FacetCollector[WorkspaceActivity[_]]] = Seq(
    ActivityStatusCollector(),
    ActivityTypeCollector(),
    ActivityStartedByCollector()
  )
}

case class ActivityStatusCollector() extends NoLabelKeywordFacetCollector[WorkspaceActivity[_]] {

  override def appliesForFacet: SearchApiModel.Facet = Facets.activityStatus

  override def extractKeywordIds(activity: WorkspaceActivity[_])
                                (implicit user: UserContext): Set[String] = {
    Set(activity.status().concreteStatus)
  }
}

case class ActivityTypeCollector() extends IdAndLabelKeywordFacetCollector[WorkspaceActivity[_]] {

  override def appliesForFacet: SearchApiModel.Facet = Facets.activityType

  override protected def extractIdAndLabel(projectTask: WorkspaceActivity[_])
                                          (implicit user: UserContext): Set[(String, String)] = {
    if(projectTask.isCacheActivity) {
      Set(("cache", "Cache activity"))
    } else {
      Set(("non-cache", "Non-Cache activity"))
    }
  }
}

case class ActivityStartedByCollector() extends IdAndLabelKeywordFacetCollector[WorkspaceActivity[_]] {

  override def appliesForFacet: SearchApiModel.Facet = Facets.activityStartedBy

  override protected def extractIdAndLabel(projectTask: WorkspaceActivity[_])
                                          (implicit user: UserContext): Set[(String, String)] = {
    val userOpt =
      for(user <- projectTask.startedBy.user) yield {
        (user.uri, user.label)
      }
    userOpt.toSet
  }

}