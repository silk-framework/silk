package org.silkframework.rule.plugins.transformer.replace

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}

/**
  * Transformer that takes two inputs.
  * It acts similar as [[MapTransformer]] with the difference that instead of defining a default value, it
  * defines a default input. The first input is the value that should be mapped. The second input is the
  * default value that should be returned if it cannot be mapped.
  */
@Plugin(
  id = "mapWithDefaultInput",
  categories = Array("Replace"),
  label = "Map with default",
  description =
      """
Takes two inputs.
Tries to map the first input based on the map of values parameter config.
If the input value is not found in the map, it takes the value of the second input.
The indexes of the mapped value and the default value match. If there are less default values than
values to map, the last default value is replicated to match the count.
      """
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
