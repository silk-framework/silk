package controllers.workspace

import java.io.{ByteArrayInputStream, InputStream}
import java.util.UUID
import java.util.zip.ZipInputStream

import helper.IntegrationTestTrait
import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.util.Identifier
import org.silkframework.workspace._
import play.api.libs.ws.WS

import scala.reflect.ClassTag
import scala.util.Try

/**
  * Tests the workspace and project export features.
  */
class WorkspaceExportIntegrationTest extends FlatSpec with IntegrationTestTrait
    with SingleProjectWorkspaceProviderTestTrait with MustMatchers {
  override def projectPathInClasspath: String = "controllers/workspace/miniProject.zip"

  override def workspaceProvider: String = "mockableInMemoryWorkspace"

  it should "export project with broken workspace provider" in {
    makeWorkspaceFail()
    val projectExportUri = baseUrl + s"/workspace/projects/$projectId/export/xmlZip"
    val responseBody = checkResponse(WS.url(projectExportUri).get()).bodyAsBytes
    checkZipEntries(responseBody, Seq(
      "singleProject/workflow/Workflow.xml",
      "singleProject/linking/miniLinking/alignment.xml",
      "singleProject/linking/miniLinking/linkSpec.xml",
      "singleProject/transform/miniTransform/rules.xml",
      "singleProject/transform/miniTransform/dataset.xml",
      "singleProject/dataset/miniCsv.xml",
      "singleProject/dataset/internalDataset.xml",
      "singleProject/resources/miniResource.csv",
      "singleProject/config.xml"))
  }

  it should "export workspace with broken workspace provider" in {
    makeWorkspaceFail()
    val workspaceExportURI = baseUrl + "/workspace/export/xmlZip"
    val responseBody = checkResponse(WS.url(workspaceExportURI).get()).bodyAsBytes
    checkZipEntries(responseBody, Seq(
      "singleProject/workflow/Workflow.xml",
      "singleProject/linking/miniLinking/alignment.xml",
      "singleProject/linking/miniLinking/linkSpec.xml",
      "singleProject/transform/miniTransform/rules.xml",
      "singleProject/transform/miniTransform/dataset.xml",
      "singleProject/dataset/miniCsv.xml",
      "singleProject/dataset/internalDataset.xml",
      "singleProject/resources/miniResource.csv",
      "singleProject/config.xml"
    ))
  }

  private lazy val workspace: Workspace = WorkspaceFactory().workspace

  private lazy val provider: WorkspaceProvider = workspace.provider

  private def workspaceId: UUID = provider.asInstanceOf[MockableWorkspaceProvider].id

  override def routes: Option[String] = Some("test.Routes")

  private def checkZipEntries(responseBody: Array[Byte], expectedFiles: Seq[String]): Unit = {
    val entries = zipEntries(responseBody)
    entries.map(_.name) mustBe expectedFiles
    entries.foreach(_.size must be > 10) // Check for 'No empty files'
  }

  private def zipEntries(responseBytes: Array[Byte]): Seq[ZipEntry] = {
    val zip = new ZipInputStream(new  ByteArrayInputStream(responseBytes))
    var results = List.empty[ZipEntry]
    var nextEntry = zip.getNextEntry
    while(nextEntry != null) {
      results ::= ZipEntry(nextEntry.getName, streamSize(zip))
      nextEntry = zip.getNextEntry
    }
    results
  }

  private def streamSize(is: InputStream): Int = {
    var input = is.read()
    var count = 0
    while(input != -1) {
      input = is.read()
      count += 1
    }
    count
  }

  case class ZipEntry(name: String, size: Int)

  private def makeWorkspaceFail(): Unit = {
    val brokenWorkspace = new BreakableWorkspaceProviderConfig() {
      override def readProjects()
                               (implicit user: UserContext): Option[Seq[ProjectConfig]] = {
        throw new RuntimeException("Cannot read projects. Workspace provider is broken!")
      }

      override def readTasks[T <: TaskSpec : ClassTag](project: Identifier, projectResources: ResourceManager)(implicit user: UserContext): Option[Seq[Task[T]]] = {
        throw new RuntimeException("Cannot read tasks. Workspace provider is broken!")
      }

      override def readTasksSafe[T <: TaskSpec : ClassTag](project: Identifier, projectResources: ResourceManager)(implicit user: UserContext): Option[Seq[Try[Task[T]]]] = {
        throw new RuntimeException("Cannot read tasks safely. Workspace provider is broken!")
      }
    }
    MockableWorkspaceProvider.configWorkspace(workspaceId, brokenWorkspace)
  }
}