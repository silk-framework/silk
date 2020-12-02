package controllers.workflowApi

import controllers.workflowApi.variableWorkflow.VariableWorkflowRequestUtils
import helper.IntegrationTestTrait
import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import play.api.libs.ws.WSResponse
import play.api.routing.Router

import scala.concurrent.Future

class SimpleVariableWorkflowApiTest extends FlatSpec
    with SingleProjectWorkspaceProviderTestTrait with IntegrationTestTrait with MustMatchers {
  behavior of "Simple variable workflow execution API"

  private val inputOnlyWorkflow = "7e4b3499-4743-40c7-81e6-72b33f34a496_Workflowinputonly"
  private val outputOnlyWorkflow = "67fe02eb-43a7-4b74-a6a2-c65a5c097636_Workflowoutputonly"
  private val validVariableWorkflows = Seq(
    "f5bbbdc1-3c80-425d-a2ce-3bb08aefde1c_Workflow",
    "0c338c22-c43e-4a1c-960d-da44b8176c56_Workflowmultipleofthesameinputandoutput",
    inputOnlyWorkflow,
    outputOnlyWorkflow
  )
  private val invalidVariableWorkflows = Seq(
    "283e0d90-514f-4f32-9a6f-81340d592b8f_Workflowtoomanysources",
    "293470bf-a1ff-42a3-a16a-3b0c5d8e3468_Workflowtoomanysinks"
  )
  private val brokenWorkflow = "da370f28-629a-4d01-a54b-5f0b8a15906f_BrokenWorkflow"
  // File the input only workflow writes to
  private val outputCsv = "output.csv"

  private val sourceProperty1 = "inputProp1"
  private val sourceProperty2 = "inputProp2"

  private def targetProp(nr: Int) = s"targetProp$nr"

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
      checkResponseExactStatusCode(executeVariableWorkflow(validVariableWorkflows.head, Map.empty, acceptMimeType = mimeType), NOT_ACCEPTABLE)
    }
  }

  it should "execute valid workflows" in {
    for(validWorkflow <- validVariableWorkflows) {
      checkResponse(executeVariableWorkflow(validWorkflow, Map.empty, acceptMimeType = "application/xml"))
    }
  }

  it should "return a 500 when the workflow execution fails" in {
    val response = checkResponseExactStatusCode(executeVariableWorkflow(brokenWorkflow, Map.empty, acceptMimeType = "application/xml"), INTERNAL_ERROR)
    (response.json \ "title").asOpt[String] must not be empty
  }

  it should "return the correct response bodies given valid ACCEPT values" in {
    // FIXME CMEM-3051: Enable JSON in test as soon as writing to it is supported
    for(mimeType <- VariableWorkflowRequestUtils.acceptedMimeType.filter(mimeType => mimeType != "application/json"
        && mimeType != "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
      for(IndexedSeq(param1Values, param2Values) <- Seq(
        IndexedSeq(Seq(), Seq()),
        IndexedSeq(Seq("val 1"), Seq("val 2")),
        IndexedSeq(Seq(), Seq("val A", "val B"))
      )) {
        for(usePost <- Seq(false, true)) {
          val parameterMap = Map(
            sourceProperty1 -> param1Values,
            sourceProperty2 -> param2Values
          ).filter(_._2.nonEmpty)
          val response = checkResponseExactStatusCode(
            executeVariableWorkflow(validVariableWorkflows.head, parameterMap, usePost, mimeType))
          checkForCorrectReturnType(mimeType, response.body, param1Values.isEmpty && param2Values.isEmpty)
          checkForValues(1, param1Values, response.body)
          checkForValues(2, param2Values, response.body)
        }
      }
    }
  }

  it should "return the correct response for an output only variable workflow" in {
    for(usePost <- Seq(true, false)) {
      val response = checkResponseExactStatusCode(
        executeVariableWorkflow(outputOnlyWorkflow, Map.empty, usePost, APPLICATION_XML))
      checkForCorrectReturnType(APPLICATION_XML, response.body, noValues = false)
      checkForValues(1, Seq("csv value 1"), response.body)
      checkForValues(2, Seq("csv value 2"), response.body)
    }
  }

  it should "write the correct output dataset for an input only variable workflow" in {
    val inputParams = Map(
      sourceProperty1 -> Seq("input value A"),
      sourceProperty2 -> Seq("XYZ")
    )
    for(usePost <- Seq(true, false)) {
      checkResponseExactStatusCode(
        executeVariableWorkflow(inputOnlyWorkflow, inputParams, usePost, APPLICATION_XML), NO_CONTENT)
      val outputCsvResource = project.resources.get(outputCsv)
      outputCsvResource.loadAsString.split("[\\r\\n]+") mustBe Seq("targetProp1,targetProp2", "input value A,XYZ")
      outputCsvResource.delete()
    }
  }

  it should "take the dataset content directly as POST payload" in {
    val inputValue = "some test value"
    for(payload <- Seq(
      (s"""{"$sourceProperty1":"$inputValue"}""", APPLICATION_JSON),
      (s"""[{"$sourceProperty1":"$inputValue"}]""", APPLICATION_JSON),
      (
          s"""<?xml version='1.0' encoding='utf-8'?>
             |<Root><$sourceProperty1>$inputValue</$sourceProperty1></Root>""".stripMargin, APPLICATION_XML),
      (s"""$sourceProperty1\n$inputValue""", "text/comma-separated-values"),
      (s"""$sourceProperty1\n$inputValue""", "text/csv")
    )) {
      val response = checkResponseExactStatusCode(
        executeVariableWorkflow(validVariableWorkflows.head, contentOpt = Some(payload)))
      checkForValues(1, Seq(inputValue), response.body)
    }
  }

  // Checks if all input values exist in the workflow output
  private def checkForValues(targetPropNr: Int, values: Seq[String], body: String): Unit = {
    for (value <- values) {
      body must include(targetProp(targetPropNr))
      body must include(value)
    }
  }

  // Checks if the returned result is indeed in the format of the MIME type.
  private def checkForCorrectReturnType(mimeType: String,
                                        body: String,
                                        noValues: Boolean): Unit = {
    mimeType match {
      case "application/xml" =>
        body must startWith("<?xml")
        body must include("<Entity")
      case "application/n-triples" =>
        if (noValues) {
          body mustBe ""
        } else {
          body must include("<urn:instance:variable_workflow_json_input")
        }
      case "text/comma-separated-values" | "text/csv" =>
        body must include(s"${targetProp(1)},${targetProp(2)}")
    }
  }

  // Executes a simple variable workflow
  private def executeVariableWorkflow(workflowId: String,
                                      parameters: Map[String, Seq[String]] = Map.empty,
                                      usePost: Boolean = false,
                                      acceptMimeType: String = "application/json",
                                      contentOpt: Option[(String, String)] = None): Future[WSResponse] = {
    val path = controllers.workflowApi.routes.ApiWorkflowApi.variableWorkflowResult(projectId, workflowId).url
    var request = client.url(s"$baseUrl$path")
        .withHttpHeaders(ACCEPT -> acceptMimeType)
    if(usePost || contentOpt.isDefined) {
      contentOpt match {
        case Some(content) =>
          request.withHttpHeaders(CONTENT_TYPE -> content._2).post(content._1)
        case None =>
          request.post(parameters)
      }
    } else {
      for((param, paramValues) <- parameters;
          paramValue <- paramValues) {
        request = request.addQueryStringParameters(param -> paramValue)
      }
      request.get()
    }
  }
}
