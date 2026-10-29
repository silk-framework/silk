package org.silkframework.workspace.activity.vocabulary

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.PluginType
import org.silkframework.runtime.plugin.{AnyPlugin, ParameterValues, PluginContext, PluginRegistry}

/** Optional plugin for enriching vocabulary lookup results with localized labels. */
@PluginType()
trait VocabularyLookupLabelPlugin extends AnyPlugin {
  /** Returns localized labels for the provided absolute resolved URIs. */
  def labels(absoluteUris: Seq[String],
             preferredLanguage: String)
            (implicit userContext: UserContext): Map[String, String]
}

object VocabularyLookupLabelPlugin {
  implicit private val pluginContext: PluginContext = PluginContext.empty

  /** All registered vocabulary lookup label plugins. */
  def plugins: Seq[VocabularyLookupLabelPlugin] = {
    PluginRegistry.availablePlugins[VocabularyLookupLabelPlugin].map { pluginDescription =>
      pluginDescription(ParameterValues.empty)
    }
  }
}
