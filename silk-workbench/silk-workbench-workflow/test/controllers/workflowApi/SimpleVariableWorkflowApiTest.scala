package controllers.workflowApi

import controllers.workflowApi.variableWorkflow.VariableWorkflowRequestUtils
import helper.IntegrationTestTrait
import org.scalatest.FlatSpec
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import play.api.libs.ws.WSResponse
import play.api.routing.Router

import scala.concurrent.Future

class SimpleVariableWorkflowApiTest extends FlatSpec with SingleProjectWorkspaceProviderTestTrait with IntegrationTestTrait {
  behavior of "Simple variable workflow API"

  private val validVariableWorkflows = Seq(
    "f5bbbdc1-3c80-425d-a2ce-3bb08aefde1c_Workflow",
    "0c338c22-c43e-4a1c-960d-da44b8176c56_Workflowmultipleofthesameinputandoutput"
  )
  private val invalidVariableWorkflows = Seq(
    "283e0d90-514f-4f32-9a6f-81340d592b8f_Workflowtoomanysources",
    "293470bf-a1ff-42a3-a16a-3b0c5d8e3468_Workflowtoomanysinks"
  )

  override def routes: Option[Class[_ <: Router]] = Some(classOf[workflowApi.Routes])

  override def projectPathInClasspath: String = "Simplevariableworkflowproject.zip"

  override def workspaceProviderId: String = "inMemory"

  it should "not execute invalid workflows" in {
    for(invalidWorkflowId <- invalidVariableWorkflows) {
      checkResponseExactStatusCode(executeVariableWorkflow(invalidWorkflowId, Map.empty), BAD_REQUEST)
    }
  }

  it should "not accept invalid ACCEPT header value" in {
    for(mimeType <- Seq("application/msword", "text/plain")) {
      checkResponseExactStatusCode(executeVariableWorkflow(validVariableWorkflows.head, Map.empty, mimeType = mimeType), NOT_ACCEPTABLE)
    }
  }

  it should "execute valid workflows" in {
    for(validWorkflow <- validVariableWorkflows) {
      checkResponseExactStatusCode(executeVariableWorkflow(validWorkflow, Map.empty, mimeType = "application/xml"))
    }
  }

  it should "return the correct response bodies given valid ACCEPT values" in {
    // FIXME CMEM-3051: Enable JSON in test as soon as writing to it is supported
    for(mimeType <- VariableWorkflowRequestUtils.acceptedMimeType.filter(mimeType => mimeType != "application/json"
        && mimeType != "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
      checkResponseExactStatusCode(executeVariableWorkflow(validVariableWorkflows.head, Map(), mimeType = mimeType))
    }
  }

  private def executeVariableWorkflow(workflowId: String,
                                      parameters: Map[String, Seq[String]],
                                      usePost: Boolean = false,
                                      mimeType: String = "application/json"): Future[WSResponse] = {
    val path = controllers.workflowApi.routes.ApiWorkflowApi.variableWorkflowResult(projectId, workflowId).url
    var request = client.url(s"$baseUrl$path")
        .withHttpHeaders(ACCEPT -> mimeType)
    if(usePost) {
      request.post(parameters)
    } else {
      for((param, paramValues) <- parameters;
          paramValue <- paramValues) {
        request = request.addQueryStringParameters(param -> paramValue)
      }
      request.get()
    }
  }
}
