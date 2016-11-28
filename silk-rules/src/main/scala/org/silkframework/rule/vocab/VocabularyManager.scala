package org.silkframework.rule.vocab

import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.util.Identifier

trait VocabularyManager {

  def get(uri: String, project: Identifier): Vocabulary

}

object VocabularyManager {

  private lazy val instance = {
    PluginRegistry.createFromConfig[VocabularyManager]("vocabulary.manager")
  }

  def apply() = instance

}
