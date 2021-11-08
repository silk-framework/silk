package models.linking

import java.net.URLEncoder

import org.silkframework.config.{DefaultConfig, Task, TaskSpec}
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
    * The label of the source task, where the entities are coming from.
    */
  def taskLabel: String

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
  val dataManagerUrl: Option[String] = {
    val config = DefaultConfig.instance()
    if(config.hasPath("eccencaDataManager.baseUrl")) {
      Some(config.getString("eccencaDataManager.baseUrl").stripSuffix("/"))
    } else {
      None
    }
  }

  /**
    * Returns a LinkResolver for a given task.
    */
  def forTask(sourceTask: Task[TaskSpec]): LinkResolver = {
    sourceTask.data match {
      case dataset: GenericDatasetSpec =>
        dataset.plugin match {
          case ds: RdfDataset =>
            dataManagerUrl match {
              case Some(url) =>
                new DataManagerResolver(sourceTask.label(), ds, url)
              case None =>
                DereferencingLinkResolver(sourceTask.label())
            }
          case _ =>
            NoLinkResolver(sourceTask.label())
        }
      case _ =>
        NoLinkResolver(sourceTask.label())
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
case class NoLinkResolver(taskLabel: String) extends LinkResolver {

  def apply(entityUri: String): Option[String] = None

}

/**
  * Directly links to the URI of the entity.
  * Can be used for entities that use dereferenceable URIs.
  */
case class DereferencingLinkResolver(taskLabel: String) extends LinkResolver {

  def apply(entityUri: String): Option[String] = {
    Some(entityUri)
  }

}

/**
  * Links to the entity page in the DataManager.
  */
class DataManagerResolver(val taskLabel: String, dataset: RdfDataset, dataManagerUrl: String) extends LinkResolver {

  private val graphOpt = dataset.graphOpt

  def apply(entityUri: String): Option[String] = {
    graphOpt match {
      case Some(graphUri) =>
        Some(s"$dataManagerUrl/explore?graph=${enc(graphUri)}&resource=${enc(entityUri)}")
      case None =>
        // The DataManager cannot resolve URIs without their corresponding graph so we return the URI itself
        Some(entityUri)
    }
  }

  private def enc(uri: String): String = {
    URLEncoder.encode(uri, "UTF-8")
  }

}
