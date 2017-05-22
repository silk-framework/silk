package org.silkframework.rule.vocab

import org.silkframework.rule.vocab.GenericInfo.GenericInfoFormat
import org.silkframework.rule.vocab.Vocabularies.VocabulariesFormat
import org.silkframework.rule.vocab.Vocabulary.VocabularyFormat
import org.silkframework.rule.vocab.VocabularyProperty.VocabularyPropertyXmlFormat
import org.silkframework.runtime.plugin.PluginModule

class VocabularyPlugins extends PluginModule {

  override def pluginClasses: Seq[Class[_]] = Seq(VocabulariesFormat.getClass, VocabularyFormat.getClass, GenericInfoFormat.getClass) ++ serializers

  private def serializers = VocabularyPropertyXmlFormat.getClass :: Nil

}
