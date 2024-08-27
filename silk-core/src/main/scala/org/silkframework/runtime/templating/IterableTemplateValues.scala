package org.silkframework.runtime.templating

import java.util
import scala.jdk.CollectionConverters.IteratorHasAsJava

/**
  * An iterable wrapper around sequences of values.
  * Overrides toString for template output.
  */
class IterableTemplateValues(val values: Seq[String]) extends java.lang.Iterable[String] {

  override def toString: String = values.mkString("")

  override def iterator(): util.Iterator[String] = {
    values.iterator.asJava
  }

  override def equals(obj: Any): Boolean = {
    obj match {
      case iterableValues: IterableTemplateValues =>
        iterableValues.values == this.values
      case _ =>
        false
    }
  }

  override def hashCode(): Int = {
    values.hashCode()
  }
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
