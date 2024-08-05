package org.silkframework.plugins.dataset.csv

/**
 * Sanitizer for CSV rows. It deals with the causes for CSV Injection, as documented in
 * https://owasp.org/www-community/attacks/CSV_Injection.
 *
 * The solution is backported from DataPlatform. See https://gitlab.eccenca.com/elds/backend/-/merge_requests/591/diffs
 * as well as the task https://jira.eccenca.com/browse/CMEM-4219 for further details.
 */
object CSVSanitizer {
  def sanitize(str: String): String = {
    if (str.isEmpty) "\"\"" // there are CSV parsers that only accept "" as an escaped quote
    else if (
      str.startsWith("=") || str.startsWith("+") || str.startsWith("-")
        || str.startsWith("@") || str.startsWith("\\u000D") || str.startsWith("\\u0009")
    ) "\"'" + doubleQuoteMask(str) + "\""
    else if (
      str.contains("\"") || str.contains(",") || str.contains("\r") || str.contains("\n")
    ) "\"" + doubleQuoteMask(str) + "\""
    else str
  }

  private def doubleQuoteMask(str: String): String = str.replace("\"", "\"\"")
}
