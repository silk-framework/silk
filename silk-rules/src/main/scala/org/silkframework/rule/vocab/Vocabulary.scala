package org.silkframework.rule.vocab

import org.silkframework.rule.vocab.Info.InfoFormat
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import scala.xml.Node

case class Vocabulary(info: Info, classes: Traversable[VocabularyClass], properties: Traversable[VocabularyProperty])

object Vocabulary {

  /**
    * XML serialization format for a sequence of vocabularies.
    */
  implicit object VocabularySeqFormat extends XmlFormat[Seq[Vocabulary]] {

    def read(node: Node)(implicit readContext: ReadContext) = {
      for(vocabNode <- node \ "Vocabulary") yield
        VocabularyFormat.read(vocabNode)
    }

    def write(desc: Seq[Vocabulary])(implicit writeContext: WriteContext[Node]): Node = {
      <Vocabularies>{
        for(vocabulary <- desc) yield {
          VocabularyFormat.write(vocabulary)
        }
      }</Vocabularies>
    }
  }

  /**
    * XML serialization format for vocabularies.
    */
  implicit object VocabularyFormat extends XmlFormat[Vocabulary] {

    def read(node: Node)(implicit readContext: ReadContext) = {
      val classes = readClasses(node)
      val properties = readProperties(node, classes)

      Vocabulary(
        info = InfoFormat.read((node \ "Info").head),
        classes = classes,
        properties = properties
      )
    }

    def readClasses(node: Node)(implicit readContext: ReadContext) = {
      for(classNode <- node \ "Classes" \ "Class") yield {
        VocabularyClass(InfoFormat.read((node \ "Info").head))
      }
    }

    def readProperties(node: Node, classes: Seq[VocabularyClass])(implicit readContext: ReadContext) = {
      val classMap = classes.map(c => (c.info.uri, c)).toMap
      for(propertyNode <- node \ "Properties" \ "Property") yield {
        VocabularyProperty(
          info = InfoFormat.read((node \ "Info").head),
          domain = (node \ "@domain").headOption.map(_.text).filter(_.nonEmpty).map(classMap),
          range = (node \ "@range").headOption.map(_.text).filter(_.nonEmpty).map(classMap)
        )
      }
    }

    def write(desc: Vocabulary)(implicit writeContext: WriteContext[Node]): Node = {
      <Vocabulary>
        ( InfoFormat.write(desc.info) )
        <Classes>{
          for(cl <- desc.classes) yield {
            <Class>
              { InfoFormat.write(cl.info) }
            </Class>
          }
        }</Classes>
        <Properties>{
          for(prop <- desc.properties) yield {
            <Property domain={prop.domain.map(_.info.uri).getOrElse("")} range={prop.range.map(_.info.uri).getOrElse("")} >
              { InfoFormat.write(prop.info) }
            </Property>
          }
          }</Properties>
      </Vocabulary>
    }
  }
}

case class VocabularyClass(info: Info)

case class VocabularyProperty(info: Info, domain: Option[VocabularyClass], range: Option[VocabularyClass])

case class Info(uri: String, label: String, description: String)

object Info {

  implicit object InfoFormat extends XmlFormat[Info] {

    def read(node: Node)(implicit readContext: ReadContext) = {
      Info(
        uri = (node \ "@uri").text,
        label = (node \ "@label").text,
        description = node.text.trim
      )
    }

    def write(desc: Info)(implicit writeContext: WriteContext[Node]): Node = {
      <Info uri={desc.uri} label={desc.label}>
        {desc}
      </Info>
    }
  }

}
