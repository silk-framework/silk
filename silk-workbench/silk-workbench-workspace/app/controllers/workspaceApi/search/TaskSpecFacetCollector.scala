package controllers.workspaceApi.search

import controllers.workspaceApi.search.SearchApiModel.{Facet, Facets}
import org.silkframework.config.{MetaData, TaskSpec}
import org.silkframework.util.Uri
import org.silkframework.workspace.ProjectTask

import scala.collection.mutable

/**
  * Generic facets
  */
case class TaskSpecFacetCollector[T <: TaskSpec]() extends ItemTypeFacetCollector[T] {
  override val facetCollectors: Seq[FacetCollector[T]] = Seq(
    CreatedByFacetCollector[T](),
    LastModifiedByFacetCollector[T]()
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
trait UserFacetCollector[T <: TaskSpec] extends KeywordFacetCollector[T] {
  private val userUris = new mutable.ListMap[String, Int]()
  private val userLabels = new mutable.ListMap[String, String]()

  /** Collect facet values of a single facet. */
  override def collect(projectTask: ProjectTask[T]): Unit = {
    val metaData = projectTask.metaData
    val (userUri, label) = userUriAndLabel(metaData)
    userUris.put(userUri, userUris.getOrElseUpdate(userUri, 0) + 1)
    userLabels.put(userUri, label)
  }

  private def userUriAndLabel(metaData: MetaData): (String, String) = {
    userUri(metaData) match {
      case Some(userUri) => (userUri.uri, Uri(userUri).localName.getOrElse(userUri.uri))
      case None => ("", "Unknown user")
    }
  }

  protected def userUri(metaData: MetaData): Option[Uri]

  override def extractKeywordIds(projectTask: ProjectTask[T]): Set[String] = {
    val (userUri, _) = userUriAndLabel(projectTask.metaData)
    Set(userUri)
  }

  override def keywordStats: Seq[(String, String, Int)] = {
    userUris.toSeq.map(st => (st._1, userLabels(st._1), st._2))
  }
}