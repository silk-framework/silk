package org.silkframework.runtime.serialization

import org.silkframework.config.Prefixes
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceManager}
import org.silkframework.util.IdentifierGenerator

/**
  * Holds context information when deserializing data.
  */
case class ReadContext(resources: ResourceManager = EmptyResourceManager,
                       prefixes: Prefixes = Prefixes.empty,
                       identifierGenerator: IdentifierGenerator = new IdentifierGenerator())
