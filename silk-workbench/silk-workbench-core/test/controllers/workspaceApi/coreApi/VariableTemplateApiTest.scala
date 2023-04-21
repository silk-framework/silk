package controllers.workspaceApi.coreApi

import controllers.workspaceApi.coreApi
import controllers.workspaceApi.coreApi.VariableTemplateApi.TemplateVariablesFormat
import helper.{ApiClient, IntegrationTestTrait}
import org.scalatest.{FlatSpec, Matchers, MustMatchers}
import play.api.libs.json.Json
import controllers.workspaceApi.coreApi.routes.{VariableTemplateApi => TemplateApi}
import org.silkframework.runtime.templating.{TemplateVariable, TemplateVariables}
import org.silkframework.workspace.{ProjectConfig, WorkspaceFactory}

class VariableTemplateApiTest extends FlatSpec with IntegrationTestTrait with ApiClient with Matchers {

  behavior of "VariableTemplate API"

  override def workspaceProviderId: String = "inMemory"

  protected override def routes = Some(classOf[test.Routes])

  it should "allow managing variables" in {
    val projectName = "project"
    WorkspaceFactory().workspace.createProject(ProjectConfig(projectName))
    getVariables(projectName).variables shouldBe empty

    val variables = TemplateVariables(Seq(TemplateVariable("myVar", "myValue", None, isSensitive = false, "project")))
    putVariables(projectName, variables)
    getVariables(projectName) shouldBe variables
  }

  def getVariables(projectId: String): TemplateVariables = {
    val request = createRequest(TemplateApi.getVariables(projectId))
    val json = checkResponse(request.get()).json
    Json.fromJson[TemplateVariablesFormat](json).get.convert
  }

  def putVariables(projectId: String, variables: TemplateVariables): Unit = {
    val request = createRequest(TemplateApi.putVariables(projectId))
    checkResponse(request.post(Json.toJson(TemplateVariablesFormat(variables))))
  }
}
