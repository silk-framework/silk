package org.silkframework.workspace.project

import helper.IntegrationTestTrait
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import play.api.libs.json.JsArray
import testWorkspace.Routes

class FixProjectTaskIntegrationTest extends AnyFlatSpec with Matchers with SingleProjectWorkspaceProviderTestTrait with IntegrationTestTrait {
  behavior of "Fix project task API"

  protected override def routes: Option[Class[Routes]] = Some(classOf[testWorkspace.Routes])

  override def workspaceProviderId: String = "inMemory"

  /**
    * Returns the path of the XML zip project that should be loaded before the test suite starts.
    */
  override def projectPathInClasspath: String = "diProjects/project-with-broken-tasks.zip"

  override def failOnTaskLoadingErrors: Boolean = false

  it should "There exists one task loading error" in {
    val url = controllers.projectApi.routes.ProjectApi.projectTasksLoadingErrorReport(projectId)
    val response = client.url(s"$baseUrl$url")
      .withHttpHeaders(ACCEPT -> APPLICATION_JSON)
      .get()
    val jsonResult = checkResponse(response).json
    jsonResult mustBe a[JsArray]
    jsonResult.as[JsArray].value must have size 1
  }
}
