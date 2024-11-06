package org.silkframework.plugins.dataset.csv

/**
 * Sanitizer for CSV rows. It deals with the main cause for CSV Injection, as documented in
 * https://owasp.org/www-community/attacks/CSV_Injection.
 */
object CSVSanitizer {
  def sanitize(str: String): String = {
    if (str.startsWith("=") || str.startsWith("+") || str.startsWith("-")
        || str.startsWith("@") || str.startsWith("\\u000D") || str.startsWith("\\u0009")
    ) "\"'" + doubleQuoteMask(str) + "\""
    else str
  }

  private def doubleQuoteMask(str: String): String = str.replace("\"", "\"\"")
}
