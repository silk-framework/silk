package controllers.workspace

import helper.IntegrationTestTrait
import org.scalatest.TestSuite
import org.silkframework.config.TaskSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.util.Identifier
import org.silkframework.workspace._

import java.io.{ByteArrayInputStream, InputStream}
import java.util.UUID
import java.util.zip.ZipInputStream
import scala.io.{Codec, Source}
import scala.reflect.ClassTag
import org.scalatest.matchers.must.Matchers


/**
  * Test base for workspace and project export integration tests.
  */
trait ExportIntegrationTestTrait
    extends IntegrationTestTrait
        with SingleProjectWorkspaceProviderTestTrait
        with Matchers {
  this: TestSuite =>

  override def projectPathInClasspath: String = "controllers/workspace/miniProject.zip"

  //FIXME the project generates task loading errors during import
  override def failOnTaskLoadingErrors: Boolean = false

  protected override def routes = Some(classOf[testWorkspace.Routes])

  override def workspaceProviderId: String = "mockableInMemoryWorkspace"

  private lazy val workspace: Workspace = WorkspaceFactory().workspace

  private lazy val provider: WorkspaceProvider = workspace.provider

  private def workspaceId: UUID = provider.asInstanceOf[MockableWorkspaceProvider].id

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
        result = Source.fromInputStream(zip)(Codec.UTF8).mkString
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

      override def readTasks[T <: TaskSpec : ClassTag](project: Identifier)
                                                      (implicit context: PluginContext): Option[Seq[LoadedTask[T]]] = {
        throw new RuntimeException("Cannot read tasks. Workspace provider is broken!")
      }

      override def readAllTasks(project: Identifier)
                               (implicit context: PluginContext): Option[Seq[LoadedTask[_]]] = {
        throw new RuntimeException("Cannot read all tasks. Workspace provider is broken!")
      }
    }
    MockableWorkspaceProvider.configWorkspace(workspaceId, brokenWorkspace)
  }
}
