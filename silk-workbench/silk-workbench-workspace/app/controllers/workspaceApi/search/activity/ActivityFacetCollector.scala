package controllers.workspaceApi.search.activity

import controllers.workspaceApi.search.{FacetCollector, IdAndLabelKeywordFacetCollector, ItemTypeFacetCollector, NoLabelKeywordFacetCollector, SearchApiModel}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.activity.WorkspaceActivity

/**
  * Collects facet data of activities.
  */
case class ActivityFacetCollector() extends ItemTypeFacetCollector[WorkspaceActivity[_]] {
  override val facetCollectors: Seq[FacetCollector[WorkspaceActivity[_]]] = Seq(
    ActivityStatusCollector(),
    ActivityTypeCollector()
  )
}

case class ActivityStatusCollector() extends NoLabelKeywordFacetCollector[WorkspaceActivity[_]] {

  override def appliesForFacet: SearchApiModel.Facet = ActivityFacets.status

  override def extractKeywordIds(activity: WorkspaceActivity[_])
                                (implicit user: UserContext): Set[String] = {
    Set(activity.status().name)
  }

}

case class ActivityTypeCollector() extends IdAndLabelKeywordFacetCollector[WorkspaceActivity[_]] {

  override def appliesForFacet: SearchApiModel.Facet = ActivityFacets.activityType

  override protected def extractIdAndLabel(projectTask: WorkspaceActivity[_])
                                          (implicit user: UserContext): Set[(String, String)] = {
    if(projectTask.isCacheActivity) {
      Set(("cache", "Cache activity"))
    } else {
      Set(("non-cache", "Non-Cache activity"))
    }
  }
}