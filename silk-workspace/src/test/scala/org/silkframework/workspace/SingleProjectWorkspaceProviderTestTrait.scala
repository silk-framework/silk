package org.silkframework.workspace

import java.io.File

import org.scalatest._
import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.runtime.activity.{TestUserContextTrait, UserContext}
import org.silkframework.workspace.activity.workflow.{LocalWorkflowExecutorGeneratingProvenance, Workflow}
import org.silkframework.workspace.xml.XmlZipProjectMarshaling

/**
  * Trait that can be mixed in to replace the workspace provider with an in-memory version
  * that has a project pre-loaded from the Classpath.
  */
trait SingleProjectWorkspaceProviderTestTrait extends BeforeAndAfterAll with TestWorkspaceProviderTestTrait with TestUserContextTrait { this: TestSuite =>
  /**
    * Returns the path of the XML zip project that should be loaded before the test suite starts.
    */
  def projectPathInClasspath: String

  /** The id under which this project will be accessible */
  def projectId: String = "singleProject"

  /**
    * Fail if a task loading error occurred during project import.
    */
  def failOnTaskLoadingErrors: Boolean = true

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val is = try {
      new File(getClass.getClassLoader.getResource(projectPathInClasspath).getFile)
    } catch {
      case npe: NullPointerException =>
        throw new RuntimeException(s"Project file '$projectPathInClasspath' does not exist!")
    }
    assert(Option(is).isDefined, "Resource was not found in classpath: " + projectPathInClasspath)
    WorkspaceFactory().workspace.importProject(projectId, is, XmlZipProjectMarshaling())
    val loadingErrors = WorkspaceFactory().workspace.project(projectId).loadingErrors
    if(failOnTaskLoadingErrors && loadingErrors.nonEmpty) {
      fail("Test project could not load all tasks. Details: " + loadingErrors)
    }
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
  }

  def project(implicit userContext: UserContext): Project = {
    WorkspaceFactory().workspace(userContext).project(projectId)
  }

  def workspaceEndpoint(implicit userContext: UserContext): SparqlEndpoint = {
    WorkspaceFactory().workspace(userContext).provider.sparqlEndpoint match {
      case Some(endpoint) =>
        endpoint
      case _ =>
        throw new RuntimeException("Not an RDF workspace provider configured!")
    }
  }

  def executeWorkflow(workflowId: String)
                     (implicit userContext: UserContext): Unit = {
    project.task[Workflow](workflowId).activity[LocalWorkflowExecutorGeneratingProvenance].control.startBlocking()
  }
}
