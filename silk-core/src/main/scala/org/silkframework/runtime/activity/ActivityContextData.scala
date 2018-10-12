package org.silkframework.runtime.activity

/**
  * Used for passing generic context (meta-) data for ActivityContexts
  * @tparam T - the type of the thing to pass into the ActivityExecution
  */
trait ActivityContextData[T] {

  /**
    * @return the thing to pass into the ActivityExecution
    */
  def contextData: T

}
