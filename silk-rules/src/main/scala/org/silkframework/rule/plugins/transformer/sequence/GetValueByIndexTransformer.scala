package org.silkframework.rule.plugins.transformer.sequence

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.Plugin

/**
  * For each input sequence take the element with specified index and concatenate all values to a new sequence.
  * If one input has no element with the specified index it is either ignored or it fails.
  */
@Plugin(
  id = "getValueByIndex",
  categories = Array("Sequence"),
  label = "Get value by index",
  description =
    """Returns the value found at the specified index. Fails or returns an empty result depending on failIfNoFound is set or not.
       Please be aware that this will work only if the data source supports some kind of ordering like XML or JSON. This
       is probably not a good idea to do with RDF models.

       If emptyStringToEmptyResult is true then instead of a result with an empty String, an empty result is returned.
    """
)
case class GetValueByIndexTransformer(index: Int,
                                      failIfNotFound: Boolean = false,
                                      emptyStringToEmptyResult: Boolean = false) extends Transformer {
  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    values.flatMap { vs =>
      vs.drop(index).headOption match {
        case None if failIfNotFound =>
          throw new IndexOutOfBoundsException("No value at index " + index + ".")
        case None if !failIfNotFound =>
          Seq()
        case Some(v) =>
          if(emptyStringToEmptyResult && v.isEmpty) {
            Seq()
          } else {
            Seq(v)
          }
      }
    }
  }
}
