package org.silkframework.util

object MarkdownUtils {

  implicit class MarkdownFormatting(sb: StringBuilder) {

    def appendMap(map: Map[Any, Any]): Unit = {
      for((key, value) <- map) {
        sb.append(s"- **$key:** $value\n")
      }
      sb.append("\n")
    }
  }

}
