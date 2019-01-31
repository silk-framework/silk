package org.silkframework.workspace

import org.scalatest.FlatSpec

class InMemoryWorkspaceProviderTest extends FlatSpec with WorkspaceProviderTestTrait {

  override def createWorkspaceProvider(): WorkspaceProvider = InMemoryWorkspaceProvider()
}
