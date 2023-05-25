package org.silkframework.runtime.serialization

import org.silkframework.config.Prefixes
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceManager}
import org.silkframework.util.{Identifier, IdentifierGenerator}

/** ReadContext to be used in tests. */
object TestReadContext {
  def apply(resources: ResourceManager = EmptyResourceManager(),
            prefixes: Prefixes = Prefixes.empty,
            identifierGenerator: IdentifierGenerator = new IdentifierGenerator(),
            validationEnabled: Boolean = false,
            user: UserContext = UserContext.Empty,
            projectId: Option[Identifier] = None): ReadContext = {
    ReadContext(resources, prefixes, identifierGenerator, validationEnabled, user, projectId)
  }
}
