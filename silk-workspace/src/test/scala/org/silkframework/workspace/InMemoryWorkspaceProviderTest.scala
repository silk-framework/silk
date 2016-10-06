package org.silkframework.workspace

import org.scalatest.FlatSpec

/**
  * Created on 9/15/16.
  */
class InMemoryWorkspaceProviderTest extends FlatSpec with WorkspaceProviderTestTrait {

  override def createWorkspaceProvider(): WorkspaceProvider = InMemoryWorkspaceProvider()
}
