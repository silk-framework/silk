package org.silkframework.plugins.path

import org.silkframework.config.Prefixes
import org.silkframework.entity.paths.TypedPath
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.AnyPlugin
import play.api.libs.json.{Format, Json}

/** (Maybe temporary solution) Plugin to resolve various meta data of Silk paths that are otherwise not available. */
trait PathMetaDataPlugin[T] extends AnyPlugin {
  /** The plugin ID of the data source/task the paths originated from and this path meta data plugin can then be applied to. */
  def sourcePluginClass: Class[T]

  /** Fetch path meta data.
    *
    * @param sourcePlugin      The (dataset) plugin this plugin is able to fetch path meta data for.
    * @param paths             The paths for that meta data should be fetched.
    * @param preferredLanguage Language tag for the preferred language that, e.g. the label should be in.
    * @return Path meta data for each input path. None if no meta data could be fetched.
    */
  def fetchMetaData(sourcePlugin: T,
                    paths: Traversable[TypedPath],
                    preferredLanguage: String)
                   (implicit userContext: UserContext, prefixes: Prefixes): Traversable[PathMetaData]
}

/** The path meta data.
  *
  * @param value    The actual path as Silk path expression.
  * @param label    The path label.
  * @param valueType Human-readable type of the path's values.
  */
case class PathMetaData(value: String, label: Option[String], valueType: String)

object PathMetaData {
  implicit val pathMetaDataFormat: Format[PathMetaData] = Json.format[PathMetaData]
}