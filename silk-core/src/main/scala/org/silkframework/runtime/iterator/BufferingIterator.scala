package org.silkframework.runtime.iterator

/**
  * Iterator that is based on a single method `retrieveNext()`.
  * The next element is buffered for each iteration step.
  */
trait BufferingIterator[T] extends CloseableIterator[T] {

  // Buffered next element
  private var nextElement: Option[T] = None

  // Delay the first call to retrieveNext until the first time elements are retrieved.
  private var initialized: Boolean = false

  /**
    * Retrieves the next element.
    *
    * @return The next element or `None` if there are no more elements.
    */
  def retrieveNext(): Option[T]

  override final def hasNext: Boolean = {
    init()
    nextElement.isDefined
  }

  override final def next(): T = {
    init()
    val element =
      nextElement match {
        case Some(e) =>
          e
        case None =>
          throw new NoSuchElementException("No more elements")
      }
    nextElement = retrieveNext()
    element
  }

  @inline
  private def init(): Unit = {
    if (!initialized) {
      nextElement = retrieveNext()
      initialized = true
    }
  }
}
