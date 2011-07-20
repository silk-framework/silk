package de.fuberlin.wiwiss.silk.util

/**
 * Thrown if the configuration is not valid.
 */
class ValidationException(val errors: Seq[String], cause: Throwable) extends Exception(errors.mkString(" ")) {
  def this(errors: Seq[String]) = this (errors, null)

  def this(error: String, cause: Throwable) = this (error :: Nil, cause)

  def this(error: String) = this (error :: Nil, null)
}