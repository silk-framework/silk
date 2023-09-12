package org.silkframework.serialization.json

import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.JsonHelpers._
import org.silkframework.serialization.json.JsonSerializers.{PARAMETERS, TYPE, fromJson, toJson, UiAnnotationsJsonFormat}
import org.silkframework.workspace.activity.workflow._
import org.silkframework.workspace.annotation.UiAnnotations
import play.api.libs.json._

object WorkflowSerializers {

  implicit object WorkflowJsonFormat extends JsonFormat[Workflow] {

    private final val OPERATORS = "operators"
    private final val DATASETS = "datasets"
    final val UI_ANNOTATIONS = "uiAnnotations"
    final val REPLACEABLE_INPUTS = "replaceableInputs"
    final val REPLACEABLE_OUTPUTS = "replaceableOutputs"

    override def typeNames: Set[String] = Set(JsonSerializers.TASK_TYPE_WORKFLOW)

    override def read(value: JsValue)(implicit readContext: ReadContext): Workflow = {
      val parameterObject = optionalValue(value, PARAMETERS) match {
        case None => value
        case _ => objectValue(value, PARAMETERS)
      }
      Workflow(
        operators =  WorkflowOperatorsParameter(
          arrayValueOption(parameterObject, OPERATORS).map(_.value.map(WorkflowOperatorJsonFormat.read).toSeq).getOrElse(Seq.empty)),
        datasets = WorkflowDatasetsParameter(
          arrayValueOption(parameterObject, DATASETS).map(_.value.map(WorkflowDatasetJsonFormat.read).toSeq).getOrElse(Seq.empty)),
        uiAnnotations = optionalValue(parameterObject, UI_ANNOTATIONS).map(fromJson[UiAnnotations]).getOrElse(UiAnnotations()),
        replaceableInputs = arrayValueOption(parameterObject, REPLACEABLE_INPUTS).getOrElse(JsArray()).value.map(_.as[String]).toIndexedSeq,
        replaceableOutputs = arrayValueOption(parameterObject, REPLACEABLE_OUTPUTS).getOrElse(JsArray()).value.map(_.as[String]).toIndexedSeq
      )
    }

    override def write(value: Workflow)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        JsonSerializers.TASKTYPE -> JsonSerializers.TASK_TYPE_WORKFLOW,
        TYPE -> "workflow",
        JsonSerializers.PARAMETERS -> Json.obj(
          OPERATORS -> value.operators.map(WorkflowOperatorJsonFormat.write),
          DATASETS -> value.datasets.map(WorkflowDatasetJsonFormat.write),
          UI_ANNOTATIONS -> toJson(value.uiAnnotations),
          REPLACEABLE_INPUTS -> value.replaceableInputs.taskIds,
          REPLACEABLE_OUTPUTS -> value.replaceableOutputs.taskIds
        )
      )
    }
  }

  implicit object WorkflowOperatorsParameterFormat extends JsonFormat[WorkflowOperatorsParameter] {
    override def read(value: JsValue)(implicit readContext: ReadContext): WorkflowOperatorsParameter = {
      mustBeJsArray(value)(_.value.map(WorkflowOperatorJsonFormat.read).toSeq)
    }

    override def write(value: WorkflowOperatorsParameter)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsArray(value.value.map(WorkflowOperatorJsonFormat.write))
    }
  }

  implicit object WorkflowDatasetsParameterFormat extends JsonFormat[WorkflowDatasetsParameter] {
    override def read(value: JsValue)(implicit readContext: ReadContext): WorkflowDatasetsParameter = {
      mustBeJsArray(value)(_.value.map(WorkflowDatasetJsonFormat.read).toSeq)
    }

    override def write(value: WorkflowDatasetsParameter)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsArray(value.value.map(WorkflowDatasetJsonFormat.write))
    }
  }

  implicit object TaskIdentifierParameterFormat extends JsonFormat[TaskIdentifierParameter] {
    override def read(value: JsValue)(implicit readContext: ReadContext): TaskIdentifierParameter = {
      TaskIdentifierParameter(mustBeJsArray(value)(_.value.map(_.as[String])).toSeq)
    }

    override def write(value: TaskIdentifierParameter)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsArray(value.taskIds.map(id => JsString(id)))
    }
  }

  private final val POSX = "posX"
  private final val POSY = "posY"
  private final val TASK = "task"
  private final val INPUTS = "inputs"
  private final val CONFIG_INPUTS = "configInputs"
  private final val DEPENDENCY_INPUTS = "dependencyInputs"
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
        errorOutputs = mustBeJsArray(requiredValue(value, ERROR_OUTPUTS))(_.value.map(_.as[JsString].value).toSeq),
        position = nodePosition(value),
        nodeId = nodeId(value),
        outputPriority = outputPriority(value),
        configInputs = configInputs(value),
        dependencyInputs = dependencyInputs(value)
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
        CONFIG_INPUTS -> JsArray(op.configInputs.map(JsString)),
        DEPENDENCY_INPUTS -> JsArray(op.dependencyInputs.map(JsString))
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
        configInputs = configInputs(value),
        dependencyInputs = dependencyInputs(value)
      )
    }

    override def write(op: WorkflowDataset)(implicit writeContext: WriteContext[JsValue]): JsObject = {
      Json.obj(
        POSX -> op.position._1,
        POSY -> op.position._2,
        TASK -> op.task.toString,
        INPUTS -> JsArray(op.inputs.map(JsString)),
        OUTPUTS -> JsArray(op.outputs.map(JsString)),
        ID -> op.nodeId,
        OUTPUT_PRIORITY -> op.outputPriority,
        CONFIG_INPUTS -> JsArray(op.configInputs.map(JsString)),
        DEPENDENCY_INPUTS -> JsArray(op.dependencyInputs.map(JsString))
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
      mustBeJsArray(requiredValue(value, INPUTS))(_.value.map(_.as[JsString].value).toIndexedSeq)
    }

    protected def outputs(value: JsValue): IndexedSeq[String] = {
      mustBeJsArray(requiredValue(value, OUTPUTS))(_.value.map(_.as[JsString].value).toIndexedSeq)
    }

    protected def configInputs(value: JsValue): Seq[String] = {
      optionalValue(value, CONFIG_INPUTS).map(js => mustBeJsArray(js)(_.value.map(_.as[JsString].value)).toList).getOrElse(Seq.empty)
    }

    protected def dependencyInputs(value: JsValue): Seq[String] = {
      optionalValue(value, DEPENDENCY_INPUTS).map(js => mustBeJsArray(js)(_.value.map(_.as[JsString].value)).toList).getOrElse(Seq.empty)
    }
  }
}
