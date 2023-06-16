package org.silkframework.rule.evaluation

import org.silkframework.rule.input.{Input, PathInput, TransformInput}

/**
 * An intermediate value of a input operator evaluation.
 */
sealed trait Value {
  /**
   * The corresponding input that generated this value
   */
  def input: Input

  /**
   * The values the resulted from evaluating the corresponding input.
   */
  def values: Seq[String]

  /**
   * The intermediate values of the children operators of the corresponding input.
   */
  def children: Seq[Value]

  /**
    * Error that occurred for the values.
    */
  def error: Option[Throwable]

  /**
    * Returns a new instance of this value with a given error attached.
    */
  def withError(ex: Throwable): Value

  /**
    * Formats the error into a multi-line string.
    * Includes the cause of the error, if it is not already contained in the main error message.
    */
  def formattedErrorMessage: Option[String] = {
    for(ex <- error) yield {
      val messages = LazyList.iterate(ex)(_.getCause).takeWhile(_ != null).map(_.getMessage)
      messages.reduce { (msg, cause) => if(msg.contains(cause)) msg else msg + "\nCause: " + cause }
    }
  }
}

/**
 * An intermediate value of a transformation evaluation.
 */
case class TransformedValue(input: TransformInput, values: Seq[String], children: Seq[Value], error: Option[Throwable] = None) extends Value {

  def withError(ex: Throwable): Value = copy(error = Some(ex))

}

/**
 * An intermediate value of a path input evaluation.
 */
case class InputValue(input: PathInput, values: Seq[String], error: Option[Throwable] = None) extends Value {

  def children = Seq.empty

  def withError(ex: Throwable): Value = copy(error = Some(ex))
}