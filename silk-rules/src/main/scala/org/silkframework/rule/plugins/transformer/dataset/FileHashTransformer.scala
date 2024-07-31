package org.silkframework.rule.plugins.transformer.dataset

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.plugin.{AutoCompletionResult, ParamValue, PluginContext, PluginParameterAutoCompletionProvider}
import org.silkframework.runtime.resource.{Resource, ResourceCache}
import org.silkframework.workspace.WorkspaceReadTrait
import org.silkframework.workspace.resources.ResourceAutoCompletionProvider

import java.security.{MessageDigest, Security}
import scala.jdk.CollectionConverters.IterableHasAsScala

@Plugin(
  id = "fileHash",
  categories = Array("Dataset"),
  label = "File hash",
  description =
    """Calculates the hash sum of a file. The hash sum is cached so that subsequent calls to this operator are fast.
      Note that initially and everytime the specified resource has been updated, this operator might take a long time (depending on the file size).""".stripMargin
)
case class FileHashTransformer(@Param(value = "File for which the hash sum will be calculated.",
                                      autoCompletionProvider = classOf[ResourceAutoCompletionProvider], allowOnlyAutoCompletedValues = true)
                               file: Resource,
                               @Param(value = "The hash algorithm to be used.",
                                      autoCompletionProvider = classOf[HashAlgorithmAutoCompletionProvider], allowOnlyAutoCompletedValues = true)
                               algorithm: String = "SHA-1") extends Transformer {

  private val cache = new Cache

  override def referencedResources: Seq[Resource] = Seq(file)

  override def resourceUpdated(resource: Resource): Unit = {
    cache.updateNow()
  }

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    Seq(cache.value)
  }

  private class Cache extends ResourceCache[String](file) {

    private val bufferSize = 4096

    override protected def load(): String = {
      val buffer = new Array[Byte](bufferSize)
      val hashSum = MessageDigest.getInstance(algorithm)
      file.read { inputStream =>
        try {
          var numRead = 0
          while ({ numRead = inputStream.read(buffer); numRead } != -1) {
            hashSum.update(buffer, 0, numRead)
          }
        } finally {
          inputStream.close()
        }
      }
      // Convert the byte array to a hexadecimal string
      hashSum.digest().map("%02x".format(_)).mkString
    }
  }
}

/**
 * Auto-completion for project resources.
 */
case class HashAlgorithmAutoCompletionProvider() extends PluginParameterAutoCompletionProvider {

  private lazy val algorithms = {
    Security.getAlgorithms("MessageDigest").asScala.toSeq
  }

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
                           (implicit context: PluginContext): Option[String] = {
    None
  }
}

