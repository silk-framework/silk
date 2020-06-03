package controllers.core

import controllers.util.SerializationUtils
import javax.inject.Inject
import org.silkframework.runtime.plugin._
import play.api.mvc.{Action, AnyContent, InjectedController, Request, Result}

class PluginApi @Inject() () extends InjectedController {

  def plugins(addMarkdownDocumentation: Boolean): Action[AnyContent] = Action { implicit request => {
    val pluginTypes = Seq(
      "org.silkframework.workspace.WorkspaceProvider",
      "org.silkframework.workspace.resources.ResourceRepository",
      "org.silkframework.config.CustomTask",
      "org.silkframework.dataset.Dataset",
      "org.silkframework.rule.similarity.DistanceMeasure",
      "org.silkframework.rule.input.Transformer",
      "org.silkframework.rule.similarity.Aggregator"
    )
    serialize(addMarkdownDocumentation, pluginTypes)
  }}

  def pluginsForTypes(pluginType: String, addMarkdownDocumentation: Boolean): Action[AnyContent] = Action { implicit request => {
    val pluginTypes = pluginType.split("\\s*,\\s*")
    serialize(addMarkdownDocumentation, pluginTypes)
  }}

  private def serialize(addMarkdownDocumentation: Boolean,
                        pluginTypes: Seq[String])
                       (implicit request: Request[AnyContent]): Result = {
    val pluginList = PluginList.load(pluginTypes, addMarkdownDocumentation)

    SerializationUtils.serializeCompileTime(pluginList, None)
  }
}
