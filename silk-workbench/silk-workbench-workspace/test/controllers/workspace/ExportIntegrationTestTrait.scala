package controllers.workspace

import java.io.{ByteArrayInputStream, InputStream}
import java.util.UUID
import java.util.zip.ZipInputStream

import helper.IntegrationTestTrait
import org.scalatest.{MustMatchers, Suite}
import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.util.Identifier
import org.silkframework.workspace._

import scala.io.Source
import scala.reflect.ClassTag
import scala.util.Try


/**
  * Test base for workspace and project export integration tests.
  */
trait ExportIntegrationTestTrait
    extends IntegrationTestTrait
        with SingleProjectWorkspaceProviderTestTrait
        with MustMatchers {
  this: Suite =>

  override def projectPathInClasspath: String = "controllers/workspace/miniProject.zip"

  override def workspaceProvider: String = "mockableInMemoryWorkspace"

  private lazy val workspace: Workspace = WorkspaceFactory().workspace

  private lazy val provider: WorkspaceProvider = workspace.provider

  private def workspaceId: UUID = provider.asInstanceOf[MockableWorkspaceProvider].id

  override def routes: Option[String] = Some("test.Routes")

  protected def checkZipEntries(responseBody: Array[Byte], expectedFiles: Seq[String]): Unit = {
    val entries = zipEntries(responseBody)
    entries.map(_.name) mustBe expectedFiles
    entries.foreach(_.size must be > 10) // Check for 'No empty files'
  }

  // Get the file as string
  protected def getZipEntry(responseBody: Array[Byte], file: String): String = {
    val zip = zipStream(responseBody)
    var nextEntry = zip.getNextEntry
    var result = ""
    while(nextEntry != null && result.isEmpty) {
      if(nextEntry.getName == file) {
        result = Source.fromInputStream(zip).mkString
      }
      nextEntry = zip.getNextEntry
    }
    zip.close()
    result
  }

  private def zipEntries(responseBytes: Array[Byte]): Seq[ZipEntry] = {
    val zip = zipStream(responseBytes)
    var results = List.empty[ZipEntry]
    var nextEntry = zip.getNextEntry
    while(nextEntry != null) {
      results ::= ZipEntry(nextEntry.getName, streamSize(zip))
      nextEntry = zip.getNextEntry
    }
    zip.close()
    results
  }

  private def zipStream(responseBytes: Array[Byte]): ZipInputStream = {
    new ZipInputStream(new  ByteArrayInputStream(responseBytes))
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

  protected def makeWorkspaceFail(): Unit = {
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
