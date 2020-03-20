package org.silkframework.runtime.serialization

import org.silkframework.config.Prefixes
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceLoader}

/**
  * Holds context information when serializing data.
  */
case class WriteContext[U](parent: Option[U] = None,
                           prefixes: Prefixes = Prefixes.empty,
                           projectId: Option[String] = None,
                           projectUri: Option[String] = None,
                           resourceLoader: ResourceLoader = EmptyResourceManager())
