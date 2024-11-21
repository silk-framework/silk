package org.silkframework.workspace

import org.scalatest.flatspec.AnyFlatSpec
import org.silkframework.config.Prefixes
import org.silkframework.runtime.activity.UserContext

class InMemoryWorkspaceProviderTest extends AnyFlatSpec with WorkspaceProviderTestTrait {

  override def createWorkspaceProvider(workspacePrefixes: Prefixes): WorkspaceProvider = new InMemoryWorkspaceProvider() {
    override def fetchRegisteredPrefixes()(implicit userContext: UserContext): Prefixes = {
      workspacePrefixes
    }
  }
}
