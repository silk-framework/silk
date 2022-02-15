package controllers.workspaceApi.search.activity

import controllers.workspaceApi.search.SearchApiModel.{Facet, FacetType}

object ActivityFacets {

  final val status: Facet = Facet("status", "Status", "The activity status.", FacetType.keyword)
  final val activityType: Facet = Facet("activityType", "Activity type", "Activity type (either cache or non-cache activity).", FacetType.keyword)
  final val startedBy: Facet = Facet("startedBy", "Started by", "The user that started the activity", FacetType.keyword)

  val facetIds: Seq[String] = Seq(status, activityType).map(_.id)
  assert(facetIds.distinct.size == facetIds.size, "Facet IDs must be unique!")
}

