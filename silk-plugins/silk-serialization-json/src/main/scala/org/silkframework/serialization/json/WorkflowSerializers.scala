package org.silkframework.serialization.json

import org.silkframework.dataset.Dataset
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.JsonHelpers._
import org.silkframework.workspace.activity.workflow.{Workflow, WorkflowDataset, WorkflowOperator, WorkflowPayload}
import play.api.libs.json.{JsArray, _}

object WorkflowSerializers {

  implicit object WorkflowJsonFormat extends JsonFormat[Workflow] {

    private final val OPERATORS = "operators"
    private final val DATASETS = "datasets"

    private final val POSX = "posX"
    private final val POSY = "posY"
    private final val TASK = "task"
    private final val INPUTS = "inputs"
    private final val OUTPUTS = "outputs"
    private final val ERROR_OUTPUTS = "errorOutputs"
    private final val ID = "id"
    private final val OUTPUT_PRIORITY = "outputPriority"

    override def read(value: JsValue)(implicit readContext: ReadContext): Workflow = {
      Workflow(
        operators = mustBeJsArray(requiredValue(value, OPERATORS))(_.value.map(readOperator)),
        datasets = mustBeJsArray(requiredValue(value, DATASETS))(_.value.map(readDataset))
      )
    }

    override def write(value: Workflow)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        OPERATORS -> value.operators.map(writeOperator),
        DATASETS -> value.datasets.map(writeDataset)
      )
    }

    private def readOperator(value: JsValue): WorkflowOperator = {
      WorkflowOperator(
        inputs = mustBeJsArray(requiredValue(value, INPUTS))(_.value.map(_.as[JsString].value)),
        task = stringValue(value, TASK),
        outputs = mustBeJsArray(requiredValue(value, OUTPUTS))(_.value.map(_.as[JsString].value)),
        errorOutputs = mustBeJsArray(requiredValue(value, ERROR_OUTPUTS))(_.value.map(_.as[JsString].value)),
        position = (numberValue(value, POSX).toInt, numberValue(value, POSY).toInt),
        nodeId = stringValue(value, ID),
        outputPriority = numberValueOption(value, OUTPUT_PRIORITY).map(_.toDouble)
      )
    }

    private def writeOperator(op: WorkflowOperator): JsObject = {
      Json.obj(
        POSX -> op.position._1,
        POSY -> op.position._2,
        TASK -> op.task.toString,
        INPUTS -> JsArray(op.inputs.map(JsString)),
        OUTPUTS -> JsArray(op.outputs.map(JsString)),
        ERROR_OUTPUTS -> JsArray(op.errorOutputs.map(JsString)),
        ID -> op.nodeId.toString,
        OUTPUT_PRIORITY -> op.outputPriority
      )
    }

    private def readDataset(value: JsValue): WorkflowDataset = {
      WorkflowDataset(
        inputs = mustBeJsArray(requiredValue(value, INPUTS))(_.value.map(_.as[JsString].value)),
        task = stringValue(value, TASK),
        outputs = mustBeJsArray(requiredValue(value, OUTPUTS))(_.value.map(_.as[JsString].value)),
        position = (numberValue(value, POSX).toInt, numberValue(value, POSY).toInt),
        nodeId = stringValue(value, ID),
        outputPriority = numberValueOption(value, OUTPUT_PRIORITY).map(_.toDouble)
      )
    }

    private def writeDataset(op: WorkflowDataset): JsObject = {
      Json.obj(
        POSX -> op.position._1,
        POSY -> op.position._2,
        TASK -> op.task.toString,
        INPUTS -> JsArray(op.inputs.map(JsString)),
        OUTPUTS -> JsArray(op.outputs.map(JsString)),
        ID -> op.nodeId.toString,
        OUTPUT_PRIORITY -> op.outputPriority
      )
    }
  }

  implicit object WorkflowPayloadJsonFormat extends WriteOnlyJsonFormat[WorkflowPayload] {

    override def write(value: WorkflowPayload)
                      (implicit writeContext: WriteContext[JsValue]): JsValue = {
//      val resources = value.dataSinks.values.flatMap(_.referencedResources)
//      val resourceMap = resources.map(res => (res.name, res.loadAsString)).toMap
//      Json.obj("resources" -> resourceMap)

      val sink2ResourceMap = sinkToResourceMapping(value.dataSinks, value.variableSinks)
      variableSinkResultJson(value.resourceManager, sink2ResourceMap)
    }

    private def sinkToResourceMapping(sinks: Map[String, Dataset], variableSinks: Seq[String]) = {
      variableSinks.map(s =>
        s -> sinks.get(s).flatMap(_.parameters.get("file")).getOrElse(s + "_file_resource")
      ).toMap
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

}
