package org.silkframework.workbench.workflow

import controllers.util.ProjectUtils.{createDatasets, createInMemoryResourceManagerForResources, getProject}
import org.silkframework.dataset.{Dataset, DatasetPluginAutoConfigurable}
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.runtime.plugin.{MultilineStringParameter, PluginContext}
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.serialization.json.WriteOnlyJsonFormat
import org.silkframework.workbench.utils.UnsupportedMediaTypeException
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.TaskActivityFactory
import org.silkframework.workspace.activity.workflow.{AllReplaceableDatasets, LocalWorkflowExecutorGeneratingProvenance, Workflow}
import play.api.libs.json._

import scala.xml.{Node, NodeSeq, XML}
import WorkflowWithPayloadExecutorFactory._
import org.silkframework.runtime.plugin.annotations.Plugin

@Plugin(
  id = "ExecuteWorkflowWithPayload",
  label = "Execute with payload",
  categories = Array("WorkflowExecution"),
  description = "Executes a workflow with custom payload."
)
case class WorkflowWithPayloadExecutorFactory(configuration: MultilineStringParameter = MultilineStringParameter(DEFAULT_CONFIGURATION),
                                              configurationType: String = "application/json")
  extends TaskActivityFactory[Workflow, WorkflowWithPayloadExecutor] {

  override def isSingleton: Boolean = false

  def apply(task: ProjectTask[Workflow]): Activity[WorkflowOutput] = {
    new WorkflowWithPayloadExecutor(task, configuration.str, configurationType)
  }
}

object WorkflowWithPayloadExecutorFactory {

  val DEFAULT_CONFIGURATION: String =
    """
      |{
      |  "DataSources": [
      |    {
      |      "id": "inputDataset",
      |      "data": {
      |        "taskType": "Dataset",
      |        "type": "json",
      |        "parameters": {
      |          "file": "test_file_resource"
      |        }
      |      }
      |    }
      |  ],
      |  "Sinks": [
      |    {
      |      "id": "outputDataset",
      |      "data": {
      |        "taskType": "Dataset",
      |        "type": "file",
      |        "parameters": {
      |          "file": "outputResource",
      |          "format": "N-Triples"
      |        }
      |      }
      |    }
      |  ],
      |  "Resources": {
      |    "test_file_resource": [
      |      {"id":"1"},
      |      {"id":"2" }
      |    ]
      |  }
      |}
    """.stripMargin
}

class WorkflowWithPayloadExecutor(task: ProjectTask[Workflow], configuration: String, configurationType: String) extends Activity[WorkflowOutput] {

  override def run(context: ActivityContext[WorkflowOutput])
                  (implicit userContext: UserContext): Unit = {

    val projectName = task.project.id
    val allReplaceableDatasets = task.data.allReplaceableDatasets(task.project)

    // Create sinks and resources for variable datasets, all resources are returned in the response
    val replaceableSinks = allReplaceableDatasets.sinks
    val (dataSources, sinks, resultResourceManager) = configurationType match {
      case "application/xml" | "text/xml" =>
        val xml = XML.loadString(configuration)
        createSourcesSinksFromXml(projectName, allReplaceableDatasets, replaceableSinks.toSet, xml)
      case "application/json" =>
        val json = Json.parse(configuration)
        createSourceSinksFromJson(projectName, allReplaceableDatasets, replaceableSinks.toSet, json)
      case _ =>
        throw UnsupportedMediaTypeException.supportedFormats("application/xml", "application/json")
    }
    checkReplaceableDatasetsCovered(allReplaceableDatasets, dataSources.keySet, sinks.keySet)
    context.value() = WorkflowOutput(sinks, replaceableSinks, resultResourceManager)

    val activity = LocalWorkflowExecutorGeneratingProvenance(task, dataSources, sinks, useLocalInternalDatasets = true)
    context.child(activity, 1.0).startBlocking()
  }

  // Checks that all replaceable input and output datasets get replaced via the provided payload
  private def checkReplaceableDatasetsCovered(allReplaceableDatasets: AllReplaceableDatasets,
                                              dataSourceReplacementSet: Set[String],
                                              sinkReplacementSet: Set[String]): Unit = {
    val missingDataSources = allReplaceableDatasets.dataSources.toSet.diff(dataSourceReplacementSet)
    val missingSinks = allReplaceableDatasets.sinks.toSet.diff(sinkReplacementSet)
    if(missingDataSources.nonEmpty || missingSinks.nonEmpty) {
      val dataSourceErrorString = if(missingSinks.nonEmpty) missingSinks.mkString(" IDs of non-replaced datasets for inputs", ", ", ".") else ""
      val sinkErrorString = if(missingSinks.nonEmpty) missingSinks.mkString(" IDs of non-replaced datasets for outputs: ", ", ", ".") else ""
      throw new IllegalArgumentException(s"Not all replaceable datasets in the workflow are replaced by the request payload!$dataSourceErrorString$sinkErrorString")
    }
  }

  private def createSourceSinksFromJson(projectName: String,
                                        variableDatasets: AllReplaceableDatasets,
                                        sinkIds: Set[String],
                                        json: JsValue)
                                       (implicit userContext: UserContext): (Map[String, Dataset], Map[String, Dataset], ResourceManager) = {
    val workflowJson = json.as[JsObject]
    var dataSources = {
      implicit val (resourceManager, _) = createInMemoryResourceManagerForResources(workflowJson, projectName, withProjectResources = true)
      createDatasets(workflowJson, Some(variableDatasets.dataSources.toSet), property = "DataSources")
    }
    // Sink
    val (sinkResourceManager, resultResourceManager) = createInMemoryResourceManagerForResources(workflowJson, projectName, withProjectResources = true)
    implicit val resourceManager: ResourceManager = sinkResourceManager
    val sinks = createDatasets(workflowJson, Some(sinkIds), property = "Sinks")
    val autoConfig = (workflowJson \ "config" \ "autoConfig").asOpt[Boolean].getOrElse(false)
    if(autoConfig) {
      implicit val pluginContext: PluginContext = PluginContext.fromProject(getProject(projectName))
      dataSources = dataSources.mapValues {
        case autoConfigDataset: DatasetPluginAutoConfigurable[_] => autoConfigDataset.autoConfigured
        case other: Dataset => other
      }
    }
    (dataSources, sinks, resultResourceManager)
  }

  private def createSourcesSinksFromXml(projectName: String, variableDatasets: AllReplaceableDatasets, sinkIds: Set[String], xmlRoot: NodeSeq)
                                       (implicit userContext: UserContext): (Map[String, Dataset], Map[String, Dataset], ResourceManager) = {
    // Create data sources from request payload
    val dataSources = {
      // Allow to read from project resources
      implicit val (resourceManager, _) = createInMemoryResourceManagerForResources(xmlRoot, projectName, withProjectResources = true)
      createDatasets(xmlRoot, Some(variableDatasets.dataSources.toSet), xmlElementTag = "DataSources")
    }
    // Sink
    val (sinkResourceManager, resultResourceManager) = createInMemoryResourceManagerForResources(xmlRoot, projectName, withProjectResources = true)
    implicit val resourceManager: ResourceManager = sinkResourceManager
    val sinks = createDatasets(xmlRoot, Some(sinkIds), xmlElementTag = "Sinks")
    (dataSources, sinks, resultResourceManager)
  }
}

case class WorkflowOutput(dataSinks: Map[String, Dataset], variableSinks: Seq[String], resourceManager: ResourceManager)

object WorkflowOutput {

  implicit object WorkflowOutputJsonFormat extends WriteOnlyJsonFormat[WorkflowOutput] {

    override def write(value: WorkflowOutput)
                      (implicit writeContext: WriteContext[JsValue]): JsValue = {
      val sink2ResourceMap = sinkToResourceMapping(value.dataSinks, value.variableSinks)
      variableSinkResultJson(value.resourceManager, sink2ResourceMap)
    }

    /** Result content for a replaceable dataset sink. */
    case class WorkflowOutputSinkContent(sinkId: String, textContent: String)

    object WorkflowOutputSinkContent {
      implicit val workflowOutputSinkContentFormat: Format[WorkflowOutputSinkContent] = Json.format[WorkflowOutputSinkContent]
    }

    private def variableSinkResultJson(resourceManager: ResourceManager, sink2ResourceMap: Map[String, String]) = {
      JsArray(
        for ((sinkId, resourceId) <- sink2ResourceMap.toSeq if resourceManager.exists(resourceId)) yield {
          val resource = resourceManager.get(resourceId, mustExist = true)
          Json.toJson(WorkflowOutputSinkContent(sinkId, resource.loadAsString()))
        }
      )
    }
  }

  implicit object WorkflowOutputXmlFormat extends XmlFormat[WorkflowOutput] {

    override def read(value: Node)(implicit readContext: ReadContext): WorkflowOutput = {
      throw new UnsupportedOperationException(s"Parsing values of type WorkflowPayload from Xml is not supported at the moment")
    }

    override def write(value: WorkflowOutput)(implicit writeContext: WriteContext[Node]): Node = {
      val sink2ResourceMap = sinkToResourceMapping(value.dataSinks, value.variableSinks)
      variableSinkResultXml(value.resourceManager, sink2ResourceMap)
    }

    private def variableSinkResultXml(resourceManager: ResourceManager, sink2ResourceMap: Map[String, String]) = {
      <WorkflowResults>
        {for ((sinkId, resourceId) <- sink2ResourceMap if resourceManager.exists(resourceId)) yield {
        val resource = resourceManager.get(resourceId, mustExist = true)
        <Result sinkId={sinkId}>{resource.loadAsString()}</Result>
      }}
      </WorkflowResults>
    }
  }

  private def sinkToResourceMapping(sinks: Map[String, Dataset], variableSinks: Seq[String]) = {
    variableSinks.map(s =>
      s -> sinks.get(s).flatMap(_.parameters.toStringMap.get("file")).getOrElse(s + "_file_resource")
    ).toMap
  }
}
