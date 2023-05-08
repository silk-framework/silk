package org.silkframework.workspace

import org.scalatest.flatspec.AnyFlatSpec

class InMemoryWorkspaceProviderTest extends AnyFlatSpec with WorkspaceProviderTestTrait {

  override def createWorkspaceProvider(): WorkspaceProvider = new InMemoryWorkspaceProvider()
}
