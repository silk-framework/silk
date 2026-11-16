package org.silkframework.serialization.json.changes

import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.workspace.WorkspaceProvider
import org.silkframework.workspace.xml.XmlWorkspaceProvider

/** The round trip against the XML workspace provider, which writes XML on put and parses it on reload. */
class XmlChangePayloadRoundTripTest extends ChangePayloadRoundTripTrait {

  override lazy val workspaceProvider: WorkspaceProvider = new XmlWorkspaceProvider(InMemoryResourceManager())
}
