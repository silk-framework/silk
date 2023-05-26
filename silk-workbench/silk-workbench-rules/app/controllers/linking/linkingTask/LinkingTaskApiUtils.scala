package controllers.linking.linkingTask

import controllers.linking.evaluation.LinkageRuleEvaluationResult
import controllers.workspace.taskApi.TaskApiUtils
import org.silkframework.config.Prefixes
import org.silkframework.entity.ReferenceLink
import org.silkframework.plugins.path.PathMetaDataPlugin
import org.silkframework.rule.evaluation.{EvaluationResult, LinkageRuleEvaluator, ReferenceEntities}
import org.silkframework.rule.input.Transformer
import org.silkframework.rule.similarity.{Aggregator, DistanceMeasure}
import org.silkframework.rule.{LinkSpec, LinkageRule}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{PluginContext, PluginRegistry}
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.JsonSerializers._
import org.silkframework.serialization.json.{InputJsonSerializer, JsonHelpers, JsonSerialization}
import org.silkframework.workspace.{Project, ProjectTask, Workspace, WorkspaceFactory}
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}

object LinkingTaskApiUtils {
  /** Add parameter value labels for auto-completable parameters in the link rule, e.g. path labels or enumeration parameters etc.
    * This will replace the values directly inside the rules.
    */
  def getLinkSpecWithRuleNodeParameterValueLabels(linkingTask: ProjectTask[LinkSpec],
                                                  sourcePathLabels: Map[String, String],
                                                  targetPathLabels: Map[String, String])
                                                 (implicit userContext: UserContext): JsObject = {
    implicit val writeContext: WriteContext[JsValue] = WriteContext.fromProject[JsValue](linkingTask.project)
    implicit val workspace: Workspace = WorkspaceFactory().workspace
    implicit val project: Project = linkingTask.project
    // JSON only
    val linkSpec = linkingTask.data
    val linkSpecJson: JsObject = JsonSerialization.toJson[LinkSpec](linkSpec).as[JsObject]
    val linkSpecWithRuleParameterLabels = addLabelsRecursive(linkSpecJson, LabelReplaceData(sourcePathLabels, targetPathLabels, isTarget = false))
    val linkSpecWithParameterLabels = addParameterLabels(linkSpecWithRuleParameterLabels, linkingTask)
    linkSpecWithParameterLabels
  }

  private def addParameterLabels(linkSpecJson: JsObject,
                                 linkingTask: ProjectTask[LinkSpec])
                                (implicit userContext: UserContext): JsObject = {
    val parameterValue = (linkSpecJson \ PARAMETERS).as[JsObject].value
    val updatedParameters: JsObject = TaskApiUtils.parametersWithLabel(linkingTask.project.id, linkingTask, parameterValue)
    val updatedDataFields = linkSpecJson.fields ++ Seq(PARAMETERS -> updatedParameters)
    JsObject(updatedDataFields)
  }

  case class LabelReplaceData(sourcePathLabels: Map[String, String],
                              targetPathLabels: Map[String, String],
                              isTarget: Boolean)

  // Path to traverse in a linking rule without changing anything
  private val pathToTraverse = Seq(PARAMETERS, LinkSpecJsonFormat.RULE, OPERATOR)
  private val operatorTypes = Set(AggregationJsonFormat.AGGREGATION_TYPE, ComparisonJsonFormat.COMPARISON_TYPE, InputJsonSerializer.PATH_INPUT, InputJsonSerializer.TRANSFORM_INPUT)

  /** Add labels to linking rule operator parameter values if available */
  def addLabelsRecursive(jsArray: JsArray,
                         labelReplaceData: LabelReplaceData)
                        (implicit userContext: UserContext,
                         workspace: Workspace,
                         project: Project): JsArray = {
    JsArray(jsArray.value.map(v => addLabelsRecursive(v.as[JsObject], labelReplaceData)))
  }

  /** Adds labels to operator parameter values if available.
    *
    * @param jsObject         The JSON representation of any object that can occur in a linkage rule.
    *                         This object can be the JSON representation of the link spec or linkage rule itself
    * @param labelReplaceData Labels for source paths.
    */
  def addLabelsRecursive(jsObject: JsObject,
                         labelReplaceData: LabelReplaceData)
                        (implicit userContext: UserContext,
                         workspace: Workspace,
                         project: Project): JsObject = {
    (jsObject \ TYPE).asOpt[String] match {
      case Some(operatorType) if (operatorTypes.contains(operatorType)) =>
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
                                       labelReplaceData: LabelReplaceData)
                                      (implicit userContext: UserContext,
                                       workspace: Workspace,
                                       project: Project): JsObject = {
    operatorType match {
      case AggregationJsonFormat.AGGREGATION_TYPE =>
        val withAddedOperatorParameterLabels = updateOperatorParameterValues(jsObject, JsonHelpers.stringValue(jsObject, AggregationJsonFormat.AGGREGATOR), classOf[Aggregator])
        withAddedOperatorParameterLabels ++ Json.obj(
          AggregationJsonFormat.OPERATORS -> addLabelsRecursive((jsObject \ AggregationJsonFormat.OPERATORS).as[JsArray], labelReplaceData)
        )
      case ComparisonJsonFormat.COMPARISON_TYPE =>
        val withAddedOperatorParameterLabels = updateOperatorParameterValues(jsObject, JsonHelpers.stringValue(jsObject, ComparisonJsonFormat.METRIC), classOf[DistanceMeasure])
        withAddedOperatorParameterLabels ++ Json.obj(
          ComparisonJsonFormat.SOURCEINPUT -> addLabelsRecursive((jsObject \ ComparisonJsonFormat.SOURCEINPUT).as[JsObject], labelReplaceData.copy(isTarget = false)),
          ComparisonJsonFormat.TARGETINPUT -> addLabelsRecursive((jsObject \ ComparisonJsonFormat.TARGETINPUT).as[JsObject], labelReplaceData.copy(isTarget = true))
        )
      case InputJsonSerializer.TRANSFORM_INPUT =>
        val withAddedOperatorParameterLabels = updateOperatorParameterValues(jsObject, JsonHelpers.stringValue(jsObject, TransformInputJsonFormat.FUNCTION), classOf[Transformer])
        withAddedOperatorParameterLabels ++ Json.obj(
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

  private def updateOperatorParameterValues(operatorJson: JsObject,
                                            pluginId: String,
                                            pluginParentClass: Class[_])
                                           (implicit userContext: UserContext,
                                            workspace: Workspace,
                                            project: Project): JsObject = {
    PluginRegistry.pluginDescriptionsById(pluginId, assignableTo = Some(Seq(pluginParentClass))).headOption match {
      case Some(pluginDescription) =>
        val parameters = JsonHelpers.objectValue(operatorJson, PARAMETERS)
        implicit val prefixes: Prefixes = project.config.prefixes
        val updatedParameters = TaskApiUtils.addLabelsToValues(project.id, parameters.value, pluginDescription)
        operatorJson ++ Json.obj(
          PARAMETERS -> JsObject(updatedParameters)
        )
      case None =>
        operatorJson
    }
  }

  /** Path meta data plugins that augment input paths with meta data. */
  def pathMetaDataPlugins: Map[Class[_], PathMetaDataPlugin[_]] = {
    val pathMetaDataPlugins = PluginRegistry.availablePlugins[PathMetaDataPlugin[_]]
    pathMetaDataPlugins.map(plugin => {
      val pathMetaDataPlugin = plugin.apply()(PluginContext.empty)
      (pathMetaDataPlugin.sourcePluginClass, pathMetaDataPlugin)
    }).toMap
  }

  /** Score of reference links for a given linking rule. */
  def referenceLinkEvaluationScore(linkageRule: LinkageRule, referenceEntityCacheValue: ReferenceEntities): LinkageRuleEvaluationResult = {
    val score = LinkageRuleEvaluator(linkageRule, referenceEntityCacheValue)
    linkRuleEvaluationResult(score)
  }

  /** Score of reference links for a given linking rule. */
  def referenceLinkEvaluationScore(linkageRule: LinkageRule, referenceLinks: Seq[ReferenceLink], threshold: Double = 0.0): LinkageRuleEvaluationResult = {
    val score = LinkageRuleEvaluator(linkageRule, referenceLinks, threshold)
    linkRuleEvaluationResult(score)
  }

  private def linkRuleEvaluationResult(evaluationResult: EvaluationResult): LinkageRuleEvaluationResult = {
    LinkageRuleEvaluationResult(evaluationResult.truePositives, evaluationResult.trueNegatives, evaluationResult.falsePositives, evaluationResult.falseNegatives,
      f"${evaluationResult.fMeasure}%.2f", f"${evaluationResult.precision}%.2f", f"${evaluationResult.recall}%.2f")
  }
}
