package org.silkframework.util

/**
 * Utilities for formatting markdown text.
 */
object MarkdownUtils {

  /**
   * Adds markdown formatting methods to StringBuilder.
   */
  implicit class MarkdownFormatting(sb: StringBuilder) {

    /**
     * Appends key-value pairs as a formatted list.
     */
    def appendMap(map: Map[Any, Any]): Unit = {
      for((key, value) <- map) {
        sb.append(s"- **$key:** $value\n")
      }
      sb.append("\n")
    }
  }

}
