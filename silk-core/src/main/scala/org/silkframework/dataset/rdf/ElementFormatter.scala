package org.silkframework.dataset.rdf

/**
  * A [[Formatter]] that also defined how to format elements of a specific type.
  */
trait ElementFormatter[T] extends Formatter {
  /** Formats an element. */
  def formatElement(element: T): String
}
