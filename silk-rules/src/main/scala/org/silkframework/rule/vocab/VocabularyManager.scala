package org.silkframework.rule.vocab

import org.silkframework.runtime.plugin.PluginRegistry

trait VocabularyManager {

  def get(uri: String): Vocabulary

}

object VocabularyManager {

  private lazy val instance = {
    PluginRegistry.createFromConfig[VocabularyManager]("vocabulary.manager")
  }

  def apply() = instance

}
