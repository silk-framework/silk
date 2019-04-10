package controllers.workspace

import java.io._
import java.nio.charset.StandardCharsets

import helper.IntegrationTestTrait
import org.asynchttpclient.{AsyncCompletionHandler, AsyncHttpClient, Request, Response}
import org.asynchttpclient.request.body.multipart.FilePart
import org.scalatestplus.play.PlaySpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource._
import org.silkframework.workspace.WorkspaceFactory
import play.api.libs.ws.ahc.AhcWSResponse
import play.api.libs.ws.{WS, WSResponse}

import scala.concurrent.{Future, Promise}
import scala.util.Try

class ProjectMarshalingApiTest extends PlaySpec with IntegrationTestTrait {

  protected override def routes = Some("workspace.Routes")

  override def workspaceProvider: String = "inMemory"

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
    val asyncHttpClient: AsyncHttpClient = WS.client.underlying
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
    val request = WS.url(s"$baseUrl/import/xmlZip")
    val response = request.post(workspaceBytes)
    checkResponse(response)
  }

  private def exportWorkspace(): Array[Byte] = {
    val request = WS.url(s"$baseUrl/export/xmlZip")
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
        val wsResponse = AhcWSResponse(response)
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
}
