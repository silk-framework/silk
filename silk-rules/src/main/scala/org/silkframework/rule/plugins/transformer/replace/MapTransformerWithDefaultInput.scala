package org.silkframework.rule.plugins.transformer.replace

import org.silkframework.rule.input.Transformer
import org.silkframework.rule.plugins.transformer.value.InputHashTransformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, PluginReference}

/**
  * Transformer that takes two inputs.
  * It acts similar as [[MapTransformer]] with the difference that instead of defining a default value, it
  * defines a default input. The first input is the value that should be mapped. The second input is the
  * default value that should be returned if it cannot be mapped.
  */
@Plugin(
  id = MapTransformerWithDefaultInput.pluginId,
  categories = Array("Replace"),
  label = "Map with default",
  description = """Maps input values from the first input using a predefined map, with fallback to default values provided by the second input.""",
  documentationFile = "MapTransformerWithDefaultInput.md",
  relatedPlugins = Array(
    new PluginReference(
      id = InputHashTransformer.pluginId,
      description = "The Map with default plugin maps each input value and returns one output value per position, using the second input as the fallback when a value is not found in the map. The Input hash plugin returns one hash value for all input values combined, so the result is one combined identifier rather than a mapped value sequence."
    ),
    new PluginReference(
      id = MapTransformer.pluginId,
      description = "Map with default takes its fallback values from a second input, one per input value, so the fallback can differ per value. If the fallback is a fixed string that applies to all unmapped values, the Map plugin accepts it as a parameter and requires only one input."
    )
  )
)
case class MapTransformerWithDefaultInput(@Param(value = "A map of values", example = "A:1,B:2,C:3")
                                          map: Map[String, String]) extends Transformer {
  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    if(values.size != 2) {
      throw new IllegalArgumentException("MapDefaultInput takes exactly two inputs! Instead found " + values.size + " inputs.")
    }
    if(values(1).size < 1) {
      throw new IllegalArgumentException("Default input for MapDefaultInput contains no value!")
    }
    val valuesToMap = values.head
    val defaults = values(1)
    val defaultValues = if(defaults.size < valuesToMap.size) {
      val filler = defaults.last
      val fillDefaults = for(i <- defaults.size until valuesToMap.size) yield filler
      defaults ++ fillDefaults
    } else {
      values(1)
    }
    val results = for((value, default) <- valuesToMap.zip(defaultValues)) yield {
      evaluate(value).getOrElse(default)
    }
    results.toSeq
  }

  private def evaluate(value: String): Option[String] = {
    map.get(value)
  }
}

object MapTransformerWithDefaultInput {
  final val pluginId = "mapWithDefaultInput"
}
