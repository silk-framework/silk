package controllers.core

import helper.IntegrationTestTrait
import org.scalatest.FlatSpec
import org.silkframework.serialization.json.PluginSerializers

class PluginApiTest extends FlatSpec with IntegrationTestTrait {
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
}
