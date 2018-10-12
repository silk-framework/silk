package org.silkframework.workspace

import org.scalatest._
import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.xml.XmlZipProjectMarshaling

/**
  * Trait that can be mixed in to replace the workspace provider with an in-memory version
  * that has a project pre-loaded from the Classpath.
  */
trait SingleProjectWorkspaceProviderTestTrait extends BeforeAndAfterAll with TestWorkspaceProviderTestTrait { this: Suite =>
  /**
    * Returns the path of the XML zip project that should be loaded before the test suite starts.
    */
  def projectPathInClasspath: String

  /** The id under which this project will be accessible */
  def projectId: String
  def userContext: UserContext

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val is = getClass.getClassLoader.getResourceAsStream(projectPathInClasspath)
    assert(Option(is).isDefined, "Resource was not found in classpath: " + projectPathInClasspath)
    implicit val userContext: UserContext = UserContext.Empty
    WorkspaceFactory().workspace.importProject(projectId, is, XmlZipProjectMarshaling())
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
  }

  def project(implicit userContext: UserContext): Project = {
    WorkspaceFactory().workspace(userContext).project(projectId)
  }

  def workspaceEndpoint(implicit userContext: UserContext): SparqlEndpoint = {
    WorkspaceFactory().workspace(userContext).provider match {
      case rdfWorkspace: RdfWorkspaceProvider =>
        rdfWorkspace.endpoint
      case _ =>
        throw new RuntimeException("Not an RDF workspace provider configured!")
    }
  }
}
