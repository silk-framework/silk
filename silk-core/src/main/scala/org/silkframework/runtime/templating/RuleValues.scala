package org.silkframework.runtime.templating

import java.util
import scala.collection.JavaConverters._

/**
  * A wrapper around the values passed between rule operators, such as transformers.
  * Overrides toString for template output.
  */
class RuleValues(val values: Seq[String]) extends java.lang.Iterable[String] {

  override def toString: String = values.mkString("")

  override def iterator(): util.Iterator[String] = {
    values.iterator.asJava
  }
}

object RuleValues {

  /**
    * Creates a RuleValues object only if required.
    * If a sequence with just one values is provided, a string is generated for compatibility with built-in functions.
    */
  def fromValues(values: Seq[String]): AnyRef = {
    if(values.size == 1) {
      values.head
    } else {
      new RuleValues(values)
    }
  }

}
