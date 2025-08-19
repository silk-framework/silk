package controllers.workflowApi

import akka.stream.scaladsl.{FileIO, Source}
import controllers.workflowApi.variableWorkflow.VariableWorkflowRequestUtils
import controllers.workflowApi.workflow.WorkflowInfo
import controllers.workspace.ActivityClient
import controllers.workspace.activityApi.StartActivityResponse
import helper.IntegrationTestTrait
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.dataset.DatasetSpec
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.plugins.dataset.BinaryFileDataset
import org.silkframework.plugins.dataset.rdf.datasets.InMemoryDataset
import org.silkframework.runtime.resource.FileResource
import org.silkframework.serialization.json.JsonHelpers
import org.silkframework.util.FileUtils
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import org.silkframework.workspace.activity.workflow.{Workflow, WorkflowDataset, WorkflowOperator}
import org.silkframework.workspace.annotation.UiAnnotations
import play.api.libs.json.JsArray
import play.api.libs.ws.WSResponse
import play.api.mvc.MultipartFormData.FilePart
import play.api.routing.Router

import java.io.File
import scala.concurrent.Future

class SimpleVariableWorkflowApiTest extends AnyFlatSpec with BeforeAndAfterAll
    with SingleProjectWorkspaceProviderTestTrait with IntegrationTestTrait with Matchers {
  behavior of "Simple variable workflow execution API"

  private val variableInputDataset = "1e80c0ed-9ca9-4d67-8868-65f7655aa416_Variableinputdataset"
  private val variableOutputDataset = "3a41ee9d-1ee7-4abe-9a62-603015abdb20_VariableOutput"
  private val additionalVarDataset = "6bc6f320-7683-42f8-a6f5-dd580d6de160_Varibledatasetadditional"

  private val inputOnlyWorkflow = "7e4b3499-4743-40c7-81e6-72b33f34a496_Workflowinputonly"
  private val outputOnlyWorkflow = "67fe02eb-43a7-4b74-a6a2-c65a5c097636_Workflowoutputonly"
  private val inputOutputWorkflow = "f5bbbdc1-3c80-425d-a2ce-3bb08aefde1c_Workflow"
  private val validVariableWorkflows = Seq(
    inputOutputWorkflow,
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

  override def routes: Option[Class[_ <: Router]] = Some(classOf[testWorkflowApi.Routes])

  override def projectPathInClasspath: String = "Simplevariableworkflowproject.zip"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  private val nonEmptyRequestParameters = Map(sourceProperty1 -> Seq("val"))
  it should "not execute invalid workflows" in {
    for (invalidWorkflowId <- invalidVariableWorkflows) {
      checkResponseExactStatusCode(executeVariableWorkflow(invalidWorkflowId, Map.empty), BAD_REQUEST)
    }
  }

  it should "not accept invalid ACCEPT header value" in {
    for (mimeType <- Seq("application/msword", "text/plain")) {
      checkResponseExactStatusCode(executeVariableWorkflow(inputOutputWorkflow, nonEmptyRequestParameters, acceptMimeType = mimeType), NOT_ACCEPTABLE)
    }
  }

  it should "execute valid workflows" in {
    for (validWorkflow <- validVariableWorkflows) {
      checkResponse(executeVariableWorkflow(validWorkflow, nonEmptyRequestParameters, acceptMimeType = "application/xml"))
    }
  }

  it should "return a 500 when the workflow execution fails" in {
    val response = checkResponseExactStatusCode(executeVariableWorkflow(brokenWorkflow, nonEmptyRequestParameters, acceptMimeType = "application/xml"), INTERNAL_ERROR)
    (response.json \ "title").asOpt[String] must not be empty
  }

  it should "return the correct response bodies given valid ACCEPT values" in {
    val ignoredMimeTypes = Seq("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", BinaryFileDataset.mimeType)
    for (mimeType <- VariableWorkflowRequestUtils.acceptedMimeType.filterNot(ignoredMimeTypes.contains)) {
      for (IndexedSeq(param1Values, param2Values) <- Seq(
        IndexedSeq(Seq("val 1"), Seq("val 2")),
        IndexedSeq(Seq(), Seq("val A", "val B"))
      )) {
        for (usePost <- Seq(false, true)) {
          val parameterMap = Map(
            sourceProperty1 -> param1Values,
            sourceProperty2 -> param2Values
          ).filter(_._2.nonEmpty)
          val response = checkResponseExactStatusCode(
            executeVariableWorkflow(inputOutputWorkflow, parameterMap, usePost, mimeType))
          checkForCorrectReturnType(mimeType, response.body, param1Values.isEmpty && param2Values.isEmpty)
          checkForValues(1, param1Values, response.body)
          checkForValues(2, param2Values, response.body)
        }
      }
    }
  }

  it should "return an error if no (query or form) parameters are specified for the input source" in {
    for (usePost <- Seq(false, true)) {
      checkResponseExactStatusCode(
        executeVariableWorkflow(inputOutputWorkflow, Map.empty, usePost), BAD_REQUEST)
    }
  }

  it should "return the correct response for an output only variable workflow" in {
    for (usePost <- Seq(true, false)) {
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
    for (usePost <- Seq(true, false)) {
      checkResponseExactStatusCode(
        executeVariableWorkflow(inputOnlyWorkflow, inputParams, usePost, APPLICATION_XML), NO_CONTENT)
      val outputCsvResource = project.resources.get(outputCsv)
      outputCsvResource.loadAsString().split("[\\r\\n]+") mustBe Seq("targetProp1,targetProp2", "input value A,XYZ")
      outputCsvResource.delete()
    }
  }

  it should "take the dataset content directly as POST payload" in {
    val inputValue = "some test value ä€"
    for (payload <- Seq(
      (s"""{"$sourceProperty1":"$inputValue"}""", APPLICATION_JSON),
      (s"""[{"$sourceProperty1":"$inputValue"}]""", APPLICATION_JSON),
      (
        s"""<?xml version='1.0' encoding='utf-8'?>
           |<Root><$sourceProperty1>$inputValue</$sourceProperty1></Root>""".stripMargin, APPLICATION_XML),
      (s"""$sourceProperty1\n$inputValue""", "text/comma-separated-values"),
      (s"""$sourceProperty1\n$inputValue""", "text/csv")
    )) {
      val response = checkResponseExactStatusCode(
        executeVariableWorkflow(inputOutputWorkflow, contentOpt = Some(payload)))
      checkForValues(1, Seq(inputValue), response.body)
    }
  }

  it should "support auto-config" in {
    // This payload will not work the way it is
    val csvPayLoad =
      s"""$sourceProperty1;$sourceProperty2
         |sourceVal1;sourceVal2
         |""".stripMargin
    val response = checkResponseExactStatusCode(
      executeVariableWorkflow(
        inputOutputWorkflow,
        contentOpt = Some((csvPayLoad, "text/csv")),
        acceptMimeType = APPLICATION_JSON,
        additionalQueryParameters = Map(VariableWorkflowRequestUtils.QUERY_CONFIG_PARAM_AUTO_CONFIG -> "true")
      ))
    for (i <- 1 to 2) {
      (response.json \\ s"targetProp$i").head.as[JsArray].value.map(_.as[String]) mustBe IndexedSeq(s"sourceVal$i")
    }
  }

  it should "return an error if a content-type is specified without valid content" in {
    for (mimeType <- Seq(APPLICATION_JSON, APPLICATION_XML, "text/csv")) {
      checkResponseExactStatusCode(
        executeVariableWorkflow(inputOutputWorkflow, contentOpt = Some(("", mimeType))),
        responseCode = BAD_REQUEST
      )
    }
  }

  it should "return the variable dataset info in the workflow info list" in {
    workflowList() map { wi =>
      (wi.label, wi.variableInputs, wi.variableOutputs)
    } sortBy (_._1) mustBe Seq(
      ("Broken Workflow", Seq(variableInputDataset), List(variableOutputDataset)),
      ("Workflow", List(variableInputDataset), List(variableOutputDataset)),
      ("Workflow input only", List(variableInputDataset), List()),
      ("Workflow multiple of the same input and output", List(variableInputDataset), List(variableOutputDataset)),
      ("Workflow output only", List(), List(variableOutputDataset)),
      ("Workflow too many sinks", List(variableInputDataset), List(variableOutputDataset, additionalVarDataset)),
      ("Workflow too many sources", List(variableInputDataset, additionalVarDataset), List(variableOutputDataset))
    )
  }

  it should "return the variable dataset info in the workflow info" in {
    workflowInfo(inputOutputWorkflow) mustBe WorkflowInfo(
      inputOutputWorkflow,
      "Workflow",
      projectId,
      "Simple variable workflow project",
      List(variableInputDataset),
      List(variableOutputDataset),
      Seq.empty
    )
  }

  it should "execute a workflow asynchronously" in {
    val startResponse = executeVariableWorkflowAsync(outputOnlyWorkflow, "xml")

    // Wait for completion
    val activityClient = new ActivityClient(baseUrl, projectId, outputOnlyWorkflow)
    activityClient.waitForActivity(startResponse.activityId, Some(startResponse.instanceId))

    // Retrieve result
    val response = checkResponseExactStatusCode(getVariableWorkflowResult(outputOnlyWorkflow, startResponse.instanceId, APPLICATION_XML))
    checkForCorrectReturnType(APPLICATION_XML, response.body, noValues = false)
    checkForValues(1, Seq("csv value 1"), response.body)
    checkForValues(2, Seq("csv value 2"), response.body)
  }

  it should "support running variable workflows with marked datasets" in {
    val transformTask = "b944ba5e-87b1-4511-8d67-02cb00da6baf_Transform"
    val inputDataset = "inputDataset1"
    val outputDataset = "outputDataset1"
    project.addTask[GenericDatasetSpec](inputDataset, DatasetSpec(InMemoryDataset()))
    project.addTask[GenericDatasetSpec](outputDataset, DatasetSpec(InMemoryDataset()))
    val workflow = Workflow(
      operators = Seq(
        WorkflowOperator(inputs = Seq(Some(inputDataset)), task = transformTask, outputs = Seq(outputDataset), Seq(), (0, 0), transformTask, None, Seq.empty, Seq.empty)
      ),
      datasets = Seq(
        WorkflowDataset(Seq(), inputDataset, Seq(transformTask), (0, 0), inputDataset, None, Seq.empty, Seq.empty),
        WorkflowDataset(Seq(Some(transformTask)), outputDataset, Seq(), (0, 0), outputDataset, None, Seq.empty, Seq.empty)
      ),
      uiAnnotations = UiAnnotations(),
      replaceableInputs = Seq(inputDataset),
      replaceableOutputs = Seq(outputDataset)
    )
    val workflowId = "newWorkflow"
    project.addTask[Workflow](workflowId, workflow)
    val inputValue = "some test value ä€"
    val response = checkResponseExactStatusCode(
      executeVariableWorkflow(workflowId, contentOpt = Some((s"""{"$sourceProperty1":"$inputValue"}""", APPLICATION_JSON))))
    checkForValues(1, Seq(inputValue), response.body)
  }

  it should "allow re-configuring the data source and sink parameters" in {
    val csvPayLoad =
      s"""$sourceProperty1;$sourceProperty2
         |sourceVal1;sourceVal2
         |""".stripMargin
    val response = checkResponseExactStatusCode(
      executeVariableWorkflow(
        inputOutputWorkflow,
        acceptMimeType = "text/csv",
        contentOpt = Some((csvPayLoad, "text/csv")),
        additionalQueryParameters = Map(
          s"${VariableWorkflowRequestUtils.QUERY_DATA_SOURCE_CONFIG_PREFIX}separator" -> ";",
          s"${VariableWorkflowRequestUtils.QUERY_DATA_SINK_CONFIG_PREFIX}separator" -> "|"
        )
      )
    )
    response.body must startWith (s"${targetProp(1)}|${targetProp(2)}")
    response.body must include ("sourceVal1|sourceVal2")
  }

  val (exampleFile, exampleResource): (File, FileResource) = {
    val file = File.createTempFile("inputFile", "dat")
    file.deleteOnExit()
    val resource = FileResource(file)
    resource.writeString(
      s"""<?xml version='1.0' encoding='utf-8'?>
         |<Root><$sourceProperty1>some value</$sourceProperty1></Root>""".stripMargin)
    (file, resource)
  }

  it should "support uploading files via multipart/form-data request" in {
    val response = checkResponseExactStatusCode(getVariableWorkflowResultMultiPart(inputOutputWorkflow, exampleFile))
    checkForCorrectReturnType(APPLICATION_XML, response.body, noValues = false)
    checkForValues(1, Seq("some value"), response.body)
  }

  it should "support arbitrary input and output datasets via custom mime type application/x-plugin-<PLUGIN_ID>" in {
    val response = checkResponseExactStatusCode(getVariableWorkflowResultMultiPart(inputOutputWorkflow, exampleFile,
      contentType = "application/x-plugin-xml", acceptMimeType = "application/x-plugin-xml"))
    checkForCorrectReturnType(APPLICATION_XML, response.body, noValues = false)
    checkForValues(1, Seq("some value"), response.body)
  }

  it should "remove temp files on GC" in {
    removeTempFiles()
    val workflowId = inputOutputWorkflow
    checkResponse(executeVariableWorkflow(workflowId, nonEmptyRequestParameters))
    eventually {
      new File(FileUtils.tempDir).listFiles() must not be empty
    }
    project.removeTask[Workflow](workflowId)
    val runtime = Runtime.getRuntime
    runtime.gc()
    eventually {
      new File(FileUtils.tempDir).listFiles() mustBe empty
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
      case "application/json" =>
        body must (
            startWith("[") or
                startWith("{")
            )
    }
  }

  private def workflowList(): Seq[WorkflowInfo] = {
    val workflowInfoListRequest = request(controllers.workflowApi.routes.WorkflowApi.workflowInfoList())
    JsonHelpers.fromJsonValidated[Seq[WorkflowInfo]](checkResponse(workflowInfoListRequest.get()).json)
  }

  private def workflowInfo(workflowId: String): WorkflowInfo = {
    val workflowInfoRequest = request(controllers.workflowApi.routes.WorkflowApi.workflowInfo(projectId, workflowId))
    JsonHelpers.fromJsonValidated[WorkflowInfo](checkResponse(workflowInfoRequest.get()).json)
  }

  // Executes a simple variable workflow
  private def executeVariableWorkflow(workflowId: String,
                                      parameters: Map[String, Seq[String]] = Map.empty,
                                      usePost: Boolean = false,
                                      acceptMimeType: String = "application/xml",
                                      contentOpt: Option[(String, String)] = None,
                                      additionalQueryParameters: Map[String, String] = Map.empty): Future[WSResponse] = {
    val path = {
      if(usePost) {
        controllers.workflowApi.routes.WorkflowApi.variableWorkflowResultPost(projectId, workflowId).url
      } else {
        controllers.workflowApi.routes.WorkflowApi.variableWorkflowResultGet(projectId, workflowId).url
      }
    }
    var request = client.url(s"$baseUrl$path")
        .addHttpHeaders(ACCEPT -> acceptMimeType)
    additionalQueryParameters.foreach { queryParam =>
      request = request.addQueryStringParameters(queryParam)
    }
    if(usePost || contentOpt.isDefined) {
      contentOpt match {
        case Some(content) =>
          request.addHttpHeaders(CONTENT_TYPE -> content._2).post(content._1)
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

  private def executeVariableWorkflowAsync(workflowId: String,
                                           datasetType: String,
                                           parameters: Map[String, Seq[String]] = Map.empty,
                                           contentOpt: Option[(String, String)] = None): StartActivityResponse = {
    val path =  controllers.workflowApi.routes.WorkflowApi.executeVariableWorkflowAsync(projectId, workflowId).url
    val request = client.url(s"$baseUrl$path")
                        .withQueryStringParameters((VariableWorkflowRequestUtils.QUERY_PARAM_OUTPUT_TYPE -> datasetType))
    val response =
      contentOpt match {
        case Some(content) =>
          request.addHttpHeaders(CONTENT_TYPE -> content._2).post(content._1)
        case None =>
          request.post(parameters)
      }
    JsonHelpers.fromJsonValidated[StartActivityResponse](checkResponse(response).json)
  }

  private def getVariableWorkflowResult(workflowId: String,
                                        instanceId: String,
                                        acceptMimeType: String = "application/xml"): Future[WSResponse] = {
    val path =  controllers.workflowApi.routes.WorkflowApi.variableWorkflowAsyncResult(projectId, workflowId, instanceId).url
    val request = client.url(s"$baseUrl$path").addHttpHeaders(ACCEPT -> acceptMimeType)
    request.get()
  }

  private def getVariableWorkflowResultMultiPart(workflowId: String,
                                                 file: File,
                                                 contentType: String = "application/xml",
                                                 acceptMimeType: String = "application/xml"): Future[WSResponse] = {
    val path = controllers.workflowApi.routes.WorkflowApi.variableWorkflowResultPost(projectId, workflowId).url
    val request = client.url(s"$baseUrl$path")
      .addHttpHeaders(ACCEPT -> acceptMimeType)
    request.post(Source(
      FilePart("hello", "shouldNotMatter.txt", Option(contentType), FileIO.fromPath(file.toPath)) :: Nil
    ))
  }

  override def afterAll(): Unit = {
    super.afterAll()
    FileUtils.toFileUtils(new File(FileUtils.tempDir)).deleteRecursiveOnExit()
  }

  private def removeTempFiles(): Unit = {
    FileUtils.toFileUtils(new File(FileUtils.tempDir)).deleteRecursive(true)
  }
}
