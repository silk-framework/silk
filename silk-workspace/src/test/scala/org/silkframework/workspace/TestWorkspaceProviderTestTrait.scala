package org.silkframework.workspace

import java.io.{File, FileNotFoundException}

import org.scalatest.{BeforeAndAfterAll, Suite}
import org.silkframework.config.Prefixes
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.workspace.resources.FileRepository

/**
  * Setups a test workspace with an in-memory workspace provider and temporary file based resource repository.
  */
trait TestWorkspaceProviderTestTrait extends BeforeAndAfterAll { this: Suite =>
  var oldWorkspaceFactory: WorkspaceFactory = _
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
    super.beforeAll()
    implicit val resourceManager: InMemoryResourceManager = InMemoryResourceManager()
    implicit val prefixes: Prefixes = Prefixes.empty
    val provider = PluginRegistry.create[WorkspaceProvider](workspaceProvider, Map.empty)
    val replacementWorkspace = new Workspace(provider, FileRepository(tmpDir.getAbsolutePath))
    val rdfWorkspaceFactory = new WorkspaceFactory {
      /**
        * The current workspace of this user.
        */
      override def workspace(implicit userContext: UserContext): Workspace = replacementWorkspace

    }
    oldWorkspaceFactory = WorkspaceFactory.factory
    WorkspaceFactory.factory = rdfWorkspaceFactory
  }

  override protected def afterAll(): Unit = {
    WorkspaceFactory.factory = oldWorkspaceFactory
    deleteRecursively(tmpDir)
    super.afterAll()
  }
}
