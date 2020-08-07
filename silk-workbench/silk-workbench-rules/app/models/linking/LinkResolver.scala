package models.linking

import java.net.URLEncoder

import org.silkframework.config.DefaultConfig
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.rdf.RdfDataset

/**
  * Given an entity URI, generates a browser link that navigates to the entity.
  */
object LinkResolver {

  /**
    * If an eccenca DataPlatform is configured, generate Links to eccenca DataManager
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

  def apply(entityUri: String, dataset: GenericDatasetSpec): Option[String] = {
    dataset.plugin match {
      case ds: RdfDataset =>
        generateUrl(entityUri, ds)
      case _ =>
        None
    }
  }

  private def generateUrl(entityUri: String, dataset: RdfDataset) = {
    dataManagerUrl match {
      case Some(url) =>
        dataset.sparqlEndpoint.sparqlParams.graph match {
          case Some(graphUri) =>
            Some(s"$url}/explore?graph=${enc(graphUri)}&resource=${enc(entityUri)}")
          case None =>
            None
        }
      case None =>
        Some(entityUri)
    }
  }

  private def enc(uri: String): String = {
    URLEncoder.encode(uri, "UTF-8")
  }

}
