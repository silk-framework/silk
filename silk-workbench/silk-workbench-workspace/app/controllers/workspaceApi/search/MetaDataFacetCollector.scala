package controllers.workspaceApi.search

import controllers.workspaceApi.search.SearchApiModel.{Facet, Facets}
import org.silkframework.config.{HasMetaData, MetaData}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Uri

/**
  * Generic facets
  */
case class MetaDataFacetCollector() extends ItemTypeFacetCollector[HasMetaData] {
  override val facetCollectors: Seq[FacetCollector[HasMetaData]] = Seq(
    CreatedByFacetCollector(),
    LastModifiedByFacetCollector(),
    TagFacetCollector()
  )
}

/** Collects values for the "Created by" facet. */
case class CreatedByFacetCollector() extends UserFacetCollector {

  override def appliesForFacet: Facet = Facets.createdBy

  override protected def userUri(metaData: MetaData): Option[Uri] = metaData.createdByUser
}

/** Collects values for the "Last modified by" facet. */
case class LastModifiedByFacetCollector() extends UserFacetCollector {

  override def appliesForFacet: Facet = Facets.lastModifiedBy

  override protected def userUri(metaData: MetaData): Option[Uri] = metaData.lastModifiedByUser
}

/** User facet trait */
trait UserFacetCollector extends IdAndLabelKeywordFacetCollector[HasMetaData] {

  protected def userUri(metaData: MetaData): Option[Uri]

  override protected def extractIdAndLabel(projectTask: HasMetaData)
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
case class TagFacetCollector() extends IdAndLabelKeywordFacetCollector[HasMetaData] {

  /** The facet this collector applies for. */
  override def appliesForFacet: Facet = Facets.tags

  override protected def extractIdAndLabel(projectTask: HasMetaData)
                                          (implicit user: UserContext): Set[(String, String)] = {
    projectTask.tags().map(tag => (tag.uri, tag.label))
  }
}