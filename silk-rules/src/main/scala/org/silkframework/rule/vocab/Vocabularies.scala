package org.silkframework.rule.vocab

import org.silkframework.rule.vocab.Vocabulary.VocabularyFormat
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import scala.xml.Node
import language.implicitConversions

/**
  * Holds multiple vocabularies.
  */
case class Vocabularies(vocabularies: Seq[Vocabulary]) {

  def isEmpty: Boolean = vocabularies.isEmpty

}

object Vocabularies {

  def empty = Vocabularies(Seq.empty)

  implicit def vocabulariesSequence(v: Vocabularies): Seq[Vocabulary] = v.vocabularies

  /**
    * XML serialization format for a sequence of vocabularies.
    */
  implicit object VocabulariesFormat extends XmlFormat[Vocabularies] {

    def read(node: Node)(implicit readContext: ReadContext) = {
      Vocabularies(
        for(vocabNode <- node \ "Vocabulary") yield
          VocabularyFormat.read(vocabNode)
      )
    }

    def write(desc: Vocabularies)(implicit writeContext: WriteContext[Node]): Node = {
      <Vocabularies>{
        for(vocabulary <- desc.vocabularies) yield {
          VocabularyFormat.write(vocabulary)
        }
        }</Vocabularies>
    }
  }

}


