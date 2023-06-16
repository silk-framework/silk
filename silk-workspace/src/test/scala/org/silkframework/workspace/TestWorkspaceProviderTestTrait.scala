package org.silkframework.workspace

import org.scalatest.{BeforeAndAfterAll, TestSuite}
import org.silkframework.config.{MetaData, Prefixes}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{ParameterValues, PluginContext, PluginRegistry, TestPluginContext}
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.util.Identifier
import org.silkframework.workspace.resources.SharedFileRepository

import java.io.{File, FileNotFoundException}

/**
  * Setups a test workspace with an in-memory workspace provider and temporary file based resource repository.
  */
trait TestWorkspaceProviderTestTrait extends BeforeAndAfterAll { this: TestSuite =>
  var oldWorkspaceFactory: WorkspaceFactory = _
  private val tmpDir = File.createTempFile("di-resource-repository", "-tmp")
  tmpDir.delete()
  tmpDir.mkdirs()

  /** The workspace provider that is used for holding the test workspace. */
  def workspaceProviderId: String = "inMemoryRdfWorkspace"

  def deleteRecursively(f: File): Unit = {
    if (f.isDirectory) {
      for (c <- f.listFiles())
        deleteRecursively(c)
    }
    if (!f.delete()) {
      throw new FileNotFoundException("Failed to delete file: " + f)
    }
  }

  /**
    * The WorkspaceProvide instance
    */
  lazy val workspaceProvider: WorkspaceProvider = {
    implicit val pluginContext: PluginContext = TestPluginContext(resources = InMemoryResourceManager())
    PluginRegistry.create[WorkspaceProvider](workspaceProviderId, ParameterValues.empty)
  }

  // This will initialize the workspace before any tests are executed
  def initWorkspaceBeforeAll: Boolean = true

  // Workaround for config problem, this should make sure that the workspace is a fresh in-memory RDF workspace
  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val replacementWorkspace = new Workspace(workspaceProvider, SharedFileRepository(tmpDir.getAbsolutePath))
    val rdfWorkspaceFactory = new WorkspaceFactory {
      /**
        * The current workspace of this user.
        */
      override def workspace(implicit userContext: UserContext): Workspace = replacementWorkspace

    }
    oldWorkspaceFactory = WorkspaceFactory.factory
    WorkspaceFactory.factory = rdfWorkspaceFactory
    implicit val testUserContext: UserContext.Empty.type = UserContext.Empty
    if(initWorkspaceBeforeAll) {
      WorkspaceFactory().workspace.projects // Initialize workspace before starting tests
    }
  }

  override protected def afterAll(): Unit = {
    WorkspaceFactory.factory = oldWorkspaceFactory
    deleteRecursively(tmpDir)
    super.afterAll()
  }

  def retrieveOrCreateProject(projectId: Identifier)(implicit userContext: UserContext): Project = {
    WorkspaceFactory().workspace(userContext).findProject(projectId) match{
      case Some(p) => p
      case None => WorkspaceFactory().workspace(userContext).createProject(new ProjectConfig(projectId, metaData = MetaData(Some(projectId))))
    }
  }
}
