package controllers.util

import java.nio.charset.StandardCharsets

/**
 * Builds Content-Disposition header values with the file name encoded according to RFC 6266.
 * Non-ASCII file names are carried by the RFC 5987 `filename*` parameter, so the header value always stays ASCII-safe.
 * Putting them into the header directly would generate an invalid header, because non-ASCII characters are not allowed.
 */
object ContentDisposition {

  // RFC 5987 attr-char: characters that may stay unencoded in the filename* parameter
  private val attrChars: Set[Char] = (('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')).toSet ++ "!#$&+-.^_`|~"

  /**
   * Generates a header value of the form `attachment; filename="fallback"; filename*=UTF-8''encoded`.
   * The `filename*` parameter is only added if the file name contains characters that are not safe within a quoted string.
   *
   * @param dispositionType Either "attachment" or "inline".
   * @param fileName The file name, may contain arbitrary unicode characters.
   */
  def apply(dispositionType: String, fileName: String): String = {
    val fallback = fileName.map(c => if (c >= ' ' && c <= '~' && c != '"' && c != '\\') c else '?')
    if (fallback == fileName) {
      s"""$dispositionType; filename="$fileName""""
    } else {
      val encoded = fileName.getBytes(StandardCharsets.UTF_8).map { b =>
        val c = (b & 0xff).toChar
        if (attrChars.contains(c)) c.toString else "%%%02X".format(b & 0xff)
      }.mkString
      s"""$dispositionType; filename="$fallback"; filename*=UTF-8''$encoded"""
    }
  }
}
