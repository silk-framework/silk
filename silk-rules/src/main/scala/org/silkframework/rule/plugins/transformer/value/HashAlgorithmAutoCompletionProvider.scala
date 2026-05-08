package org.silkframework.rule.plugins.transformer.value

import org.silkframework.runtime.plugin.{AutoCompletionResult, ParamValue, PluginContext, PluginParameterAutoCompletionProvider}
import org.silkframework.workspace.WorkspaceReadTrait

import java.security.Security
import scala.jdk.CollectionConverters.IterableHasAsScala

/**
 * Provides autocomplete suggestions for hash algorithm names.
 *
 * The algorithm list is sourced from the JVM's registered security providers via [[java.security.Security#getAlgorithms]],
 * scoped to the `MessageDigest` service type. The result is JVM-dependent — different provider configurations
 * return different sets. The list is built once on first access and cached.
 */
case class HashAlgorithmAutoCompletionProvider() extends PluginParameterAutoCompletionProvider {

  private lazy val algorithms = Security.getAlgorithms("MessageDigest").asScala.toSeq

  override def autoComplete(searchQuery: String, dependOnParameterValues: Seq[ParamValue],
                            workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Iterable[AutoCompletionResult] = {
    val multiSearchWords = extractSearchTerms(searchQuery)
    algorithms
      .filter(r => matchesSearchTerm(multiSearchWords, r.toLowerCase))
      .map(r => AutoCompletionResult(r, None))
  }

  override def valueToLabel(value: String, dependOnParameterValues: Seq[ParamValue],
                            workspace: WorkspaceReadTrait)
                           (implicit context: PluginContext): Option[String] = None
}
