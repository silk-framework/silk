package org.silkframework.workspace

import org.scalatest.{BeforeAndAfterAll, TestSuite}
import org.silkframework.config.{MetaData, Prefixes}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{ParameterValues, PluginContext, PluginRegistry, TestPluginContext}
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.util.{ConfigTestTrait, Identifier}
import org.silkframework.workspace.resources.{ResourceRepository, SharedFileRepository}

import java.io.{File, FileNotFoundException}
import scala.util.Try

/**
  * Setups a test workspace with an in-memory workspace provider, a temporary file based resource repository and,
  * unless the suite configures a journal store itself, an in-memory change journal.
  */
trait TestWorkspaceProviderTestTrait extends BeforeAndAfterAll { this: TestSuite =>
  var oldWorkspaceFactory: WorkspaceFactory = _
  private var testWorkspace: Workspace = _
  // The previous value of the journal store property, while this suite set it
  private var journalStoreBackup: Option[Iterable[(String, Option[String])]] = None
  private val tmpDir = File.createTempFile("di-resource-repository", "-tmp")
  tmpDir.delete()
  tmpDir.mkdirs()

  /** The workspace provider that is used for holding the test workspace. */
  def workspaceProviderId: String = "inMemoryRdfWorkspace"

  /** Creates the resource repository for testing based on a temporary directory */
  def createResourceRepository(dir: File): ResourceRepository = {
    SharedFileRepository(dir.getAbsolutePath)
  }

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
    // The journal store is a config singleton. A suite that does not set one records into the in-memory store, so no
    // test writes into the file store that a configuration on the class path may default to, e.g. the root build's.
    if(System.getProperty(TestWorkspaceProviderTestTrait.JOURNAL_STORE_KEY) == null) {
      journalStoreBackup = Some(ConfigTestTrait.updateAndBackupParameters(
        Seq(TestWorkspaceProviderTestTrait.JOURNAL_STORE_KEY -> Some("inMemoryChangeJournal"))))
    }
    val replacementWorkspace = new Workspace(workspaceProvider, createResourceRepository(tmpDir))
    testWorkspace = replacementWorkspace
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
      WorkspaceFactory().workspace.userProjects // Initialize workspace before starting tests
    }
    runAfterWorkspaceInit()
  }

  def runAfterWorkspaceInit(): Unit = {
    // This is run right after the workspace has initialized.
  }

  override protected def afterAll(): Unit = {
    WorkspaceFactory.factory = oldWorkspaceFactory
    stopAllActivities()
    clearChangeJournals()
    journalStoreBackup.foreach(ConfigTestTrait.updateAndBackupParameters)
    journalStoreBackup = None
    deleteRecursively(tmpDir)
    super.afterAll()
  }

  /** Drops the journals of the test projects: the configured journal store outlives this suite's workspace. */
  private def clearChangeJournals(): Unit = {
    implicit val userContext: UserContext = UserContext.Empty
    if (testWorkspace != null) {
      testWorkspace.userProjects.foreach(_.changeJournal.clear())
    }
  }

  /** Cancels and awaits all activities, so none still holds a file in tmpDir when it is deleted. */
  private def stopAllActivities(): Unit = {
    implicit val userContext: UserContext = UserContext.Empty
    if (testWorkspace != null) {
      val controls = testWorkspace.activities.map(_.control) ++
        testWorkspace.userProjects.flatMap(p => p.activities.map(_.control) ++ p.allTasks.flatMap(_.activities.map(_.control)))
      controls.foreach(_.cancel())
      controls.foreach(c => Try(c.waitUntilFinished()))
    }
  }

  def retrieveOrCreateProject(projectId: Identifier, prefixes: Prefixes = Prefixes.default)(implicit userContext: UserContext): Project = {
    WorkspaceFactory().workspace(userContext).projectOption(projectId) match{
      case Some(p) => p
      case None => WorkspaceFactory().workspace(userContext).createProject(new ProjectConfig(projectId, metaData = MetaData(Some(projectId)), projectPrefixes = prefixes))
    }
  }
}

object TestWorkspaceProviderTestTrait {
  /** The config key of the journal store plugin. */
  private val JOURNAL_STORE_KEY = "workspace.changes.plugin"
}
