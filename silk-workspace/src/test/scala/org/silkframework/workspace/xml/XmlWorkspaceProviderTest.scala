package org.silkframework.workspace.xml

import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.workspace.{WorkspaceProvider, WorkspaceProviderTestTrait}
import org.scalatest.flatspec.AnyFlatSpec

class XmlWorkspaceProviderTest extends AnyFlatSpec with WorkspaceProviderTestTrait {

  override def createWorkspaceProvider(): WorkspaceProvider = {
    new XmlWorkspaceProvider(InMemoryResourceManager())
  }
}
