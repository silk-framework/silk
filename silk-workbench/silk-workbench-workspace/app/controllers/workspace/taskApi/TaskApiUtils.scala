package controllers.workspace.taskApi

import org.silkframework.config.{Prefixes, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin._
import org.silkframework.runtime.resource.EmptyResourceManager
import org.silkframework.workspace.{ProjectTask, Workspace, WorkspaceFactory}
import play.api.libs.json.{JsObject, JsString, JsValue}

import java.util.logging.Logger
import scala.util.control.NonFatal

object TaskApiUtils {
  private val log: Logger = Logger.getLogger(this.getClass.getName)

  /** Changes the value format from "param": <VALUE> to "param": {"value": <VALUE>, "label": "<LABEL>"}. The label is optional.
    * This will be applied recursively for nested parameter types.
    */
  def parametersWithLabel(projectName: String,
                          task: ProjectTask[_ <: TaskSpec],
                          parameterValues: collection.Map[String, JsValue])
                         (implicit userContext: UserContext): JsObject = {
    try {
      val pluginDescription = PluginDescription.forTask(task)
      implicit val workspace: Workspace = WorkspaceFactory().workspace
      implicit val prefixes: Prefixes = task.project.config.prefixes
      JsObject(addLabelsToValues(projectName, parameterValues, pluginDescription))
    } catch {
      case NonFatal(ex) =>
        log.warning(s"Could not get labels of plugin parameters for task '${task.label()}' in project '$projectName'. Details: " + ex.getMessage)
        JsObject(parameterValues.view.mapValues(value => JsObject(Seq("value" -> value))).toMap)
    }
  }

  /** Adds labels to parameter values of nested objects. This is guaranteed to only go at most one level deep, since objects
    * presented in the UI are not allowed to be nested multiple levels.
    *
    * @param parameterValues   Parameter values of the plugin.
    * @param pluginDescription The plugin description of the parameter.
    */
  def addLabelsToValues(projectName: String,
                        parameterValues: collection.Map[String, JsValue],
                        pluginDescription: PluginDescription[_])
                       (implicit userContext: UserContext,
                        workspace: Workspace,
                        prefixes: Prefixes): Map[String, JsValue] = {
    val parameterDescription = pluginDescription.parameters.map(p => (p.name, p)).toMap
    val updatedParameters = for ((parameterName, parameterValue) <- parameterValues) yield {
      val pd = parameterDescription.getOrElse(parameterName,
        throw new RuntimeException(s"Parameter '$parameterName' is not part of the parameter description of plugin '${pluginDescription.id}'."))
      val updatedValue = parameterValue match {
        case valueObj: JsObject if pd.visibleInDialog && pd.parameterType.isInstanceOf[PluginObjectParameterTypeTrait] =>
          val paramPluginDescription = ClassPluginDescription(pd.parameterType.asInstanceOf[PluginObjectParameterTypeTrait].pluginObjectParameterClass)
          val updatedInnerValues = addLabelsToValues(projectName, valueObj.value, paramPluginDescription)
          JsObject(Seq("value" -> JsObject(updatedInnerValues))) // Nested objects cannot have a label
        case jsString: JsString if pd.autoCompletion.isDefined && pd.autoCompletion.get.autoCompleteValueWithLabels && jsString.value != "" =>
          implicit val pluginContext: PluginContext = PluginContext(prefixes = prefixes, resources = EmptyResourceManager(),
            user = userContext, projectId = Some(projectName))
          val autoComplete = pd.autoCompletion.get
          val dependsOnParameterValues = ParamValue.createAll(fetchDependsOnValues(autoComplete, parameterValues),
                                                              autoComplete.autoCompletionDependsOnParameters, pluginDescription)
          val label = autoComplete.autoCompletionProvider.valueToLabel(jsString.value, dependsOnParameterValues, workspace)
          JsObject(Seq("value" -> jsString) ++ label.toSeq.map(l => "label" -> JsString(l)))
        case other: JsValue =>
          JsObject(Seq("value" -> other))
      }
      (parameterName, updatedValue)
    }
    updatedParameters.toMap
  }

  private def fetchDependsOnValues(autoComplete: ParameterAutoCompletion,
                                   parameterValues: collection.Map[String, JsValue]): Seq[SimpleParameterValue] = {
    autoComplete.autoCompletionDependsOnParameters.map { param =>
      parameterValues.getOrElse(param,
        throw new RuntimeException(s"No value found for plugin parameter '$param'. Could not retrieve label!")).asOpt[String] match {
        case Some(value) => ParameterStringValue(value)
        case None => throw new RuntimeException(s"Value of dependsOn parameter '${param}' is not a String based parameter.")
      }
    }
  }
}
