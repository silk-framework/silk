package controllers.core

import helper.IntegrationTestTrait
import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.plugin.{AutoCompletionResult, PluginParameterAutoCompletionProvider, PluginRegistry}
import org.silkframework.serialization.json.{PluginParameterJsonPayload, PluginSerializers}
import play.api.libs.json.{JsValue, Json}

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
    autoCompletableTestPluginJson.autoCompleteSupport mustBe true
    autoCompletableTestPluginJson.autoCompleteValueWithLabels mustBe Some(true)
    autoCompletableTestPluginJson.allowOnlyAutoCompletedValues mustBe Some(true)
  }
}

@Plugin(
  id = "autoCompletableTestPlugin",
  label = "Test dummy auto completable plugin"
)
case class AutoCompletableTestPlugin(@Param(value = "Some param", autoCompletionProvider = classOf[TestAutoCompletionProvider],
                                            autoCompleteValueWithLabels = true, allowOnlyAutoCompletedValues = true)
                                            completableParam: String) extends Transformer {
  override def apply(values: Seq[Seq[String]]): Seq[String] = Seq.empty
}

case class TestAutoCompletionProvider() extends PluginParameterAutoCompletionProvider {
  val values = Seq("val1" -> "First value", "val2" -> "Second value", "val3" -> "Third value")

  override protected def autoComplete(searchQuery: String, projectId: String)
                                     (implicit userContext: UserContext): Traversable[AutoCompletionResult] = {
    val multiWordQuery = extractSearchTerms(searchQuery)
    values.filter(v => matchesSearchTerm(multiWordQuery, v._2)).map{ case (value, label) => AutoCompletionResult(value, Some(label))}
  }

  override def valueToLabel(value: String)
                           (implicit userContext: UserContext): Option[String] = {
    values.find(_._1 == value).map(_._2)
  }
}