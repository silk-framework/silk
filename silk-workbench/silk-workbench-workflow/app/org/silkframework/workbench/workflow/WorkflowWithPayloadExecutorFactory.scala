package org.silkframework.workbench.workflow

import controllers.util.ProjectUtils.{createDatasets, createInMemoryResourceManagerForResources}
import org.silkframework.dataset.Dataset
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.runtime.plugin.{MultilineStringParameter, Plugin}
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.serialization.json.WriteOnlyJsonFormat
import org.silkframework.workbench.utils.UnsupportedMediaTypeException
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.TaskActivityFactory
import org.silkframework.workspace.activity.workflow.{AllVariableDatasets, LocalWorkflowExecutorGeneratingProvenance, Workflow}
import play.api.libs.json._
import scala.xml.{Node, NodeSeq, XML}
import WorkflowWithPayloadExecutorFactory._

@Plugin(
  id = "ExecuteWorkflowWithPayload",
  label = "Execute Workflow with payload",
  categories = Array("WorkflowExecution"),
  description = "Executes a workflow with custom payload."
)
case class WorkflowWithPayloadExecutorFactory(configuration: MultilineStringParameter = MultilineStringParameter(DEFAULT_CONFIGURATION),
                                              configurationType: String = "application/json")
  extends TaskActivityFactory[Workflow, WorkflowWithPayloadExecutor] {

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

    val projectName = task.project.name
    val variableDatasets = task.data.variableDatasets(task.project)

    // Create sinks and resources for variable datasets, all resources are returned in the response
    val variableSinks = variableDatasets.sinks
    val (dataSources, sinks, resultResourceManager) = configurationType match {
      case "application/xml" | "text/xml" =>
        val xml = XML.loadString(configuration)
        createSourcesSinksFromXml(projectName, variableDatasets, variableSinks.toSet, xml)
      case "application/json" =>
        val json = Json.parse(configuration)
        createSourceSinksFromJson(projectName, variableDatasets, variableSinks.toSet, json)
      case _ =>
        throw UnsupportedMediaTypeException.supportedFormats("application/xml", "application/json")
    }
    context.value() = WorkflowOutput(sinks, variableSinks, resultResourceManager)

    val activity = LocalWorkflowExecutorGeneratingProvenance(task, dataSources, sinks, useLocalInternalDatasets = true)
    context.child(activity, 1.0).startBlocking()
  }

  private def createSourceSinksFromJson(projectName: String, variableDatasets: AllVariableDatasets, sinkIds: Set[String], json: JsValue)
                                       (implicit userContext: UserContext): (Map[String, Dataset], Map[String, Dataset], ResourceManager) = {
    val workflowJson = json.as[JsObject]
    val dataSources = {
      implicit val (resourceManager, _) = createInMemoryResourceManagerForResources(workflowJson, projectName, withProjectResources = true)
      createDatasets(workflowJson, Some(variableDatasets.dataSources.toSet), property = "DataSources")
    }
    // Sink
    val (sinkResourceManager, resultResourceManager) = createInMemoryResourceManagerForResources(workflowJson, projectName, withProjectResources = true)
    implicit val resourceManager: ResourceManager = sinkResourceManager
    val sinks = createDatasets(workflowJson, Some(sinkIds), property = "Sinks")
    (dataSources, sinks, resultResourceManager)
  }

  private def createSourcesSinksFromXml(projectName: String, variableDatasets: AllVariableDatasets, sinkIds: Set[String], xmlRoot: NodeSeq)
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

    private def variableSinkResultJson(resourceManager: ResourceManager, sink2ResourceMap: Map[String, String]) = {
      JsArray(
        for ((sinkId, resourceId) <- sink2ResourceMap.toSeq if resourceManager.exists(resourceId)) yield {
          val resource = resourceManager.get(resourceId, mustExist = true)
          JsObject(Seq(
            "sinkId" -> JsString(sinkId),
            "textContent" -> JsString(resource.loadAsString)
          ))
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
        <Result sinkId={sinkId}>{resource.loadAsString}</Result>
      }}
      </WorkflowResults>
    }
  }

  private def sinkToResourceMapping(sinks: Map[String, Dataset], variableSinks: Seq[String]) = {
    variableSinks.map(s =>
      s -> sinks.get(s).flatMap(_.parameters.get("file")).getOrElse(s + "_file_resource")
    ).toMap
  }
}
