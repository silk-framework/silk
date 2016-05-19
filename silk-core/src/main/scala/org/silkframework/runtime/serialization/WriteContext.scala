package org.silkframework.runtime.serialization

import org.silkframework.config.Prefixes

/**
  * Holds context information when serializing data.
  */
case class WriteContext[U](parent: Option[U] = None, prefixes: Prefixes = Prefixes.empty)
