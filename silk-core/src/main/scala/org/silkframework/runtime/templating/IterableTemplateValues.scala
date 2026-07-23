package org.silkframework.runtime.templating

/**
  * A list wrapper around sequences of values.
  * Being a Java list makes the values indexable (e.g. 'values[0]') and iterable in templates.
  * Overrides toString for template output.
  */
class IterableTemplateValues(val values: Seq[String]) extends java.util.AbstractList[String] {

  override def get(index: Int): String = values(index)

  override def size(): Int = values.size

  override def toString: String = values.mkString("")
}

object IterableTemplateValues {

  /**
    * Creates a IterableTemplateValues object only if required.
    * If a sequence with just one values is provided, a string is generated for compatibility with built-in functions.
    */
  def fromValues(values: Seq[String]): AnyRef = {
    if(values.size == 1) {
      values.head
    } else {
      new IterableTemplateValues(values)
    }
  }

}
