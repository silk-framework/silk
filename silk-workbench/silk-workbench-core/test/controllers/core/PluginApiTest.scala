package controllers.core

import helper.IntegrationTestTrait
import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.config.CustomTask
import org.silkframework.entity.EntitySchema
import org.silkframework.plugins.dataset.rdf.tasks.SparqlSelectCustomTask
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.plugin.{AutoCompletionResult, PluginDescription, PluginParameterAutoCompletionProvider, PluginRegistry}
import org.silkframework.serialization.json.{PluginParameterJsonPayload, PluginSerializers}
import org.silkframework.workspace.WorkspaceReadTrait
import play.api.libs.json.{JsObject, JsValue, Json}

class PluginApiTest extends FlatSpec with IntegrationTestTrait with MustMatchers {
  behavior of "Plugin API"

  override def workspaceProviderId = "inMemory"

  protected override def routes = Some(classOf[test.Routes])

  it should "return markdown documentation for the /plugins endpoints if requested" in {
    for(addMarkdown <- Seq(false, true);
        endpoint <- Seq("plugins", "plugins/org.silkframework.dataset.Dataset")) {
      val request = client.url(s"$baseUrl/core/$endpoint?addMarkdownDocumentation=$addMarkdown")
      val response = request.get
      val markdownDocs = (checkResponse(response).json \\ PluginSerializers.MARKDOWN_DOCUMENTATION_PARAMETER).map(_.as[String])
      val markdownExists = markdownDocs.size > 0
      assert(!(markdownExists ^ addMarkdown), s"For endpoint '$endpoint' markdown was ${if(addMarkdown) "" else "not "}" +
          s"requested, but Markdown was ${if(markdownExists) "found" else "missing"}.")
    }
  }

  it should "return the correct properties for auto-completable plugins" in {
    PluginRegistry.registerPlugin(classOf[AutoCompletableTestPlugin])
    val jsonResult = checkResponse(client.url(s"$baseUrl/core/plugins").get()).json
    val autoCompletableTestPluginJson = Json.fromJson[PluginParameterJsonPayload]((jsonResult \ "autoCompletableTestPlugin" \ "properties" \ "completableParam").as[JsValue]).get
    val autoComplete = autoCompletableTestPluginJson.autoCompletion
    autoComplete mustBe defined
    autoComplete.get.autoCompleteValueWithLabels mustBe true
    autoComplete.get.allowOnlyAutoCompletedValues mustBe true
    autoComplete.get.autoCompletionDependsOnParameters mustBe Seq("otherParam")
  }

  it should "return the correct plugins for the task plugins endpoint" in {
    val jsonResult = checkResponse(client.url(s"$baseUrl/api/core/taskPlugins").get()).json
    jsonResult.as[JsObject].keys must contain allOf("transform", "linking", "workflow", "csv", "sparqlSelectOperator")
  }

  it should "return all plugins of a specific category" in {
    val transformPd = PluginDescription(classOf[TransformSpec])
    val sparqlSelectPd = PluginDescription(classOf[SparqlSelectCustomTask])
    val categoryOnlyInTransform = transformPd.categories.diff(sparqlSelectPd.categories)
    categoryOnlyInTransform must not be empty
    val jsonResult = checkResponse(client.url(s"$baseUrl/api/core/taskPlugins?category=${categoryOnlyInTransform.head}").get()).json
    val pluginIds = jsonResult.as[JsObject].keys
    pluginIds must contain (transformPd.id.toString)
    pluginIds must not contain (sparqlSelectPd.id.toString)
  }

  it should "filter all plugins by text query" in {
    // in label
    checkResponse(client.url(s"$baseUrl/api/core/taskPlugins?textQuery=dummy+test").get()).
        json.as[JsObject].keys mustBe Set("autoCompletableTestPlugin")
    // in description
    checkResponse(client.url(s"$baseUrl/api/core/taskPlugins?textQuery=unique+string+description").get()).
        json.as[JsObject].keys mustBe Set("autoCompletableTestPlugin")
    // In mix of label and description
    checkResponse(client.url(s"$baseUrl/api/core/taskPlugins?textQuery=unique+dummy").get()).
        json.as[JsObject].keys mustBe Set("autoCompletableTestPlugin")
  }
}

@Plugin(
  id = "autoCompletableTestPlugin",
  label = "Test dummy auto completable plugin",
  description = "Some unique description string"
)
case class AutoCompletableTestPlugin(@Param(value = "Some param", autoCompletionProvider = classOf[TestAutoCompletionProvider],
                                            autoCompleteValueWithLabels = true, allowOnlyAutoCompletedValues = true, autoCompletionDependsOnParameters = Array("otherParam"))
                                     completableParam: String,
                                     otherParam: String) extends CustomTask {
  override def inputSchemataOpt: Option[Seq[EntitySchema]] = None
  override def outputSchemaOpt: Option[EntitySchema] = None
}

case class TestAutoCompletionProvider() extends PluginParameterAutoCompletionProvider {
  val values = Seq("val1" -> "First value", "val2" -> "Second value", "val3" -> "Third value")

  override def autoComplete(searchQuery: String, projectId: String, dependOnParameterValues: Seq[String], workspace: WorkspaceReadTrait)
                                     (implicit userContext: UserContext): Traversable[AutoCompletionResult] = {
    val multiWordQuery = extractSearchTerms(searchQuery)
    values.filter(v => matchesSearchTerm(multiWordQuery, v._2)).map{ case (value, label) => AutoCompletionResult(value, Some(label))}
  }

  override def valueToLabel(projectId: String, value: String, dependOnParameterValues: Seq[String], workspace: WorkspaceReadTrait)
                           (implicit userContext: UserContext): Option[String] = {
    values.find(_._1 == value).map(_._2)
  }
}