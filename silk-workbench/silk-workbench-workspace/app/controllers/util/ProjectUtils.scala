package controllers.util

import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.riot.{Lang, RDFLanguages}
import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset._
import org.silkframework.dataset.rdf.{EntityRetrieverStrategy, SparqlParams}
import org.silkframework.plugins.dataset.rdf.access.SparqlSink
import org.silkframework.plugins.dataset.rdf.endpoint.JenaModelEndpoint
import org.silkframework.plugins.dataset.rdf.formatters.{FormattedJenaLinkSink, NTriplesRdfFormatter}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{FallbackResourceManager, InMemoryResourceManager, ResourceManager}
import org.silkframework.runtime.serialization.{ReadContext, XmlSerialization}
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.serialization.json.JsonSerializers
import org.silkframework.serialization.json.JsonSerializers.{TaskJsonFormat, _}
import org.silkframework.workspace.{Project, ProjectTask, WorkspaceFactory}
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results.Ok

import java.io.StringWriter
import scala.reflect.ClassTag
import scala.xml.{Node, NodeSeq}

/**
  * Utility functions for [[Project]]
  */
object ProjectUtils {
  def getProjectAndTask[T <: TaskSpec : ClassTag](projectName: String,
                                                  taskName: String)
                                                 (implicit userContext: UserContext): (Project, ProjectTask[T]) = {
    val project = getProject(projectName)
    val task = project.task[T](taskName)
    (project, task)
  }

  def getProject(projectName: String)
                (implicit userContext: UserContext): Project = {
    WorkspaceFactory().workspace.project(projectName)
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
    * @return
    */
  def createDataSource(xmlRoot: NodeSeq,
                       datasetId: Option[String])
                      (implicit resourceLoader: ResourceManager,
                       userContext: UserContext): DataSource = {
    val dataset = createDataset(xmlRoot, datasetId)
    dataset.source
  }

  def createDatasets(xmlRoot: NodeSeq,
                     datasetIds: Option[Set[String]],
                     xmlElementTag: String)
                    (implicit resourceLoader: ResourceManager): Map[String, Dataset] = {
    val datasets = createAllDatasets(xmlRoot, xmlElementTag, datasetIds)
    datasets.map { ds => (ds.id.toString, ds.data.plugin) }.toMap
  }

  /**
    * Create dataset objects from JSON serialization
    * @param workflowJson The JSON
    * @param datasetIds   Optional set of dataset IDs. If any of the found datasets is not in this set, an exception will be thrown.
    * @param property     The property in the JSON representation under which the array of dataset JSON specs is stored.
    */
  def createDatasets(workflowJson: JsObject,
                     datasetIds: Option[Set[String]],
                     property: String)
                    (implicit resourceLoader: ResourceManager): Map[String, Dataset] = {
    val datasets = createAllDatasets(workflowJson, property, datasetIds)
    datasets.map { ds => (ds.id.toString, ds.data.plugin) }.toMap
  }

  /**
    * Creates in-memory sink version of the selected datasets.
    * This does only work with [[Dataset]] that implement the [[WritableResourceDataset]] trait.
    *
    * @param sinkIds The dataset ids (keys) for which sinks should be created, the values of the map
    *                are the resource ids of the resource manager that should be used for each dataset.
    */
  def createInMemorySink(xmlRoot: NodeSeq,
                         sinkIds: Map[String, String])
                        (implicit resourceLoader: ResourceManager): Map[String, DatasetWriteAccess] = {
    val datasets = createDatasets(xmlRoot, Some(sinkIds.keySet), "Sinks")
    //    val datasetPlugins = datasets.map { ds =>
    //      val ds.plugin match {
    //        case plugin: DatasetPlugin with WritableResourceDatasetPlugin =>
    //          val writableResource = resourceLoader.get(sinkIds(ds.id.toString))
    //          plugin.replaceWritableResource(writableResource)
    //        case p =>
    //          val datasetId = ds.id.toString
    //          throw new RuntimeException(s"Type of dataset $datasetId does not support a writable resource. Pick a type that does, e.g. csv, file.")
    //      }
    //    }
    datasets
  }

  def createInMemoryDataset(xmlRoot: NodeSeq,
                            datasetIds: Map[String, String])
                        (implicit resourceLoader: ResourceManager): Map[String, Dataset] = {
    val datasets = createAllDatasets(xmlRoot, "Sinks", Some(datasetIds.keySet))
    //    val datasetPlugins = datasets.map { ds =>
    //      val ds.plugin match {
    //        case plugin: DatasetPlugin with WritableResourceDatasetPlugin =>
    //          val writableResource = resourceLoader.get(sinkIds(ds.id.toString))
    //          plugin.replaceWritableResource(writableResource)
    //        case p =>
    //          val datasetId = ds.id.toString
    //          throw new RuntimeException(s"Type of dataset $datasetId does not support a writable resource. Pick a type that does, e.g. csv, file.")
    //      }
    //    }
    datasets.map { ds => (ds.id.toString, ds.data.plugin) }.toMap
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
                           (implicit resourceLoader: ResourceManager): GenericDatasetSpec = {
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
    implicit val readContext: ReadContext = ReadContext(resourceLoader)
    val dataset = XmlSerialization.fromXml[GenericDatasetSpec](dataSource.head)
    dataset
  }

  /** Creates all datasets found in the XML document */
  private def createAllDatasets(xmlRoot: NodeSeq,
                                xmlElementName: String,
                                datasetIds: Option[Set[String]])
                               (implicit resourceLoader: ResourceManager): Seq[Task[GenericDatasetSpec]] = {
    val dataSources = xmlRoot \ xmlElementName \ "_"
    implicit val readContext: ReadContext = ReadContext(resourceLoader)
    val datasets = for (dataSource <- dataSources) yield {
      XmlSerialization.fromXml[Task[GenericDatasetSpec]](dataSource)
    }
    datasets.filter(ds => datasetIds.forall(_.contains(ds.id.toString)))
  }

  implicit val datasetTaskJsonFormat = new TaskJsonFormat[GenericDatasetSpec]()

  /** Creates all datasets found in the JSON document
    *
    * @param allowedDatasetIds Optional set of dataset IDs. If any of the found datasets is not in this set, an exception will be thrown.
    * @return
    */
  private def createAllDatasets(workflowJson: JsValue,
                                propertyName: String,
                                allowedDatasetIds: Option[Set[String]])
                               (implicit resourceLoader: ResourceManager): Seq[Task[GenericDatasetSpec]] = {
    val dataSources = (workflowJson \ propertyName).as[JsArray]
    implicit val readContext: ReadContext = ReadContext(resourceLoader)
    val datasets = for (dataSource <- dataSources.value) yield {
      JsonSerializers.fromJson[Task[GenericDatasetSpec]](dataSource)
    }
    allowedDatasetIds match {
      case Some(ids) =>
        val notMatched = datasets.filterNot(ds => ids.contains(ds.id.toString))
        if(notMatched.nonEmpty) {
          throw new IllegalArgumentException(s"Following dataset IDs were found in the request: ${notMatched.mkString(", ")}, but only following datasets IDs are valid: ${ids.mkString(", ")}")
        }
        datasets
      case None => datasets
    }
  }

  // Create a data sink as specified in a REST request
  def createEntitySink(xmlRoot: NodeSeq)
                      (implicit resourceManager: ResourceManager,
                       userContext: UserContext): (Model, EntitySink) = {
    val dataSink = xmlRoot \ "dataSink"
    if (dataSink.isEmpty) {
      val model = ModelFactory.createDefaultModel()
      val inmemoryModelSink = new SparqlSink(SparqlParams(strategy = EntityRetrieverStrategy.simple), new JenaModelEndpoint(model))
      (model, inmemoryModelSink)
    } else {
      // Don't allow to read any resources like files, SPARQL endpoint is allowed, which does not need resources
      val dataset = createDataset(dataSink, None)
      (null, dataset.entitySink)
    }
  }

  def createLinkSink(xmlRoot: NodeSeq)
                    (implicit resourceManager: ResourceManager,
                     userContext: UserContext): (Model, LinkSink) = {
    val linkSink = xmlRoot \ "linkSink"
    if (linkSink.isEmpty) {
      val model = ModelFactory.createDefaultModel()
      val inmemoryModelSink = new FormattedJenaLinkSink(model, new NTriplesRdfFormatter())
      (model, inmemoryModelSink)
    } else {
      // Don't allow to read any resources like files, SPARQL endpoint is allowed, which does not need resources
      val dataset = createDataset(xmlRoot, None)
      (null, dataset.linkSink)
    }
  }

  /**
    * Reads all resource elements and load them into an in-memory resource manager, use project resources as fallback.
    *
    * @param xmlRoot The element that contains the resource elements
    * @return The resource manager used for creating the sink and the in-memory resource manager to store results
    */
  def createInMemoryResourceManagerForResources(xmlRoot: NodeSeq,
                                                projectName: String,
                                                withProjectResources: Boolean)
                                               (implicit userContext: UserContext): (ResourceManager, ResourceManager) = {
    val resourceManager = InMemoryResourceManager()
    for (inputResource <- xmlRoot \ "resource") {
      val resourceId = inputResource \ s"@name"
      resourceManager.
          get(resourceId.text).
          writeString(inputResource.text)
    }
    wrapProjectResourceManager(projectName, withProjectResources, resourceManager)
  }

  def createInMemoryResourceManagerForResources(workflowJson: JsValue,
                                                projectName: String,
                                                withProjectResources: Boolean)
                                               (implicit userContext: UserContext): (ResourceManager, ResourceManager) = {
    val resourceManager = InMemoryResourceManager()
    val resources = (workflowJson \ "Resources").as[JsObject]
    for ((resourceId, resourceJs) <- resources.fields){
      val managedResource = resourceManager.
          get(resourceId)
      val resourceStringValue = resourceJs match {
        case jsObject: JsObject =>
          Json.stringify(jsObject)
        case jsArray: JsArray =>
          Json.stringify(jsArray)
        case jsString: JsString =>
          jsString.value
        case _ =>
          throw BadUserInputException("Resource value must be one of following JSON types: object, array or string")
      }
      managedResource.writeString(resourceStringValue)
    }
    wrapProjectResourceManager(projectName, withProjectResources, resourceManager)
  }

  private def wrapProjectResourceManager(projectName: String, withProjectResources: Boolean, resourceManager: ResourceManager)
                                        (implicit userContext: UserContext)= {
    if (withProjectResources) {
      val projectResourceManager = getProject(projectName).resources
      (FallbackResourceManager(resourceManager, projectResourceManager, writeIntoFallbackLoader = true, basePath = projectResourceManager.basePath), resourceManager)
    } else {
      (resourceManager, resourceManager)
    }
  }

  /**
    * If the model is null, we assume that the result was written to the specified sink.
    * If the model exists, then write the result into the response body.
    *
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
    node.attribute(attributeName).exists(_.text == value)
  }
}
