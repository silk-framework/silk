package controllers.util

import java.io.StringWriter

import com.hp.hpl.jena.rdf.model.{ModelFactory, Model}
import org.apache.jena.riot.{Lang, RDFLanguages}
import org.silkframework.dataset._
import org.silkframework.dataset.rdf.SparqlParams
import org.silkframework.plugins.dataset.rdf.SparqlSink
import org.silkframework.plugins.dataset.rdf.endpoint.JenaModelEndpoint
import org.silkframework.plugins.dataset.rdf.formatters.{FormattedJenaLinkSink, NTriplesRdfFormatter}
import org.silkframework.runtime.resource.{EmptyResourceManager, InMemoryResourceManager, ResourceManager}
import org.silkframework.runtime.serialization.{ReadContext, XmlSerialization}
import org.silkframework.util.Identifier
import org.silkframework.workspace.{Project, Task, User}
import play.api.mvc.Result
import play.api.mvc.Results.Ok

import scala.reflect.ClassTag
import scala.xml.{Node, NodeSeq}

/**
  * Created by andreas on 12/10/15.
  */
object ProjectUtils {
  def getProjectAndTask[T: ClassTag](projectName: String, taskName: String): (Project, Task[T]) = {
    val project = User().workspace.project(projectName)
    val task = project.task[T](taskName)
    (project, task)
  }

  def jenaModelResult(model: Model, contentType: String): Result = {
    val writer = new StringWriter()
    val lang = Option(RDFLanguages.contentTypeToLang(contentType)).getOrElse(Lang.NTRIPLES)
    model.write(writer, lang.getName)
    Ok(writer.toString).as(contentType)
  }

  /**
    * Extract a specific dataset from a XML document
    *
    * @param xmlRoot
    * @param datasetId
    * @return
    */
  def createDataSource(xmlRoot: NodeSeq,
                       datasetId: Option[String])
                      (implicit resourceLoader: ResourceManager): DataSource = {
    val dataset = createDataset(xmlRoot, datasetId)
    dataset.source
  }

  /**
    * Extract all data sources from an XML document.
    *
    * @param xmlRoot
    * @return
    */
  def createDataSources(xmlRoot: NodeSeq)
                       (implicit resourceLoader: ResourceManager): Map[String, DataSource] = {
    val datasets = createAllDatasets(xmlRoot)
    datasets.map{ ds => (ds.id.toString, ds.source) }.toMap
  }

  /**
    * Returns the first dataset it can find in the DataSources element if datasetId is None
    * else returns the Dataset with the id defined by datasetId.
    *
    * @param xmlRoot      The element that contains the DataSources element
    * @param datasetIdOpt An optional id of the dataset.
    * @return
    */
  private def createDataset(xmlRoot: NodeSeq,
                            datasetIdOpt: Option[String])
                           (implicit resourceLoader: ResourceManager): Dataset = {
    val dataSources = xmlRoot \ "DataSources" \ "_"
    val dataSource = datasetIdOpt match {
      case Some(datasetId) =>
        dataSources filter hasAttributeValue("id", datasetId)
      case None =>
        dataSources
    }
    if (dataSource.isEmpty) {
      throw new IllegalArgumentException(s"No data source with id $datasetIdOpt specified")
    }
    implicit val readContext = ReadContext(resourceLoader)
    val dataset = XmlSerialization.fromXml[Dataset](dataSource.head)
    dataset
  }

  /** Creates all datasets found in the XML document */
  private def createAllDatasets(xmlRoot: NodeSeq)
                              (implicit resourceLoader: ResourceManager): Seq[Dataset] = {
    val dataSources = xmlRoot \ "DataSources" \ "_"
    implicit val readContext = ReadContext(resourceLoader)
    for(dataSource <- dataSources) yield {
      XmlSerialization.fromXml[Dataset](dataSource)
    }
  }

  // Create a data sink as specified in a REST request
  def createEntitySink(xmlRoot: NodeSeq): (Model, EntitySink) = {
    val dataSink = xmlRoot \ "dataSink"
    if (dataSink.isEmpty) {
      val model = ModelFactory.createDefaultModel()
      val inmemoryModelSink = new SparqlSink(SparqlParams(parallel = false), new JenaModelEndpoint(model))
      (model, inmemoryModelSink)
    } else {
      // Don't allow to read any resources like files, SPARQL endpoint is allowed, which does not need resources
      implicit val resourceManager = EmptyResourceManager
      val dataset = createDataset(dataSink, None)
      (null, dataset.entitySink)
    }
  }

  def createLinkSink(xmlRoot: NodeSeq): (Model, LinkSink) = {
    val linkSink = xmlRoot \ "linkSink"
    if (linkSink.isEmpty) {
      val model = ModelFactory.createDefaultModel()
      val inmemoryModelSink = new FormattedJenaLinkSink(model, new NTriplesRdfFormatter())
      (model, inmemoryModelSink)
    } else {
      // Don't allow to read any resources like files, SPARQL endpoint is allowed, which does not need resources
      implicit val resourceManager = EmptyResourceManager
      val dataset = createDataset(xmlRoot, None)
      (null, dataset.linkSink)
    }
  }

  /**
    * Reads all resource elements and load them into an in-memory resource manager
    *
    * @param xmlRoot The element that contains the resource elements
    * @return
    */
  def createInmemoryResourceManagerForResources(xmlRoot: NodeSeq): ResourceManager = {
    val resourceManager = InMemoryResourceManager()
    for (inputResource <- xmlRoot \ "resource") {
      val resourceId = inputResource \ s"@name"
      resourceManager.
          get(resourceId.text).
          write(inputResource.text)
    }
    resourceManager
  }

  /**
    * If the model is null, we assume that the result was written to the specified sink.
    * If the model exists, then write the result into the response body.
    *
    * @param model
    * @param noResponseBodyMessage The message that should be displayed if the model does not exist
    * @return
    */
  def result(model: Model, contentType: String, noResponseBodyMessage: String): Result = {
    if (model != null) {
      jenaModelResult(model, contentType)
    } else {
      // Result is written to registered sink
      Ok(noResponseBodyMessage)
    }
  }

  private def hasAttributeValue(attributeName: String, value: String)(node: Node): Boolean = {
    node.attribute(attributeName).filter(_.text == value).isDefined
  }
}
