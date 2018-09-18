package org.silkframework.rule.vocab

import org.silkframework.config.DefaultConfig
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.util.Identifier

trait VocabularyManager {

  def get(uri: String, project: Identifier)(implicit userContext: UserContext): Vocabulary

}

object VocabularyManager {
  private var lastPlugin: String = ""
  private var vocabularyManager: Option[VocabularyManager] = None

  private def instance = this.synchronized {
    val plugin = DefaultConfig.instance().getString("vocabulary.manager.plugin")
    if(plugin != lastPlugin || vocabularyManager.isEmpty) {
      vocabularyManager = Some(PluginRegistry.createFromConfig[VocabularyManager]("vocabulary.manager"))
      lastPlugin = plugin
    }
    vocabularyManager.get
  }

  def apply(): VocabularyManager = instance

}
