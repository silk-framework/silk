package org.silkframework.serialization.json

import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.JsonHelpers._
import org.silkframework.serialization.json.JsonSerializers.PARAMETERS
import org.silkframework.workspace.activity.workflow._
import play.api.libs.json.{JsArray, _}

object WorkflowSerializers {

  implicit object WorkflowJsonFormat extends JsonFormat[Workflow] {

    private final val OPERATORS = "operators"
    private final val DATASETS = "datasets"

    override def typeNames: Set[String] = Set(JsonSerializers.TASK_TYPE_WORKFLOW)

    override def read(value: JsValue)(implicit readContext: ReadContext): Workflow = {
      val parameterObject = optionalValue(value, PARAMETERS) match {
        case None => value
        case _ => objectValue(value, PARAMETERS)
      }
      Workflow(
        operators = mustBeJsArray(requiredValue(parameterObject, OPERATORS))(_.value.map(WorkflowOperatorJsonFormat.read)),
        datasets = mustBeJsArray(requiredValue(parameterObject, DATASETS))(_.value.map(WorkflowDatasetJsonFormat.read))
      )
    }

    override def write(value: Workflow)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        JsonSerializers.TASKTYPE -> JsonSerializers.TASK_TYPE_WORKFLOW,
        JsonSerializers.PARAMETERS -> Json.obj(
          OPERATORS -> value.operators.map(WorkflowOperatorJsonFormat.write),
          DATASETS -> value.datasets.map(WorkflowDatasetJsonFormat.write)
        )
      )
    }
  }

  implicit object WorkflowOperatorsParameterFormat extends JsonFormat[WorkflowOperatorsParameter] {
    override def read(value: JsValue)(implicit readContext: ReadContext): WorkflowOperatorsParameter = {
      mustBeJsArray(value)(_.value.map(WorkflowOperatorJsonFormat.read))
    }

    override def write(value: WorkflowOperatorsParameter)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsArray(value.value.map(WorkflowOperatorJsonFormat.write))
    }
  }

  implicit object WorkflowDatasetsParameterFormat extends JsonFormat[WorkflowDatasetsParameter] {
    override def read(value: JsValue)(implicit readContext: ReadContext): WorkflowDatasetsParameter = {
      mustBeJsArray(value)(_.value.map(WorkflowDatasetJsonFormat.read))
    }

    override def write(value: WorkflowDatasetsParameter)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsArray(value.value.map(WorkflowDatasetJsonFormat.write))
    }
  }

  private final val POSX = "posX"
  private final val POSY = "posY"
  private final val TASK = "task"
  private final val INPUTS = "inputs"
  private final val CONFIG_INPUTS = "configInputs"
  private final val OUTPUTS = "outputs"
  private final val ERROR_OUTPUTS = "errorOutputs"
  private final val ID = "id"
  private final val OUTPUT_PRIORITY = "outputPriority"

  implicit object WorkflowOperatorJsonFormat extends JsonFormat[WorkflowOperator] with WorkflowNodeFormatTrait {

    override def read(value: JsValue)(implicit readContext: ReadContext): WorkflowOperator = {
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

    override def write(op: WorkflowOperator)(implicit writeContext: WriteContext[JsValue]): JsObject = {
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
  }

  implicit object WorkflowDatasetJsonFormat extends JsonFormat[WorkflowDataset] with WorkflowNodeFormatTrait {
    override def read(value: JsValue)(implicit readContext: ReadContext): WorkflowDataset = {
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

    override def write(op: WorkflowDataset)(implicit writeContext: WriteContext[JsValue]): JsObject = {
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

  trait WorkflowNodeFormatTrait {
    protected def nodeId(value: JsValue): String = {
      stringValue(value, ID)
    }

    protected def outputPriority(value: JsValue): Option[Double] = {
      numberValueOption(value, OUTPUT_PRIORITY).map(_.toDouble)
    }

    protected def nodePosition(value: JsValue): (Int, Int) = {
      (numberValue(value, POSX).toInt, numberValue(value, POSY).toInt)
    }

    protected def task(value: JsValue): String = {
      stringValue(value, TASK)
    }

    protected def inputs(value: JsValue): IndexedSeq[String] = {
      mustBeJsArray(requiredValue(value, INPUTS))(_.value.map(_.as[JsString].value))
    }

    protected def outputs(value: JsValue): IndexedSeq[String] = {
      mustBeJsArray(requiredValue(value, OUTPUTS))(_.value.map(_.as[JsString].value))
    }

    protected def configInputs(value: JsValue): Seq[String] = {
      optionalValue(value, CONFIG_INPUTS).map(js => mustBeJsArray(js)(_.value.map(_.as[JsString].value))).getOrElse(Seq.empty)
    }
  }
}
