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

  /**
    * Returns the first class with the given URI in any vocabulary.
    */
  def findClass(uri: String): Option[VocabularyClass] = {
    vocabularies.flatMap(_.getClass(uri)).headOption
  }

  /**
    * Returns the first property with the given URI in any vocabulary.
    */
  def findProperty(uri: String): Option[VocabularyProperty] = {
    vocabularies.flatMap(_.getProperty(uri)).headOption
  }

  def forwardPropertiesOfClass(classUri: String): Seq[VocabularyProperty] = {
    val searchFor = Some(classUri)
    vocabularies.flatMap(_.properties.filter(_.domain == searchFor))
  }

  def backwardPropertiesOfClass(classUri: String): Seq[VocabularyProperty] = {
    val searchFor = Some(classUri)
    vocabularies.flatMap(_.properties.filter(_.range == searchFor))
  }
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


