package org.silkframework.workspace

import org.scalatest._
import org.silkframework.config.Prefixes
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.workspace.resources.InMemoryResourceRepository
import org.silkframework.workspace.xml.XmlZipProjectMarshaling

/**
  * Trait that can be mixed in to replace the workspace provider with an in-memory version
  * that has a project pre-loaded from the Classpath.
  */
trait SingleProjectWorkspaceProviderTestTrait extends BeforeAndAfterAll { this: Suite=>
  /**
    * Returns the path of the XML zip project that should be loaded before the test suite starts.
    */
  def projectPathInClasspath: String

  /** The id under which this project will be accessible */
  def projectId: String
  def singleWorkspaceProviderId: String = "inMemoryRdfWorkspace"

  private var oldUserManager: () => User = _
  private var expectedUser: User = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    implicit val resourceManager = InMemoryResourceManager()
    implicit val prefixes = Prefixes.empty
    val provider = PluginRegistry.create[WorkspaceProvider](singleWorkspaceProviderId, Map.empty)
    val replacementWorkspace = new Workspace(provider, InMemoryResourceRepository())
    val is = getClass.getClassLoader.getResourceAsStream(projectPathInClasspath)
    assert(Option(is).isDefined, "Resource was not found in classpath: " + projectPathInClasspath)
    replacementWorkspace.importProject(projectId, is, XmlZipProjectMarshaling())
    expectedUser = new User {
      /**
        * The current workspace of this user.
        */
      override def workspace: Workspace = replacementWorkspace
    }
    oldUserManager = User.userManager
    User.userManager = () => expectedUser
  }

  this.beforeAll()
    assert(Option(expectedUser).isDefined && expectedUser == User(),
      "User was different! Try changing the mixin order of SingleProjectWorkspaceProviderTestTrait.")
    assert(project.config.id.toString == projectId)

  override protected def afterAll(): Unit = {
    User.userManager = oldUserManager
    super.afterAll()
  }

  def project: Project = {
    User().workspace.project(projectId)
  }
}
