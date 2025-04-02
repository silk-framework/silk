package org.silkframework.rule.vocab

import org.silkframework.config.DefaultConfig
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.PluginType
import org.silkframework.runtime.plugin.{AnyPlugin, PluginContext, PluginRegistry}
import org.silkframework.util.Identifier

import java.util.logging.Logger

@PluginType()
trait VocabularyManager extends AnyPlugin {

  def get(uri: String, project: Option[Identifier])(implicit userContext: UserContext): Option[Vocabulary]

  /** Retrieves a list of globally accessible vocabularies.
    * Returns None if this implementation cannot return a list of global vocabularies.
    **/
  def retrieveGlobalVocabularies()(implicit userContext: UserContext): Option[Iterable[String]]

}

object VocabularyManager {
  private var lastPlugin: String = ""
  private var vocabularyManager: Option[VocabularyManager] = None
  private val log: Logger = Logger.getLogger(getClass.getName)

  private def instance: VocabularyManager = this.synchronized {
    implicit val context: PluginContext = PluginContext.empty
    val plugin = DefaultConfig.instance().getString("vocabulary.manager.plugin")
    if(plugin != lastPlugin || vocabularyManager.isEmpty) {
      log.info("Using configured VocabularyManager plugin: " + plugin)
      vocabularyManager = Some(PluginRegistry.createFromConfig[VocabularyManager]("vocabulary.manager"))
      lastPlugin = plugin
    }
    vocabularyManager.get
  }

  def apply(): VocabularyManager = instance

}
