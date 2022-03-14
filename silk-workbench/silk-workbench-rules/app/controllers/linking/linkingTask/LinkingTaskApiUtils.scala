package controllers.linking.linkingTask

import org.silkframework.rule.LinkSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.JsonSerializers._
import org.silkframework.serialization.json.{InputJsonSerializer, JsonSerialization}
import org.silkframework.workspace.ProjectTask
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}

object LinkingTaskApiUtils {
  /** Add parameter value labels for auto-completable parameters in the link rule, e.g. path labels or enumeration parameters etc.
    * This will replace the values directly inside the rules.
    */
  def getLinkSpecWithRuleNodeParameterValueLabels(linkingTask: ProjectTask[LinkSpec],
                                                  sourcePathLabels: Map[String, String],
                                                  targetPathLabels: Map[String, String])
                                                 (implicit userContext: UserContext): JsObject = {
    implicit val writeContext: WriteContext[JsValue] = WriteContext[JsValue](prefixes = linkingTask.project.config.prefixes, projectId = Some(linkingTask.project.config.id))
    // JSON only
    val linkSpec = linkingTask.data
    // TODO: CMEM-3873: Add other operator parameter labels
    val linkSpecJson: JsObject = JsonSerialization.toJson[LinkSpec](linkSpec).as[JsObject]
    val linkSpecWithPathLabels = addLabelsRecursive(linkSpecJson, LabelReplaceData(sourcePathLabels, targetPathLabels, isTarget = false))
    linkSpecWithPathLabels
  }

  private case class LabelReplaceData(sourcePathLabels: Map[String, String],
                                      targetPathLabels: Map[String, String],
                                      isTarget: Boolean)

  // Path to traverse in a linking rule without changing anything
  private val pathToTraverse = Seq(PARAMETERS, LinkSpecJsonFormat.RULE, OPERATOR)
  private val operatorTypes = Set(AggregationJsonFormat.AGGREGATION_TYPE, ComparisonJsonFormat.COMPARISON_TYPE, InputJsonSerializer.PATH_INPUT, InputJsonSerializer.TRANSFORM_INPUT)

  // Add labels to operator parameter values if available
  def addLabelsRecursive(jsArray: JsArray,
                         labelReplaceData: LabelReplaceData): JsArray = {
    JsArray(jsArray.value.map(v => addLabelsRecursive(v.as[JsObject], labelReplaceData)))
  }

  // Add labels to operator parameter values if available
  private def addLabelsRecursive(jsObject: JsObject,
                                 labelReplaceData: LabelReplaceData): JsObject = {
    (jsObject \ TYPE).asOpt[String] match {
      case Some(operatorType) if(operatorTypes.contains(operatorType)) =>
        addLabelsRecursiveByType(jsObject, operatorType, labelReplaceData)
      case _ =>
        pathToTraverse.find(path => (jsObject \ path).asOpt[JsObject].isDefined) match {
          case Some(path) =>
            jsObject ++ Json.obj(path -> addLabelsRecursive((jsObject \ path).as[JsObject], labelReplaceData))
          case None =>
            jsObject
        }
    }
  }

  private def addLabelsRecursiveByType(jsObject: JsObject,
                                       operatorType: String,
                                       labelReplaceData: LabelReplaceData): JsObject = {
    operatorType match {
      case AggregationJsonFormat.AGGREGATION_TYPE =>
        jsObject ++ Json.obj(
          AggregationJsonFormat.OPERATORS -> addLabelsRecursive((jsObject \ AggregationJsonFormat.OPERATORS).as[JsArray], labelReplaceData)
        )
      case ComparisonJsonFormat.COMPARISON_TYPE =>
        jsObject ++ Json.obj(
          ComparisonJsonFormat.SOURCEINPUT -> addLabelsRecursive((jsObject \ ComparisonJsonFormat.SOURCEINPUT).as[JsObject], labelReplaceData.copy(isTarget = false)),
          ComparisonJsonFormat.TARGETINPUT -> addLabelsRecursive((jsObject \ ComparisonJsonFormat.TARGETINPUT).as[JsObject], labelReplaceData.copy(isTarget = true))
        )
      case InputJsonSerializer.TRANSFORM_INPUT =>
        jsObject ++ Json.obj(
          TransformInputJsonFormat.INPUTS -> addLabelsRecursive((jsObject \ TransformInputJsonFormat.INPUTS).as[JsArray], labelReplaceData)
        )
      case InputJsonSerializer.PATH_INPUT =>
        val pathValue = (jsObject \ PathInputJsonFormat.PATH).as[String]
        val labelResolution = if(labelReplaceData.isTarget) labelReplaceData.targetPathLabels else labelReplaceData.sourcePathLabels
        labelResolution.get(pathValue) match {
          case Some(label) =>
            jsObject ++ Json.obj(
              PathInputJsonFormat.PATH -> Json.obj(
                "value" -> pathValue,
                "label" -> label
              )
            )
          case None =>
            jsObject
        }
      case _ => jsObject
    }
  }
}
