package org.silkframework.runtime.resource

/**
  * A Resource with a list of predefined types overriding any automatically determined types.
  * If multiple typed are contained with an resource, their implicit ordering has to align with the
  * order of this sequence.
  * If no type is given, no overriding of the automatically determined types happen.
  */
trait ResourceWithKnownTypes { self: Resource =>
  def knownTypes: IndexedSeq[String]
}