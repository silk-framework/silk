package helper

import java.io._
import java.net.{BindException, InetSocketAddress, URLDecoder}

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import org.scalatest.{BeforeAndAfterAll, Suite}
import org.scalatestplus.play.OneServerPerSuite
import org.silkframework.config.{PlainTask, Prefixes, Task}
import org.silkframework.dataset.rdf.{GraphStoreTrait, RdfNode}
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.runtime.serialization.XmlSerialization
import org.silkframework.workspace.activity.workflow.Workflow
import org.silkframework.workspace.resources.FileRepository
import org.silkframework.workspace.{RdfWorkspaceProvider, User, Workspace, WorkspaceProvider}
import play.api.libs.ws.{WS, WSResponse}

import scala.collection.immutable.SortedMap
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.util.{Failure, Success, Try}
import scala.xml.{Elem, NodeSeq, Null, XML}

/**
  * Created on 3/17/16.
  */
trait IntegrationTestTrait extends OneServerPerSuite with BeforeAndAfterAll {
  this: Suite =>

  final val APPLICATION_JSON: String = "application/json"
  final val APPLICATION_XML: String = "application/xml"
  final val CONTENT_TYPE: String = "content-type"

  val baseUrl = s"http://localhost:$port"
  var oldUserManager: () => User = null
  final val START_PORT = 10600
  private val tmpDir = File.createTempFile("di-resource-repository", "-tmp")
  tmpDir.delete()
  tmpDir.mkdirs()

  /** The workspace provider that is used for holding the test workspace. */
  def workspaceProvider: String = "inMemoryRdfWorkspace"

  def deleteRecursively(f: File): Unit = {
    if (f.isDirectory) {
      for (c <- f.listFiles())
        deleteRecursively(c)
    }
    if (!f.delete()) {
      throw new FileNotFoundException("Failed to delete file: " + f)
    }
  }

  // Workaround for config problem, this should make sure that the workspace is a fresh in-memory RDF workspace
  override protected def beforeAll(): Unit = {
    implicit val resourceManager = InMemoryResourceManager()
    implicit val prefixes = Prefixes.empty
    val provider = PluginRegistry.create[WorkspaceProvider](workspaceProvider, Map.empty)
    val replacementWorkspace = new Workspace(provider, FileRepository(tmpDir.getAbsolutePath))
    val rdfWorkspaceUser = new User {
      /**
        * The current workspace of this user.
        */
      override def workspace: Workspace = replacementWorkspace
    }
    oldUserManager = User.userManager
    User.userManager = () => rdfWorkspaceUser
  }

  override protected def afterAll(): Unit = {
    User.userManager = oldUserManager
    deleteRecursively(tmpDir)
  }

  def init(): Unit = {
    //    System.setProperty("workspace.provider.plugin", "inMemoryRdfWorkspace")
    //    System.setProperty("workspace.provider.plugin", "file")
    //    System.setProperty("workspace.provider.file.dir", "testWorkspace")
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

    val response = request.put("")
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

  /**
    * Creates a CSV dataset from a file resources.
    *
    * @param projectId
    * @param datasetId
    * @param fileResourceId
    * @param uriPrefix The prefix that is prepended to automatically generated URIs like property URIs generated from
    *                  the header line.
    */
  def createCsvFileDataset(projectId: String, datasetId: String, fileResourceId: String,
                           uriPrefix: String, uriTemplate: Option[String] = None): WSResponse = {
    val datasetConfig =
      <Dataset id={datasetId} type="csv">
        <Param name="file" value={fileResourceId}/>
        <Param name="prefix" value={uriPrefix}/>{uriTemplate.map(uri => <Param name="uri" value={uri}/>).getOrElse(NodeSeq.Empty)}
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
    User.userManager.apply().workspace.provider match {
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
    outWriter.write(rdfString.getBytes())
    outWriter.flush()
    outWriter.close()
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
  def executeSchemaExtractionAndDataProfiling(projectId: String, datasetId: String, datasetUri: String, uriPrefix: String): WSResponse = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/tasks/$datasetId/activities/DatasetProfiler/startBlocking")
    val response = request.post(Map(
      "datasetUri" -> Seq(datasetUri),
      "uriPrefix" -> Seq(uriPrefix)
    ))
    checkResponse(response)
  }

  def autoConfigureDataset(projectId: String, datasetId: String): WSResponse = {
    val datasetConfigAutomatic = getAutoConfiguredDataset(projectId, datasetId)
    createDataset(projectId, datasetId, datasetConfigAutomatic)
  }

  def getAutoConfiguredDataset(projectId: String, datasetId: String): Elem = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/datasets/$datasetId/autoConfigured")
    val response = request.get()
    XML.loadString(checkResponse(response).body)
  }

  def createDataset(projectId: String, datasetId: String, datasetConfig: Elem): WSResponse = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/datasets/$datasetId")
    val response = request.put(datasetConfig)
    checkResponse(response)
  }

  def getDatasetConfig(projectId: String, datasetId: String): Elem = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/datasets/$datasetId")
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
    * @param uriPrefix    The URI prefix for the generated target property URIs of the mapping. This should be the same
    *                     as the prefix used for the schema extraction.
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
    * Executes the linking task. This is a blocking request.
    *
    * @param projectId
    * @param linkingTaskId
    */
  def executeLinkingTask(projectId: String, linkingTaskId: String): WSResponse = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/tasks/$linkingTaskId/activities/GenerateLinks/startBlocking")
    val response = request.post("")
    checkResponse(response)
  }

  def executeWorkflow(projectId: String, workflowId: String): WSResponse = {
    val request = WS.url(s"$baseUrl/workspace/projects/$projectId/tasks/$workflowId/activities/ExecuteLocalWorkflow/startBlocking")
    val response = request.post("")
    checkResponse(response)
  }

  def activitiesLog(): WSResponse = {
    val request = WS.url(s"$baseUrl/workspace/activities/log")
    val response = request.get()
    checkResponse(response)
  }

  def executeVariableWorkflow(projectId: String, workflowId: String, datasetPayloads: Seq[VariableDatasetPayload]): WSResponse = {
    val requestXML = {
      <Workflow>
        <DataSources>
          {datasetPayloads.filterNot(_.isSink).map(_.datasetXml)}
        </DataSources>
        <Sinks>
          {datasetPayloads.filter(_.isSink).map(_.datasetXml)}
        </Sinks>{datasetPayloads.map(_.resourceXml)}
      </Workflow>
    }
    executeVariableWorkflow(projectId, workflowId, requestXML)
  }

  def executeVariableWorkflow(projectId: String, workflowId: String, requestXML: Elem): WSResponse = {
    val request = WS.url(s"$baseUrl/workflow/workflows/$projectId/$workflowId/executeOnPayload")
    val response = request.post(requestXML)
    checkResponse(response)
  }

  def executeVariableWorkflowLocalExecutor(projectId: String, workflowId: String, datasetPayloads: Seq[VariableDatasetPayload]): WSResponse = {
    val requestXML = {
      <Workflow>
        <DataSources>
          {datasetPayloads.filterNot(_.isSink).map(_.datasetXml)}
        </DataSources>
        <Sinks>
          {datasetPayloads.filter(_.isSink).map(_.datasetXml)}
        </Sinks>{datasetPayloads.map(_.resourceXml)}
      </Workflow>
    }
    executeVariableWorkflowLocalExecutor(projectId, workflowId, requestXML)
  }

  def executeVariableWorkflowLocalExecutor(projectId: String, workflowId: String, xmlBody: Elem): WSResponse = {
    val request = WS.url(s"$baseUrl/workflow/workflows/$projectId/$workflowId/executeOnPayload")
    val response = request.post(xmlBody)
    checkResponse(response)
  }

  /**
    * Retrieves a file from the resources directory.
    */
  def file(path: String) = {
    new File(URLDecoder.decode(getClass.getClassLoader.getResource(path).getFile, "UTF-8"))
  }

  def checkResponse(futureResponse: Future[WSResponse],
                    responseCodePrefix: Char = '2'): WSResponse = {
    val response = Await.result(futureResponse, 100.seconds)
    assert(response.status.toString.head + "xx" == responseCodePrefix + "xx", s"Status text: ${response.statusText}. Response Body: ${response.body}")
    response
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
    results.map(_.get(variableName).get.value)
  }

  def createWorkflow(projectId: String, workflowId: String, workflow: Workflow): WSResponse = {
    val workflowConfig = XmlSerialization.toXml[Task[Workflow]](PlainTask(workflowId, workflow))
    val request = WS.url(s"$baseUrl/workflow/workflows/$projectId/$workflowId")
    val response = request.put(workflowConfig)
    checkResponse(response)
  }

  def resourceAsSource(resourceClassPath: String): Source = Source.createBufferedSource(getClass.getClassLoader.getResourceAsStream(resourceClassPath))

  case class VariableDatasetPayload(datasetId: String,
                                    datasetPluginType: String,
                                    pluginParams: Map[String, String],
                                    payLoadOpt: Option[String],
                                    isSink: Boolean) {
    lazy val fileResourceId = datasetId + "_file_resource"

    lazy val datasetXml: Elem = {
      <Dataset id={datasetId}>
        <DatasetPlugin type={datasetPluginType}>
          {for ((key, value) <- pluginParams.filter(_._1 != "file") ++ Map("file" -> fileResourceId)) yield {
            <Param name={key} value={value}/>
        }}
        </DatasetPlugin>
      </Dataset>
    }

    lazy val resourceXml = {
      payLoadOpt match {
        case Some(payload) =>
          <resource name={fileResourceId}>
            {payload}
          </resource>
        case None =>
          Null
      }
    }
  }

  // From https://stackoverflow.com/questions/3732109/simple-http-server-in-java-using-only-java-se-api
  def withAdditionalServer(servedContent: Traversable[ServedContent])(withPort: Int => Unit): Unit = {
    val server: HttpServer = createHttpServer
    for (responseContent <- servedContent) {
      val handler = new HttpHandler {
        override def handle(httpExchange: HttpExchange): Unit = {
          val response = responseContent.content
          val responseHeaders = httpExchange.getResponseHeaders
          responseHeaders.add("content-type", responseContent.contentType)
          httpExchange.sendResponseHeaders(200, response.getBytes("UTF-8").length)
          val os = httpExchange.getResponseBody()
          os.write(response.getBytes("UTF-8"))
          os.close()
        }
      }
      server.createContext(responseContent.contextPath, handler)
    }
    server.setExecutor(null) // creates a default executor
    server.start()
    withPort(server.getAddress.getPort)
  }

  private def createHttpServer: HttpServer = {
    var port = START_PORT
    var serverOpt: Option[HttpServer] = None
    while (serverOpt.isEmpty) {
      Try(HttpServer.create(new InetSocketAddress(port), 0)) match {
        case Success(s) =>
          serverOpt = Some(s)
        case Failure(e: BindException) =>
          port += 1
        case Failure(e) =>
          throw e
      }
    }
    serverOpt.get
  }
}

case class ServedContent(contextPath: String, content: String, contentType: String)