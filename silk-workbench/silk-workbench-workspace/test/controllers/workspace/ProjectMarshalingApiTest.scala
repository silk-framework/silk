package controllers.workspace

import helper.IntegrationTestTrait
import org.scalatestplus.play.PlaySpec
import org.silkframework.runtime.resource._
import org.silkframework.workspace.User
import play.api.libs.ws.{FileBody, InMemoryBody, WS}

class ProjectMarshalingApiTest extends PlaySpec with IntegrationTestTrait {

  protected override def routes = Some("workspace.Routes")

  override def workspaceProvider: String = "inMemory"

  "import the entire workspace" in {
    val workspaceBytes = ClasspathResource("controllers/workspace/workspace.zip").loadAsBytes
    importWorkspace(workspaceBytes)

    User().workspace.projects.map(_.config.id).toSet mustBe Set("example", "movies")
  }

  "export the entire workspace" in {
    val exportedWorkspace = exportWorkspace()
    exportedWorkspace.size > 100
  }

  private def importWorkspace(workspaceBytes: Array[Byte]): Unit = {
    val request = WS.url(s"$baseUrl/import/xmlZip")
    val response = request.post(workspaceBytes)
    checkResponse(response)
  }

  private def exportWorkspace(): Array[Byte] = {
    val request = WS.url(s"$baseUrl/export/xmlZip")
    val response = request.get()
    val result = checkResponse(response)
    result.bodyAsBytes
  }

}
