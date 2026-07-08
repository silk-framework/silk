package org.silkframework.plugins.dataset.json

import java.math.{BigDecimal => JBigDec}

/**
 * Formats numbers for JSON serialization.
 */
object NumberFormatter {

  /**
   * Maximum number of characters a number may occupy in plain notation before scientific notation is used instead.
   * Bounds the output size for numbers with extreme exponents (e.g., 1e100000000), which would otherwise be
   * expanded into millions of digits.
   */
  private val maxPlainNotationLength = 50

  /**
   * Formats a number in plain notation (e.g., 172800000), unless that would exceed [[maxPlainNotationLength]]
   * characters, in which case scientific notation is used (e.g., 1E+1000).
   */
  def format(value: JBigDec): String = {
    val normalized = value.stripTrailingZeros
    if (plainNotationLength(normalized) <= maxPlainNotationLength) {
      normalized.toPlainString
    } else {
      normalized.toString
    }
  }

  /**
   * Computes the length of the plain notation of a number without generating it.
   */
  private def plainNotationLength(value: JBigDec): Long = {
    val digits = value.precision.toLong
    val scale = value.scale.toLong
    val length =
      if (scale <= 0) {
        digits - scale // Integer digits plus trailing zeros
      } else if (digits > scale) {
        digits + 1 // Digits before and after the decimal point plus the point itself
      } else {
        scale + 2 // "0." followed by leading zeros and the digits
      }
    length + (if (value.signum < 0) 1 else 0)
  }
}
