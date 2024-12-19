package org.silkframework.workspace.xml


import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.workspace.{WorkspaceProvider, WorkspaceProviderTestTrait}
import org.scalatest.flatspec.AnyFlatSpec
import org.silkframework.config.Prefixes
import org.silkframework.runtime.activity.UserContext

class XmlWorkspaceProviderTest extends AnyFlatSpec with WorkspaceProviderTestTrait {

  override def createWorkspaceProvider(workspacePrefixes: Prefixes): WorkspaceProvider = {
    new XmlWorkspaceProvider(InMemoryResourceManager()) {
      override def fetchRegisteredPrefixes()(implicit userContext: UserContext): Prefixes = {
        workspacePrefixes
      }
    }
  }
}
