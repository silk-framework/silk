package org.silkframework.plugins.path
import org.silkframework.config.Prefixes
import org.silkframework.entity.paths.TypedPath
import org.silkframework.runtime.activity.UserContext

/** Default meta data plugin that is used if none is matching. */
case class StandardMetaDataPlugin() extends PathMetaDataPlugin[StandardMetaDataPlugin] {
  /** This returns nothing meaningful, since it should not be used to choose this plugin. */
  override def sourcePluginClass: Class[StandardMetaDataPlugin] = classOf[StandardMetaDataPlugin]

  /** Fetch path meta data.
    *
    * @param sourcePlugin      The (dataset) plugin this plugin is able to fetch path meta data for.
    * @param paths             The paths for that meta data should be fetched.
    * @param preferredLanguage Language tag for the preferred language that, e.g. the label should be in.
    * @return Path meta data for each input path. None if no meta data could be fetched.
    */
  override def fetchMetaData(sourcePlugin: StandardMetaDataPlugin,
                             paths: Traversable[TypedPath],
                             preferredLanguage: String)
                            (implicit userContext: UserContext, prefixes: Prefixes): Traversable[PathMetaData] = {
    paths.map(p => PathMetaData(p.serialize(), None, p.valueType.label))
  }
}
