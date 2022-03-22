package controllers.workspaceApi.search

import controllers.workspaceApi.search.SearchApiModel.{Facet, Facets}
import org.silkframework.config.{MetaData, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Uri
import org.silkframework.workspace.ProjectTask

/**
  * Generic facets
  */
case class TaskSpecFacetCollector[T <: TaskSpec]() extends ItemTypeFacetCollector[T] {
  override val facetCollectors: Seq[FacetCollector[T]] = Seq(
    CreatedByFacetCollector[T](),
    LastModifiedByFacetCollector[T](),
    TaskTagCollector[T]()
  )
}

/** Collects values for the "Created by" facet. */
case class CreatedByFacetCollector[T <: TaskSpec]() extends UserFacetCollector[T] {

  override def appliesForFacet: Facet = Facets.createdBy

  override protected def userUri(metaData: MetaData): Option[Uri] = metaData.createdByUser
}

/** Collects values for the "Last modified by" facet. */
case class LastModifiedByFacetCollector[T <: TaskSpec]() extends UserFacetCollector[T] {

  override def appliesForFacet: Facet = Facets.lastModifiedBy

  override protected def userUri(metaData: MetaData): Option[Uri] = metaData.lastModifiedByUser
}

/** User facet trait */
trait UserFacetCollector[T <: TaskSpec] extends IdAndLabelKeywordFacetCollector[T] {

  protected def userUri(metaData: MetaData): Option[Uri]

  override protected def extractIdAndLabel(projectTask: ProjectTask[T])
                                          (implicit user: UserContext): Set[(String, String)] = {
    val metaData = projectTask.metaData
    val uriAndLabel = userUri(metaData) match {
      case Some(userUri) => (userUri.uri, Uri(userUri).localName.getOrElse(userUri.uri))
      case None => ("", "Unknown user")
    }
    Set(uriAndLabel)
  }

}

/** Collects values for the "tags" facet. */
case class TaskTagCollector[T <: TaskSpec]() extends IdAndLabelKeywordFacetCollector[T] {

  /** The facet this collector applies for. */
  override def appliesForFacet: Facet = Facets.tags

  override protected def extractIdAndLabel(projectTask: ProjectTask[T])
                                          (implicit user: UserContext): Set[(String, String)] = {
    projectTask.tags().map(tag => (tag.uri.toString, tag.label))
  }
}