package org.silkframework.rule

/**
  * Holds example inputs/outputs for operator plugins.
  */
case class OperatorExampleValues[T <: OperatorExampleValue](examples: Seq[T]) {

  def markdownFormatted(sb: StringBuilder): Unit = {
    if (examples.nonEmpty) {
      sb ++= "\n"
      sb ++= "### Examples"
      sb ++= "\n\n"
      sb ++= "**Notation:** List of values are represented via square brackets. Example: `[first, second]` represents a list of two values \"first\" and \"second\".\n\n"
      for ((example, idx) <- examples.zipWithIndex) {
        sb ++= "---\n"
        example.description match {
          case Some(desc) =>
            sb ++= s"#### ${desc.stripSuffix(".")}:\n\n"
          case None =>
            sb ++= s"#### Example ${idx + 1}:\n\n"
        }
        if (example.parameters.nonEmpty) {
          sb ++= "* Parameters\n"
          for ((param, paramValue) <- example.parameters) {
            sb ++= s"  * *$param*: `$paramValue`\n"
          }
          sb ++= "\n"
        }
        example.markdownFormatted(sb)
        sb ++= "\n\n"
      }
    }
  }

}

/**
  * Example inputs/outputs for operator plugins.
  */
trait OperatorExampleValue {

  /**
    * Description of this example value.
    */
  def description: Option[String]

  /**
    * Plugin parameters.
    */
  def parameters: Map[String, String]

  /**
   * The class of the exception that is expected to be thrown by this example, if any.
   */
  def throwsException: Option[Class[_]]

  /**
    * Format this example as markdown.
    */
  def markdownFormatted(sb: StringBuilder): Unit
}
