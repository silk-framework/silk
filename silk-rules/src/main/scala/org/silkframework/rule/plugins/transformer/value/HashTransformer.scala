package org.silkframework.rule.plugins.transformer.value

import org.silkframework.rule.input.Transformer

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Digest operations for hash transformer plugins.
 *
 * Provides [[toHex]] and [[hashValue]] as concrete implementations over the abstract [[algorithm]]
 * parameter, which each concrete plugin supplies via its own plugin parameter. Subclasses implement
 * [[apply]] according to their combining or per-value semantics.
 */
trait HashTransformer extends Transformer {
  /** The hash algorithm name, passed directly to [[java.security.MessageDigest#getInstance]].
   *  Must be recognized by the JVM's security provider registry; unrecognized names cause
   *  [[java.security.NoSuchAlgorithmException]] at runtime. */
  def algorithm: String

  /** Encodes a byte array as a fixed-length lowercase hex string. Each byte maps to exactly two
   *  characters; without zero-padding, a byte with value 5 would render as "5" instead of "05",
   *  producing a variable-length string that breaks any downstream comparison or length check. */
  def toHex(bytes: Array[Byte]): String =
    bytes.map("%02x".format(_)).mkString

  /** Hashes a single string value and returns its hex digest. A fresh [[java.security.MessageDigest]]
   *  instance is created on every call: [[java.security.MessageDigest]] is stateful and not thread-safe,
   *  so reusing one instance across concurrent calls would corrupt the output. */
  def hashValue(v: String): String = {
    val digest = MessageDigest.getInstance(algorithm)
    digest.update(v.getBytes(StandardCharsets.UTF_8))
    toHex(digest.digest())
  }
}
