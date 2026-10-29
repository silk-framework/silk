package org.silkframework.rule.plugins.transformer.value

import org.silkframework.runtime.plugin.FixedValuesAutoCompletionProvider

import java.security.Security
import scala.jdk.CollectionConverters.IterableHasAsScala

/**
 * Provides autocomplete suggestions for hash algorithm names.
 *
 * The algorithm list is sourced from the JVM's registered security providers via [[java.security.Security#getAlgorithms]],
 * scoped to the `MessageDigest` service type. The result is JVM-dependent: different provider configurations
 * return different sets. The list is built once on first access and cached.
 */
case class HashAlgorithmAutoCompletionProvider() extends FixedValuesAutoCompletionProvider(HashAlgorithmAutoCompletionProvider.algorithms)

object HashAlgorithmAutoCompletionProvider {
  private lazy val algorithms: Seq[String] = Security.getAlgorithms("MessageDigest").asScala.toSeq.sorted
}
