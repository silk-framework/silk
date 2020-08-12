package models.linking

import java.net.URLEncoder

import org.silkframework.config.{DefaultConfig, TaskSpec}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.rdf.RdfDataset
import org.silkframework.rule.LinkSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.DPair
import org.silkframework.workspace.ProjectTask
import play.api.mvc.MultipartFormData.DataPart

/**
  * Given an entity URI, generates a browser link that navigates to the entity.
  */
trait LinkResolver {

  /**
    * Returns a browser URL for a given entity.
    *
    * @param entityUri The URI of the entity.
    *
    */
  def apply(entityUri: String): Option[String]

}

object LinkResolver {

  /**
    * The URL of the configured eccenca DataManager, if any.
    */
  private val dataManagerUrl: Option[String] = {
    val config = DefaultConfig.instance()
    if(config.hasPath("eccencaDataPlatform.url")) {
      val dataPlatformUrl = config.getString("eccencaDataPlatform.url").stripSuffix("/")
      Some(dataPlatformUrl.stripSuffix("dataplatform"))
    } else {
      None
    }
  }

  /**
    * Returns a LinkResolver for a given task.
    */
  def forTask(sourceTask: TaskSpec): LinkResolver = {
    sourceTask match {
      case dataset: GenericDatasetSpec =>
        dataset.plugin match {
          case ds: RdfDataset =>
            dataManagerUrl match {
              case Some(url) =>
                new DataManagerResolver(ds, url)
              case None =>
                new DereferencingLinkResolver()
            }
          case _ =>
            NoLinkResolver
        }
      case _ =>
        NoLinkResolver
    }
  }

  def forLinkingTask(linkTask: ProjectTask[LinkSpec])(implicit userContext: UserContext): DPair[LinkResolver] = {
    for(selection <- linkTask.dataSelections) yield {
      forTask(linkTask.project.anyTask(selection.inputId))
    }
  }

}

/**
  * Does not generate any links for entities.
  * Used for entities from sources for which we do not have corresponding entity detail pages (CSV, etc.).
  */
object NoLinkResolver extends LinkResolver {

  def apply(entityUri: String): Option[String] = None

}

/**
  * Directly links to the URI of the entity.
  * Can be used for entities that use dereferenceable URIs.
  */
class DereferencingLinkResolver extends LinkResolver {

  def apply(entityUri: String): Option[String] = {
    Some(entityUri)
  }

}

/**
  * Links to the entity page in the DataManager.
  */
class DataManagerResolver(dataset: RdfDataset, dataManagerUrl: String) extends LinkResolver {

  def apply(entityUri: String): Option[String] = {
    dataset.sparqlEndpoint.sparqlParams.graph match {
      case Some(graphUri) =>
        Some(s"$dataManagerUrl}/explore?graph=${enc(graphUri)}&resource=${enc(entityUri)}")
      case None =>
        None
    }
  }

  private def enc(uri: String): String = {
    URLEncoder.encode(uri, "UTF-8")
  }

}
