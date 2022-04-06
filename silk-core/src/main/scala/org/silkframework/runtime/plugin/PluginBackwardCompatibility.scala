package org.silkframework.runtime.plugin

/** Contains data to provide backward compatibility for specific plugin changes. */
object PluginBackwardCompatibility {
  /** Specifies mappings of Transformer plugin IDs that have been changed in the past in order to read old serializations (XML, JSON, RDF). */
  val transformerIdMapping = Map(
    "negate" -> "negateTransformer"
  )
  /** Specifies mappings of DistanceMeasure plugin IDs that have been changed in the past in order to read old serializations (XML, JSON, RDF). */
  val distanceMeasureIdMapping = Map(
    "constant" -> "constantDistance",
    "substring" -> "substringDistance"
  )
}
