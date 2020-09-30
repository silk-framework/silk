package org.silkframework.dataset

import java.text.NumberFormat
import java.util.Locale

import org.silkframework.config.DefaultConfig
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.Resource

import scala.util.control.NonFatal

/**
  * A dataset extension that allows to retrieve example values for a specific path quickly.
  */
trait PeakDataSource {
  this: DataSource =>
  final val MAX_SIZE_CONFIG_KEY = "mapping.preview.max.file.size.kb"
  private val maxFileSizeForPeak = DefaultConfig.instance().getInt(MAX_SIZE_CONFIG_KEY)

  /** Default peak implementation that should work with all sources that offer fast "random access".
    * It filters entities that have no input value for any input path. */
  def peak(entitySchema: EntitySchema, limit: Int)
          (implicit userContext: UserContext): Traversable[Entity] = {
    try {
      retrieve(entitySchema, Some(limit)).entities
    } catch {
      case NonFatal(ex) =>
        throw PeakException("Cannot retrieve values. Reason: " + ex.getMessage, Some(ex))
    }
  }

  protected def peakWithMaximumFileSize(inputResource: Resource,
                                        entitySchema: EntitySchema,
                                        limit: Int)
                                       (implicit userContext: UserContext): Traversable[Entity] = {
    inputResource.size match {
      case Some(size) =>
        if (size < maxFileSizeForPeak * 1000) {
          retrieve(entitySchema, Some(limit)).entities
        } else {
          throw PeakException(s"The input file size of ${NumberFormat.getNumberInstance(Locale.US).format(size)} bytes was larger than the maximal allowed value of $maxFileSizeForPeak kB! " +
              s"Increase the config parameter $MAX_SIZE_CONFIG_KEY appropriately.")
        }
      case None =>
        throw PeakException(s"Max file size ($MAX_SIZE_CONFIG_KEY) applies for this data source, but could not find out " +
            s"the size of resource ${inputResource.name}!")
    }
  }
}

case class PeakException(msg: String, cause: Option[Throwable] = None) extends RuntimeException(msg, cause.orNull)