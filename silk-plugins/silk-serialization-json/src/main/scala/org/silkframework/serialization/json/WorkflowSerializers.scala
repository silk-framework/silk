package org.silkframework.serialization.json

import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.JsonHelpers._
import org.silkframework.workspace.activity.workflow._
import play.api.libs.json.{JsArray, _}

object WorkflowSerializers {

  implicit object WorkflowJsonFormat extends JsonFormat[Workflow] {

    private final val OPERATORS = "operators"
    private final val DATASETS = "datasets"

    private final val POSX = "posX"
    private final val POSY = "posY"
    private final val TASK = "task"
    private final val INPUTS = "inputs"
    private final val CONFIG_INPUTS = "configInputs"
    private final val OUTPUTS = "outputs"
    private final val ERROR_OUTPUTS = "errorOutputs"
    private final val ID = "id"
    private final val OUTPUT_PRIORITY = "outputPriority"

    override def typeNames: Set[String] = Set(JsonSerializers.TASK_TYPE_WORKFLOW)

    override def read(value: JsValue)(implicit readContext: ReadContext): Workflow = {
      Workflow(
        operators = mustBeJsArray(requiredValue(value, OPERATORS))(_.value.map(readOperator)),
        datasets = mustBeJsArray(requiredValue(value, DATASETS))(_.value.map(readDataset))
      )
    }

    override def write(value: Workflow)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        JsonSerializers.TASKTYPE -> JsonSerializers.TASK_TYPE_WORKFLOW,
        OPERATORS -> value.operators.map(writeOperator),
        DATASETS -> value.datasets.map(writeDataset)
      )
    }

    private def readOperator(value: JsValue): WorkflowOperator = {
      WorkflowOperator(
        inputs = inputs(value),
        task = task(value),
        outputs = outputs(value),
        errorOutputs = mustBeJsArray(requiredValue(value, ERROR_OUTPUTS))(_.value.map(_.as[JsString].value)),
        position = nodePosition(value),
        nodeId = nodeId(value),
        outputPriority = outputPriority(value),
        configInputs = configInputs(value)
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
        OUTPUT_PRIORITY -> op.outputPriority,
        CONFIG_INPUTS -> JsArray(op.configInputs.map(JsString))
      )
    }

    private def readDataset(value: JsValue): WorkflowDataset = {
      WorkflowDataset(
        inputs = inputs(value),
        task = task(value),
        outputs = outputs(value),
        position = nodePosition(value),
        nodeId = nodeId(value),
        outputPriority = outputPriority(value),
        configInputs = configInputs(value)
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

    private def nodeId(value: JsValue): String = {
      stringValue(value, ID)
    }

    private def outputPriority(value: JsValue): Option[Double] = {
      numberValueOption(value, OUTPUT_PRIORITY).map(_.toDouble)
    }

    private def nodePosition(value: JsValue): (Int, Int) = {
      (numberValue(value, POSX).toInt, numberValue(value, POSY).toInt)
    }

    private def task(value: JsValue): String = {
      stringValue(value, TASK)
    }

    private def inputs(value: JsValue): IndexedSeq[String] = {
      mustBeJsArray(requiredValue(value, INPUTS))(_.value.map(_.as[JsString].value))
    }

    private def outputs(value: JsValue): IndexedSeq[String] = {
      mustBeJsArray(requiredValue(value, OUTPUTS))(_.value.map(_.as[JsString].value))
    }

    private def configInputs(value: JsValue): Seq[String] = {
      optionalValue(value, CONFIG_INPUTS).map(js => mustBeJsArray(js)(_.value.map(_.as[JsString].value))).getOrElse(Seq.empty)
    }
  }
}
