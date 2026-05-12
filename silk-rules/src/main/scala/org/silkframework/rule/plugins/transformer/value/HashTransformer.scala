package org.silkframework.rule.plugins.transformer.value

import org.silkframework.rule.input.Transformer

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.HexFormat

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

  // @transient: HexFormat is not serializable. lazy: transient fields are null after deserialization;
  // lazy ensures the instance is recreated on first use rather than remaining null.
  @transient private lazy val hexFormat: HexFormat = HexFormat.of()

  // @transient: not serialized. lazy: recreated on first use after deserialization.
  @transient private lazy val localDigest: ThreadLocal[MessageDigest] =
    ThreadLocal.withInitial(() => MessageDigest.getInstance(algorithm))

  /** Encodes a byte array as a fixed-length lowercase hex string. Each byte maps to exactly two
   *  characters; without zero-padding, a byte with value 5 would render as "5" instead of "05",
   *  producing a variable-length string that breaks any downstream comparison or length check. */
  def toHex(bytes: Array[Byte]): String =
    hexFormat.formatHex(bytes)

  /** Hashes a single string value and returns its hex digest. A thread-local
   *  [[java.security.MessageDigest]] instance is reused across calls on the same thread:
   *  [[java.security.MessageDigest]] is stateful and not thread-safe, so each thread maintains
   *  its own instance rather than allocating a new one per call. */
  def hashValue(v: String): String = {
    val digest = localDigest.get()
    digest.reset()
    digest.update(v.getBytes(StandardCharsets.UTF_8))
    toHex(digest.digest())
  }
}
