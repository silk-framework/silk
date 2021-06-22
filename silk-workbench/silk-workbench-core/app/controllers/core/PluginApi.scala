package controllers.core

import controllers.util.SerializationUtils
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.runtime.plugin.PluginList
import play.api.mvc._

import javax.inject.Inject

@Tag(name = "Plugin API")
class PluginApi @Inject() () extends InjectedController {

  @Operation(
    summary = "All plugins",
    description = "Lists all available plugins. The returned JSON format stays as close to JSON Schema as possible.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(PluginApi.example))
        ))
      )
    ))
  def plugins(@Parameter(description = "If true, MarkDown documentation will be added for plugins if available.")
              addMarkdownDocumentation: Boolean): Action[AnyContent] = Action { implicit request => {
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

  @Operation(
    summary = "All plugins",
    description = "Lists all available plugins that implement the given plugin type. The returned JSON format stays as close to JSON Schema as possible.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        content = Array(new Content(
          mediaType = "application/json",
          examples = Array(new ExampleObject(PluginApi.example))
        ))
      )
    ))
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

object PluginApi {

  final val example =
"""
{
  "pluginId1": {
    "title": "human-readable plugin label",
    "description": "human-readable plugin description.",
    "markdownDocumentation": "Documentation:\n\n* Optional\n* more detailed\n* Markdown documentation",
    "type": "object",
    "properties": {
      "parameterName1": {
        "title": "parameter label",
        "description": "parameter description",
        "type": "string",
        "value": "",
        "advanced": false,
        "autoCompletion" : {
          "allowOnlyAutoCompletedValues" : true,
          "autoCompleteValueWithLabels" : true,
          "autoCompletionDependsOnParameters" : ["otherParamName"]
        }
      }
    },
    "required": []
  },
  "pluginId2": {
    "title": "human-readable plugin label",
    "description": "human-readable plugin description.",
    "type": "object",
    "properties": {},
    "required": []
  }
}
"""
}