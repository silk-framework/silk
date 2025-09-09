package controllers.workspace

import helper.{IntegrationTestTrait, RequestFailedException}
import org.scalatestplus.play.PlaySpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource._
import org.silkframework.runtime.validation.RequestException
import org.silkframework.util.Identifier
import org.silkframework.workspace.WorkspaceFactory
import org.silkframework.workspace.resources.ResourceRepository
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ahc.AhcWSResponse
import play.shaded.ahc.org.asynchttpclient.request.body.multipart.FilePart
import play.shaded.ahc.org.asynchttpclient.{AsyncCompletionHandler, AsyncHttpClient, Request, Response}

import java.io._
import java.nio.charset.StandardCharsets
import scala.concurrent.{Future, Promise}
import scala.util.Try

class ProjectMarshalingApiTest extends PlaySpec with IntegrationTestTrait {

  protected override def routes = Some(classOf[workspace.Routes])

  /** Accessing resources with these names will fail during the tests. */
  @volatile
  private var failOnResources: Set[String] = Set.empty

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  override def createResourceRepository(dir: File): ResourceRepository = {
    new TestResourceRepository(super.createResourceRepository(dir))
  }

  "import the entire workspace" in {
    val workspaceBytes = ClasspathResource("controllers/workspace/workspace.zip").loadAsBytes
    importWorkspace(workspaceBytes)

    WorkspaceFactory().workspace.projects.map(_.config.id).toSet mustBe Set("example", "movies")
  }

  "export the entire workspace" in {
    val exportedWorkspace = exportWorkspace()
    clearWorkspace()
    importWorkspace(exportedWorkspace)

    WorkspaceFactory().workspace.projects.map(_.config.id).toSet mustBe Set("example", "movies")
  }

  "fail to export if a project file cannot be accessed" in {
    val workspaceBytes = ClasspathResource("controllers/workspace/workspace.zip").loadAsBytes
    importWorkspace(workspaceBytes)

    failOnResources = Set("source.nt")

    val exception = the[RequestFailedException] thrownBy exportProject("example")
    (exception.response.json \ "title").as[String] mustBe CannotAccessResourceException.errorTitle

    val exception2 = the[RequestFailedException] thrownBy exportWorkspace()
    (exception2.response.json \ "title").as[String] mustBe CannotAccessResourceException.errorTitle
  }

  "import single project workspace as project" in {
    val projectId = "singleWorkspaceProject"
    val projectZipBytes = ClasspathResource("controllers/workspace/singleProjectWorkspace.zip").loadAsBytes
    importProject(projectId, projectZipBytes)

    WorkspaceFactory().workspace.projects.map(_.config.id).toSet must contain (projectId)
  }

  "throw error if no project is found" in {
    val projectId = "nonProject"
    val projectZipBytes = ClasspathResource("controllers/workspace/nonProject.zip").loadAsBytes
    importProject(projectId, projectZipBytes, expectedResponseCodePrefix = '4')

    WorkspaceFactory().workspace.projects.map(_.config.id).toSet must not contain projectId
  }

  private def importProject(projectId: String, xmlZipInputBytes: Array[Byte], expectedResponseCodePrefix: Char = '2'): Unit = {
    val asyncHttpClient = client.underlying[AsyncHttpClient]
    var postBuilder = asyncHttpClient.preparePost(s"$baseUrl/projects/$projectId/import")
    val tempFile = File.createTempFile("di_file_upload", ".zip")
    try {
      tempFile.deleteOnExit()
      val os = new FileOutputStream(tempFile)
      try {
        os.write(xmlZipInputBytes)
      } finally {
        os.flush()
        os.close()
      }
      postBuilder = postBuilder.addBodyPart(new FilePart("file", tempFile, "application/octet-stream", StandardCharsets.UTF_8))
      val request = postBuilder.build()
      val response = executeAsyncRequest(asyncHttpClient, request, () => tempFile.delete())
      checkResponse(response, expectedResponseCodePrefix)
    } catch {
      case e: IOException =>
        Try(tempFile.delete())
        throw e
    }
  }

  private def importWorkspace(workspaceBytes: Array[Byte]): Unit = {
    val request = client.url(s"$baseUrl/import/xmlZip")
    val response = request.post(workspaceBytes)
    checkResponse(response)
  }

  private def exportProject(project: String): Array[Byte] = {
    val request = client.url(s"$baseUrl/projects/$project/export/xmlZip")
    val response = request.get()
    val result = checkResponse(response)
    result.bodyAsBytes.toArray
  }

  private def exportWorkspace(): Array[Byte] = {
    val request = client.url(s"$baseUrl/export/xmlZip")
    val response = request.get()
    val result = checkResponse(response)
    result.bodyAsBytes.toArray
  }

  private def clearWorkspace()
                            (implicit userContext: UserContext): Unit = {
    WorkspaceFactory().workspace.clear()
    WorkspaceFactory().workspace.projects.map(_.config.id).toSet mustBe Set.empty
  }

  private def executeAsyncRequest(asyncHttpClient: AsyncHttpClient,
                                  request: Request,
                                  // To run when the request completed or failed
                                  postProcessing: () => Unit): Future[WSResponse] = {
    val result = Promise[WSResponse]()
    asyncHttpClient.executeRequest(request, new AsyncCompletionHandler[WSResponse]() {
      override def onCompleted(response: Response) = {
        val wsResponse = new AhcWSResponse(response)
        result.success(wsResponse)
        postProcessing()
        wsResponse
      }

      override def onThrowable(t: Throwable) = {
        postProcessing()
        result.failure(t)
      }
    })
    result.future
  }

  class TestResourceRepository(base: ResourceRepository) extends ResourceRepository {
    override def get(project: Identifier): ResourceManager = new TestResourceManager(base.get(project))
    override def sharedResources: Boolean = base.sharedResources
    override def removeProjectResources(project: Identifier): Unit = base.removeProjectResources(project)
  }

  class TestResourceManager(base: ResourceManager) extends ResourceManager {
    override def get(name: String, mustExist: Boolean): WritableResource = {
      if(failOnResources.contains(name)) {
        throw CannotAccessResourceException
      }
      base.get(name, mustExist)
    }
    override def child(name: String): ResourceManager = base.child(name)
    override def parent: Option[ResourceManager] = base.parent
    override def basePath: String = base.basePath
    override def list: List[String] = base.list
    override def listChildren: List[String] = base.listChildren
    override def delete(name: String): Unit = base.delete(name)
  }

  object CannotAccessResourceException extends RequestException("test message", None) {
    override def errorTitle: String = "Cannot access resource"
    override def httpErrorCode: Option[Int] = None
  }
}
