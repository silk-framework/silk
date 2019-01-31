package org.silkframework.workspace.activity.transform

import org.silkframework.entity.Path
import org.silkframework.rule.vocab.{Vocabularies, Vocabulary}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}

import scala.xml.Node

/**
  * The value of the vocabulary cache.
  * Holds the target vocabularies of the transformation and suggests types and properties from it.
  */
class VocabularyCacheValue(vocabularies: Seq[Vocabulary]) extends Vocabularies(vocabularies) with MappingCandidates {
  /**
    * Suggests mapping types.
    */
  override def suggestTypes: Seq[MappingCandidate] = {
    for (vocab <- vocabularies; clazz <- vocab.classes) yield
      MappingCandidate(clazz.info.uri, 0.0)
  }.distinct

  /**
    * Suggests mapping properties.
    */
  override def suggestProperties(sourcePath: Path): Seq[MappingCandidate] = {
    for (vocab <- vocabularies; prop <- vocab.properties) yield
      MappingCandidate(prop.info.uri, 0.0)
  }.distinct
}

object VocabularyCacheValue {

  /**
    * XML serialization format for a the cache value.
    */
  implicit object ValueFormat extends XmlFormat[VocabularyCacheValue] {

    def read(node: Node)(implicit readContext: ReadContext): VocabularyCacheValue = {
      new VocabularyCacheValue(Vocabularies.VocabulariesFormat.read(node).vocabularies)
    }

    def write(desc: VocabularyCacheValue)(implicit writeContext: WriteContext[Node]): Node = {
      Vocabularies.VocabulariesFormat.write(desc)
    }
  }

}