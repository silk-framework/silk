package org.silkframework.workspace

import org.scalatest.Suite
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.ConfigTestTrait
import org.silkframework.workspace.xml.XmlZipProjectMarshaling

import java.io.File

/**
  * A trait that will configure the workspace to be in-memory.
  */
trait InMemoryWorkspaceTestTrait extends ConfigTestTrait {
  this: Suite =>
  /** The properties that should be changed.
    * If the value is [[None]] then the property value is removed,
    * else it is set to the new value.
    */
  override def propertyMap: Map[String, Option[String]] = Map(
    "workspace.provider.plugin" -> Some("inMemoryRdfWorkspace")
  )

  def importProject(projectPathInClasspath: String, projectId: String): Project = {
    implicit val userContext: UserContext = UserContext.Empty
    val workspace = WorkspaceFactory().workspace
    val projectFile = try {
      new File(getClass.getClassLoader.getResource(projectPathInClasspath).getFile)
    } catch {
      case npe: NullPointerException =>
        throw new RuntimeException(s"Project file '$projectPathInClasspath' does not exist!", npe)
    }
    if(workspace.findProject(projectId).nonEmpty) {
      workspace.removeProject(projectId)
    }
    workspace.importProject(projectId, projectFile, XmlZipProjectMarshaling())
    workspace.project(projectId)
  }
}
