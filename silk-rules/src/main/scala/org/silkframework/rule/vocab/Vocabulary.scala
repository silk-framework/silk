package org.silkframework.rule.vocab

import org.silkframework.rule.vocab.GenericInfo.InfoFormat
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import scala.xml.Node

case class Vocabulary(info: GenericInfo, classes: Traversable[VocabularyClass], properties: Traversable[VocabularyProperty]) {

  def getClass(uri: String): Option[VocabularyClass] = {
    classes.find(_.info.uri == uri)
  }

  def getProperty(uri: String): Option[VocabularyProperty] = {
    properties.find(_.info.uri == uri)
  }

}

object Vocabulary {

  /**
    * XML serialization format for vocabularies.
    */
  implicit object VocabularyFormat extends XmlFormat[Vocabulary] {
    final val INFO: String = "Info"

    def read(node: Node)(implicit readContext: ReadContext): Vocabulary = {
      val classes = readClasses(node)
      val properties = readProperties(node, classes)

      Vocabulary(
        info = InfoFormat.read((node \ INFO).head),
        classes = classes,
        properties = properties
      )
    }

    def readClasses(node: Node)(implicit readContext: ReadContext): Seq[VocabularyClass] = {
      for(classNode <- node \ "Classes" \ "Class") yield {
        val parentClasses = (classNode \ "ParentClasses" \ "Uri").map(_.text)
        VocabularyClass(InfoFormat.read((classNode \ INFO).head), parentClasses)
      }
    }

    def readProperties(node: Node, classes: Seq[VocabularyClass])(implicit readContext: ReadContext): Seq[VocabularyProperty] = {
      val classMap = classes.map(c => (c.info.uri, c)).toMap.withDefault(uri => VocabularyClass(GenericInfo(uri), Seq.empty))
      for(propertyNode <- node \ "Properties" \ "Property") yield {
        VocabularyProperty(
          info = InfoFormat.read((propertyNode \ INFO).head),
          domain = (propertyNode \ "@domain").headOption.map(_.text).filter(_.nonEmpty).map(classMap),
          range = (propertyNode \ "@range").headOption.map(_.text).filter(_.nonEmpty).map(classMap)
        )
      }
    }

    def write(desc: Vocabulary)(implicit writeContext: WriteContext[Node]): Node = {
      <Vocabulary>
        { InfoFormat.write(desc.info) }
        <Classes>{
          for(cl <- desc.classes) yield {
            <Class>
              { InfoFormat.write(cl.info) }
              <ParentClasses>
                { for(parentClassURI <- cl.parentClasses) yield {
                    <Uri>{parentClassURI}</Uri>
                  }
                }
              </ParentClasses>
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

case class VocabularyClass(info: GenericInfo, parentClasses: Traversable[String])

case class VocabularyProperty(info: GenericInfo, domain: Option[VocabularyClass], range: Option[VocabularyClass])

case class GenericInfo(uri: String, label: Option[String] = None, description: Option[String] = None)

object GenericInfo {

  implicit object InfoFormat extends XmlFormat[GenericInfo] {

    def read(node: Node)(implicit readContext: ReadContext): GenericInfo = {
      GenericInfo(
        uri = (node \ "@uri").text,
        label = (node \ "@label").headOption.filter(_.nonEmpty).map(_.text),
        description = if(node.text.trim.nonEmpty) Some(node.text.trim) else None
      )
    }

    def write(desc: GenericInfo)(implicit writeContext: WriteContext[Node]): Node = {
      <Info uri={desc.uri} label={desc.label.getOrElse("")}>
        {desc.description.getOrElse("")}
      </Info>
    }
  }

}
