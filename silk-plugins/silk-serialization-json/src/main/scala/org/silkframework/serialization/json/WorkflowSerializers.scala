package org.silkframework.serialization.json

import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.JsonHelpers._
import org.silkframework.workspace.activity.workflow._
import play.api.libs.json.{JsArray, _}

object WorkflowSerializers {

  implicit object WorkflowJsonFormat extends JsonFormat[Workflow] {

    private final val TASKTYPE = "taskType"
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
    private final val WORKFLOW_CONTRIBUTION = "workflowContribution"

    private final val WORKFLOW_TYPE = "Workflow"

    override def typeNames: Set[String] = Set(WORKFLOW_TYPE)

    override def read(value: JsValue)(implicit readContext: ReadContext): Workflow = {
      Workflow(
        operators = mustBeJsArray(requiredValue(value, OPERATORS))(_.value.map(readOperator)),
        datasets = mustBeJsArray(requiredValue(value, DATASETS))(_.value.map(readDataset))
      )
    }

    override def write(value: Workflow)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        TASKTYPE -> WORKFLOW_TYPE,
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
        outputPriority = numberValueOption(value, OUTPUT_PRIORITY).map(_.toDouble),
        workflowContribution = numberValueOption(value, WORKFLOW_CONTRIBUTION).map(_.toDouble)
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
        outputPriority = numberValueOption(value, OUTPUT_PRIORITY).map(_.toDouble),
        workflowContribution = numberValueOption(value, WORKFLOW_CONTRIBUTION).map(_.toDouble)
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
}
