package helper

import java.io._
import java.net.URLDecoder

import org.scalatest.Suite
import org.scalatestplus.play.OneServerPerSuite
import org.silkframework.config.{PlainTask, Task}
import org.silkframework.dataset.rdf.{GraphStoreTrait, RdfNode}
import org.silkframework.rule.{DatasetSelection, MappingRules, RootMappingRule, TransformSpec}
import org.silkframework.runtime.activity.{TestUserContextTrait, UserContext}
import org.silkframework.runtime.serialization.XmlSerialization
import org.silkframework.util.StreamUtils
import org.silkframework.workspace._
import org.silkframework.workspace.activity.transform.{TransformPathsCache, VocabularyCache}
import org.silkframework.workspace.activity.workflow.Workflow
import play.api.Application
import play.api.http.Writeable
import play.api.libs.json._
import play.api.libs.ws.{WS, WSRequest, WSResponse}
import play.api.mvc.{Call, Results}
import play.api.test.FakeApplication

import scala.collection.immutable.SortedMap
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.util.Random
import scala.xml.{Elem, XML}

/**
  * Basis for integration tests.
  */
trait IntegrationTestTrait extends TaskApiClient with OneServerPerSuite with TestWorkspaceProviderTestTrait with TestUserContextTrait {
  this: Suite =>

  final val APPLICATION_JSON: String = "application/json"
  final val APPLICATION_XML: String = "application/xml"
  final val CONTENT_TYPE: String = "content-type"
  final val ACCEPT: String = "accept"
  final val NOT_FOUND: Int = 404
  final val BAD_REQUEST: Int = 400

  val baseUrl = s"http://localhost:$port"

  override lazy val port: Int = 19000 + Random.nextInt(1000)

  def workspaceProject(projectId: String)
                      (implicit userContext: UserContext): Project = WorkspaceFactory().workspace.project(projectId)

  /** Routes used for testing. If None, the default routes will be used.*/
  protected def routes: Option[String] = None

  override implicit lazy val app: Application = {
    val routerConf = routes.map(r => "play.http.router" -> r).toMap
    FakeApplication(additionalConfiguration = routerConf)
  }

  /**
    * Constructs a REST request for a given Call.
    */
  def request(call: Call): WSRequest = {
    WS.url(s"$baseUrl${call.url}")
  }

  /**
    * Creates a new project in the Silk workspace.
    *
    * @param projectId the id of the new project, see [[org.silkframework.util.Identifier]] for allowed characters.
    */
  def createProject(projectId: String): WSResponse = {
    val response = WS.url(s"$baseUrl/workspace/projects/$projectId").put("")
    checkResponse(response)
  }

  /**
    * Adds common prefixes to the project, so URIs can be written as qualified names.
    *
    * @param projectId
    */
  def addProjectPrefixes(projectId: String, extraPrefixes: Map[String, String] = Map.empty): WSResponse = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/prefixes")
    val response = request.put(Map(
      "rdf" -> Seq("http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
      "rdfs" -> Seq("http://www.w3.org/2000/01/rdf-schema#"),
      "owl" -> Seq("http://www.w3.org/2002/07/owl#"),
      "source" -> Seq("https://ns.eccenca.com/source/"),
      "loan" -> Seq("http://eccenca.com/ds/loans/"),
      "stat" -> Seq("http://eccenca.com/ds/unemployment/"),
      // TODO Currently the default mapping generator maps all properties to this namespace
      "target" -> Seq("https://ns.eccenca.com/"),
      // The CMEM integration test maps to these URIs, which result in the same URIs as the schema extraction
      "loans" -> Seq("http://eccenca.com/ds/loans/"),
      "unemployment" -> Seq("http://eccenca.com/ds/unemployment/")
    ) ++ extraPrefixes.mapValues(Seq(_)))
    checkResponse(response)
  }

  def listResources(projectId: String): WSResponse = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/resources")
    val response = request.get
    checkResponse(response)
  }

  /**
    * Uploads a file and creates a resource in the project.
    *
    * @param projectId
    * @param fileName
    * @param resourceDir The directory where the file is stored.
    */
  def uploadResource(projectId: String, fileName: String, resourceDir: String): WSResponse = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/resources/$fileName")
    val response = request.put(file(resourceDir + "/" + fileName))
    checkResponse(response)
  }

  def createEmptyResource(projectId: String, resourceId: String): WSResponse = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/resources/$resourceId")
    val response = request.put(Results.EmptyContent())
    checkResponse(response)
  }

  def resourceExists(projectId: String, resourceId: String): WSResponse = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/resources/$resourceId")
    val response = request.get
    checkResponse(response)
  }

  def getResourcesJson(projectId: String): String = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/resources")
    val response = request.get
    checkResponse(response).body
  }

  def getResource(projectId: String, resourceId: String): String = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/resources/$resourceId")
    val response = request.get
    checkResponse(response).body
  }

  def executeTaskActivity(projectId: String, taskId: String, activityId: String, parameters: Map[String, String]): WSResponse = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/tasks/$taskId/activities/$activityId/startBlocking")
    val response = request.post(parameters map { case (k, v) => (k, Seq(v)) })
    checkResponse(response)
  }

  def taskActivityValue(projectId: String, taskId: String, activityId: String, contentType: String = "application/json"): WSResponse = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/tasks/$taskId/activities/$activityId/value")
                    .withHeaders("accept" -> contentType)
    val response = request.get()
    checkResponse(response)
  }

  /**
    * Creates a CSV dataset from a file resources.
    *
    * @param projectId
    * @param datasetId
    * @param fileResourceId
    */
  def createCsvFileDataset(projectId: String, datasetId: String, fileResourceId: String,
                           uriTemplate: Option[String] = None): WSResponse = {
    val datasetConfig =
      <Dataset id={datasetId} type="csv">
        <Param name="file" value={fileResourceId}/>
      </Dataset>
    createDataset(projectId, datasetId, datasetConfig)
  }

  def createRdfDumpDataset(projectId: String, datasetId: String, fileResourceId: String, format: String = "N-Triples", graph: String = ""): WSResponse = {
    val datasetConfig =
      <Dataset id={datasetId} type="file">
        <Param name="file" value={fileResourceId}/>
        <Param name="format" value={format}/>
        <Param name="graph" value={graph}/>
      </Dataset>
    createDataset(projectId, datasetId, datasetConfig)
  }

  def createSparkViewDataset(projectId: String, datasetId: String, viewName: String): WSResponse = {
    val datasetConfig =
      <Dataset id={datasetId} type="sparkView">
        <Param name="viewName" value={viewName}/>
      </Dataset>
    createDataset(projectId, datasetId, datasetConfig)
  }

  /** Loads the given RDF input stream into the specified graph of the RDF store of the workspace, i.e. this only works if the workspace provider
    * is RDF-enabled and implements the [[GraphStoreTrait]]. */
  def loadRdfIntoGraph(graph: String, contentType: String = "application/n-triples"): OutputStream = {
    WorkspaceFactory.factory.workspace.provider match {
      case rdfStore: RdfWorkspaceProvider if rdfStore.endpoint.isInstanceOf[GraphStoreTrait] =>
        val graphStore = rdfStore.endpoint.asInstanceOf[GraphStoreTrait]
        graphStore.postDataToGraph(graph, contentType)
      case e: Any =>
        fail(s"Not a RDF-enabled GraphStore supporting workspace provider (${e.getClass.getSimpleName})!")
    }
  }

  def loadRdfAsStringIntoGraph(rdfString: String, graph: String, contentType: String = "application/n-triples"): Unit = {
    val out = loadRdfIntoGraph(graph, contentType)
    val outWriter = new BufferedOutputStream(out)
    try {
      outWriter.write(rdfString.getBytes("UTF8"))
    } finally {
      outWriter.close()
    }
  }

  def loadRdfAsInputStreamIntoGraph(input: InputStream, graph: String, contentType: String = "application/n-triples"): Unit = {
    val out = loadRdfIntoGraph(graph, contentType)
    StreamUtils.fastStreamCopy(input, out, close = true)
  }

  def createXmlDataset(projectId: String, datasetId: String, fileResourceId: String): WSResponse = {
    val datasetConfig =
      <Dataset id={datasetId} type="xml">
        <Param name="file" value={fileResourceId}/>
        <Param name="basePath" value=""/>
        <Param name="uriPattern" value="http://id/{#id}"/>
      </Dataset>
    createDataset(projectId, datasetId, datasetConfig)
  }

  def createVariableDataset(projectId: String, datasetId: String): WSResponse = {
    val datasetConfig = <Dataset id={datasetId} type="variableDataset"/>
    createDataset(projectId, datasetId, datasetConfig)
  }

  /**
    * Executes schema extraction and profiling on a dataset. Attaches the schema and profiling data to
    * the resource with datasetUri as URI.
    *
    * @param projectId
    * @param datasetId
    * @param datasetUri The dataset URI. This is an external URI outside of Silk and is not used inside Silk. The calling
    *                   application must supply a meaningful resource URI.
    * @return
    */
  def executeSchemaExtractionAndDataProfiling(projectId: String,
                                              datasetId: String,
                                              datasetUri: String,
                                              uriPrefix: String,
                                              classProfilingLimit: Int = 10): WSResponse = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/tasks/$datasetId/activities/DatasetProfiler/startBlocking")
    val response = request.post(Map(
      "datasetUri" -> Seq(datasetUri),
      "uriPrefix" -> Seq(uriPrefix),
      "classProfilingLimit" -> Seq(classProfilingLimit.toString)
    ))
    checkResponse(response)
  }

  def autoConfigureDataset(projectId: String, datasetId: String): WSResponse = {
    val datasetConfigAutomatic = getAutoConfiguredDataset(projectId, datasetId)
    createDataset(projectId, datasetId, datasetConfigAutomatic)
  }

  def getAutoConfiguredDataset(projectId: String, datasetId: String): Elem = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/datasets/$datasetId/autoConfigured").
        withHeaders("accept" -> "application/xml")
    val response = request.get()
    XML.loadString(checkResponse(response).body)
  }

  def createDataset(projectId: String, datasetId: String, datasetConfig: Elem): WSResponse = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/datasets/$datasetId")
    val response = request.put(datasetConfig)
    checkResponse(response)
  }

  def getDatasetConfig(projectId: String, datasetId: String): Elem = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/datasets/$datasetId").
        withHeaders("accept" -> "application/xml")
    val response = request.get()
    XML.loadString(checkResponse(response).body)
  }

  def peakIntoDatasetTransformation(projectId: String, transformationId: String, ruleId: String): String = {
    val request = WS.url(s"$baseUrl/transform/tasks/$projectId/$transformationId/peak/$ruleId")
    val response = request.post("")
    val result = checkResponse(response)
    result.body
  }

  /**
    * Executes dataset matching based on the profiling data.
    *
    * @param projectId
    * @param datasetUri The dataset URI. This is an external URI outside of Silk and is not used inside Silk. The calling
    *                   application must supply a meaningful resource URI.
    * @return
    */
  def executeDatasetMatching(projectId: String, datasetUri: String): WSResponse = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/activities/DatasetMatcher/startBlocking")
    val response = request.post(Map(
      "datasetUri" -> Seq(datasetUri)
    ))
    checkResponse(response)
  }

  /**
    * Generate a default mapping for a dataset.
    *
    * @param projectId
    * @param datasetId
    * @param propertyUris A sequence of URIs to select the properties for the default mapping.
    */
  def createDefaultMapping(projectId: String,
                           datasetId: String,
                           uriPrefix: String,
                           propertyUris: Traversable[String] = Seq()): WSResponse = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/tasks/$datasetId/activities/DefaultMappingGenerator/startBlocking")
    val response = request.post(Map(
      "pathSelection" -> Seq(propertyUris.mkString(" ")),
      "uriPrefix" -> Seq(uriPrefix)
    ))
    checkResponse(response)
  }

  def createTask(projectId: String, taskId: String, taskJson: JsValue): WSResponse = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/tasks/$taskId")
    val response = request.put(taskJson)
    checkResponse(response)
  }

  /**
    * Create a linking task.
    *
    * @param projectId
    * @param linkingTaskId id of the new linking task
    * @param sourceId
    * @param targetId
    * @param outputDatasetId
    */
  def createLinkingTask(projectId: String, linkingTaskId: String, sourceId: String, targetId: String, outputDatasetId: String): WSResponse = {
    val request = WS.url(s"$baseUrl/linking/tasks/$projectId/$linkingTaskId")
    val response = request.withQueryString("source" -> sourceId, "target" -> targetId, "output" -> outputDatasetId).put("")
    checkResponse(response)
  }

  /**
    * Create an empty transform task.
    * @param projectId
    * @param transformTaskId ID of the transform task to create.
    * @param sourceId
    * @param targetId
    * @return
    */
  def createTransformTask(projectId: String, transformTaskId: String, sourceId: String, targetId: String, classUri: String = ""): Unit = {
    val request = WS.url(s"$baseUrl/transform/tasks/$projectId/$transformTaskId")
    val response = request.put(Map("source" -> sourceId, "sourceType" -> classUri, "target" -> targetId, "output" -> targetId).mapValues(v => Seq(v)))
    checkResponse(response)
    workspaceProject(projectId).task[TransformSpec](transformTaskId).activity[TransformPathsCache].control.waitUntilFinished()
  }

  /** Updates the root mapping rule with new mapping rules */
  def updateTransformRules(projectId: String, transformTaskId: String, mappingRules: MappingRules): Unit = {
    val oldTransformSpec = workspaceProject(projectId).task[TransformSpec](transformTaskId).data
    val newTransformTask = oldTransformSpec.copy(mappingRule = oldTransformSpec.mappingRule.copy(rules = mappingRules))
    workspaceProject(projectId).updateTask(transformTaskId, newTransformTask)
  }

  /**
    * Sets the linking rule of an existing linking task.
    *
    * @param projectId
    * @param linkingTaskId
    * @param ruleXml
    */
  def setLinkingRule(projectId: String, linkingTaskId: String, ruleXml: Elem): WSResponse = {
    val request = WS.url(s"$baseUrl/linking/tasks/$projectId/$linkingTaskId/rule")
    val response = request.put(ruleXml)
    checkResponse(response)
  }

  /**
    * Executes a transform task. This is a blocking request.
    */
  def executeTransformTask(projectId: String, transformTaskId: String, parameters: Map[String, String] = Map.empty): WSResponse = {
    var request = WS.url(s"$baseUrl/workspace/projects/$projectId/tasks/$transformTaskId/activities/ExecuteTransform/startBlocking")
    if(parameters.nonEmpty) {
      request = request.withQueryString(parameters.toSeq: _*)
    }
    val response = request.post("")
    checkResponse(response)
  }

  /**
    * Downloads the task output.
    */
  def downloadTaskOutput(projectId: String, taskId: String): WSResponse = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/tasks/$taskId/downloadOutput")
    val response = request.get()
    checkResponse(response)
  }

  /**
    * Executes the linking task. This is a blocking request.
    *
    * @param projectId
    * @param linkingTaskId
    */
  def executeLinkingTask(projectId: String, linkingTaskId: String): WSResponse = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/tasks/$linkingTaskId/activities/ExecuteLinking/startBlocking")
    val response = request.post("")
    checkResponse(response)
  }

  def evaluateLinkingTask(projectId: String, linkingTaskId: String): WSResponse = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/tasks/$linkingTaskId/activities/EvaluateLinking/startBlocking")
    val response = request.post("")
    checkResponse(response)
  }

  def executeWorkflow(projectId: String, workflowId: String, sparkExecution: Boolean = false): WSResponse = {
    val executorName = if(sparkExecution) "ExecuteSparkWorkflow" else "ExecuteLocalWorkflow"
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/tasks/$workflowId/activities/$executorName/startBlocking")
    val response = request.post("")
    checkResponse(response)
  }

  def activitiesLog(): WSResponse = {
    val request = WS.url(s"$baseUrl/workspace/activities/log")
    val response = request.get()
    checkResponse(response)
  }

  /**
    * Retrieves a file from the resources directory.
    */
  def file(path: String): File = {
    new File(URLDecoder.decode(getClass.getClassLoader.getResource(path).getFile, "UTF-8"))
  }

  def checkResponseCode(futureResponse: Future[WSResponse],
                        responseCode: Int = 200): WSResponse = {
    val response = Await.result(futureResponse, 100.seconds)
    assert(response.status == responseCode, s"Expected $responseCode response code. " +
        s"Found: status text: ${response.statusText}. Response Body: ${response.body}")
    response
  }

  def getTransformationTaskRules(project: String, taskName: String, accept: String = "application/json"): String = {
    val request = WS.url(s"$baseUrl/transform/tasks/$project/$taskName/rules")
    val response = request.
        withHeaders("accept" -> accept).
        get()
    val r = checkResponse(response)
    r.body
  }

  def getVariableValues(variableName: String,
                        results: Traversable[SortedMap[String, RdfNode]]): Traversable[String] = {
    results.map(_(variableName).value)
  }

  def createWorkflow(projectId: String, workflowId: String, workflow: Workflow): WSResponse = {
    val workflowConfig = XmlSerialization.toXml[Task[Workflow]](PlainTask(workflowId, workflow))
    val request = WS.url(s"$baseUrl/workflow/workflows/$projectId/$workflowId")
    val response = request.put(workflowConfig)
    checkResponse(response)
  }

  def resourceAsSource(resourceClassPath: String): Source = Source.createBufferedSource(getClass.getClassLoader.getResourceAsStream(resourceClassPath))

  def reloadVocabularyCache(project: Project, transformTaskId: String)
                           (implicit userContext: UserContext): Unit = {
    val control = project.task[TransformSpec](transformTaskId).activity[VocabularyCache].control
    control.reset()
    control.startBlocking()
  }

  def waitForCaches(task: String, project: String): Unit = {
    var cachesLoaded = false
    do {
      val request = WS.url(s"$baseUrl/workspace/projects/$project/tasks/$task/cachesLoaded")
      val response = request.get()
      val responseJson = checkResponse(response).json
      cachesLoaded = responseJson.as[JsBoolean].value
      if(!cachesLoaded)
        Thread.sleep(1000)
    } while(!cachesLoaded)
  }
}