package org.silkframework.plugins.transformer.sequence

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.Plugin

/**
  * Get value with
  */
@Plugin(
  id = "getValueByIndex",
  categories = Array("Sequence"),
  label = "get value by index",
  description =
    """Returns the value found at the specified index. Fails or returns an empty result depending on failIfNoFound is set or not.
       Please be aware that this will work only if the data source supports some kind of ordering like XML or JSON. This
       is probably not a good idea to do with RDF models.
    """
)
case class GetValueByIndexTransformer(index: Int, failIfNotFound: Boolean = false) extends Transformer {
  override def apply(values: Seq[Seq[String]]) = {
    values.map { vs =>
      vs.drop(index).headOption match {
        case None if failIfNotFound =>
          throw new IndexOutOfBoundsException("No value at index " + index + ".")
        case None if !failIfNotFound =>
          Seq()
        case Some(v) =>
          Seq(v)
      }
    } flatten
  }
}
