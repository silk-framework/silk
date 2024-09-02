package org.silkframework.rule.plugins.transformer.dataset

import org.silkframework.dataset.{DatasetSpec, ResourceBasedDataset}
import org.silkframework.rule.TaskContext
import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.plugin.types.ResourceOption
import org.silkframework.runtime.plugin.{AutoCompletionResult, ParamValue, PluginContext, PluginParameterAutoCompletionProvider}
import org.silkframework.runtime.resource.{Resource, ResourceCache}
import org.silkframework.runtime.validation.ValidationException
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
      Note that initially and everytime the specified resource has been updated, this operator might take a long time (depending on the file size)."""
)
case class FileHashTransformer(@Param(value = "File for which the hash sum will be calculated. If left empty, the file of the input dataset is used.",
                                      autoCompletionProvider = classOf[ResourceAutoCompletionProvider], allowOnlyAutoCompletedValues = true)
                               file: ResourceOption,
                               @Param(value = "The hash algorithm to be used.",
                                      autoCompletionProvider = classOf[HashAlgorithmAutoCompletionProvider], allowOnlyAutoCompletedValues = true)
                               algorithm: String = "SHA-1") extends Transformer {

  private val cache = file.resource.map(new Cache(_, algorithm))

  override def withContext(taskContext: TaskContext): Transformer = {
    taskContext.inputTasks.headOption.map(_.data) match {
      case Some(DatasetSpec(ds: ResourceBasedDataset, _, _)) if file.isEmpty =>
        FileHashTransformer(Some(ds.file), algorithm)
      case _ =>
        this
    }
  }

  override def referencedResources: Seq[Resource] = file.resource.toSeq

  override def resourceUpdated(resource: Resource): Unit = {
    cache.foreach(_.updateNow())
  }

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    cache match {
      case Some(c) =>
        Seq(c.value)
      case None =>
        throw new ValidationException("No resource configured and no resource supplied by input dataset.")
    }

  }
}

private class Cache(file: Resource, algorithm: String) extends ResourceCache[String](file) {

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

