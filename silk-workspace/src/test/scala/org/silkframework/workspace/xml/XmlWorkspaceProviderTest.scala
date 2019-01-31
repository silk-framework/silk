package org.silkframework.workspace.xml

import org.scalatest.FlatSpec
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.workspace.{WorkspaceProvider, WorkspaceProviderTestTrait}

class XmlWorkspaceProviderTest extends FlatSpec with WorkspaceProviderTestTrait {

  override def createWorkspaceProvider(): WorkspaceProvider = {
    new XmlWorkspaceProvider(InMemoryResourceManager())
  }
}
