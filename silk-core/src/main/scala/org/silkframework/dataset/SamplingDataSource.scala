package org.silkframework.dataset

import org.silkframework.entity.paths.TypedPath
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Uri

/**
  * A data source with fast value sampling method.
  * Useful as alternative to the retrieve method that returns full entities.
  */
trait SamplingDataSource { this: DataSource =>
  /** Sample values from a data source.
    * Returns a Traversable for each typed path.
    *
    * @param typeUri          An optional type URI to restrict the resources.
    * @param typedPaths       the paths to fetch sample values for
    * @param valueSampleLimit The maximum number of values
    * */
  def sampleValues(typeUri: Option[Uri],
                   typedPaths: Seq[TypedPath],
                   valueSampleLimit: Option[Int])
                  (implicit userContext: UserContext): Seq[Traversable[String]]
}
