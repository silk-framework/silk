package de.fuberlin.wiwiss.silk.util

import java.util.logging.Logger

/**
 * Can be used to measure the time needed to execute a provided expression.
 */
object Timer {
  def apply[T](description: String)(f: => T)(implicit logger: Logger): T = {
    val startTime = System.currentTimeMillis

    val result = f

    val elapsedTime = (System.currentTimeMillis - startTime)
    logger.info(description + " took " + elapsedTime + "ms")

    result
  }
}