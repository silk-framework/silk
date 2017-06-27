package org.silkframework.rule.vocab

import java.util.logging.Logger

import org.silkframework.rule.vocab.GenericInfo.GenericInfoFormat
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
  final val INFO: String = "Info"
  /**
    * XML serialization format for vocabularies.
    */
  implicit object VocabularyFormat extends XmlFormat[Vocabulary] {

    def read(node: Node)(implicit readContext: ReadContext): Vocabulary = {
      val classes = readClasses(node)
      val properties = readProperties(node, classes)

      Vocabulary(
        info = GenericInfoFormat.read((node \ INFO).head),
        classes = classes,
        properties = properties
      )
    }

    def readClasses(node: Node)(implicit readContext: ReadContext): Seq[VocabularyClass] = {
      for (classNode <- node \ "Classes" \ "VocabularyClass") yield {
        VocabularyClass.VocabularyClassXmlFormat.read(classNode)
      }
    }

    def readProperties(node: Node, classes: Seq[VocabularyClass])(implicit readContext: ReadContext): Seq[VocabularyProperty] = {
      val classMap = classes.map(c => (c.info.uri, c)).toMap.withDefault(uri => VocabularyClass(GenericInfo(uri), Seq.empty))
      for (propertyNode <- node \ "Properties" \ "VocabularyProperty") yield {
        VocabularyProperty(
          info = GenericInfoFormat.read((propertyNode \ INFO).head),
          domain = (propertyNode \ "@domain").headOption.map(_.text).filter(_.nonEmpty).map(classMap),
          range = (propertyNode \ "@range").headOption.map(_.text).filter(_.nonEmpty).map(classMap)
        )
      }
    }

    def write(desc: Vocabulary)(implicit writeContext: WriteContext[Node]): Node = {
      <Vocabulary>
        {GenericInfoFormat.write(desc.info)}<Classes>
        {for (cl <- desc.classes) yield {
          {
            VocabularyClass.VocabularyClassXmlFormat.write(cl)
          }
        }}
      </Classes>
        <Properties>
          {for (prop <- desc.properties) yield {
          VocabularyProperty.VocabularyPropertyXmlFormat.write(prop)
        }}
        </Properties>
      </Vocabulary>
    }
  }
}

case class VocabularyClass(info: GenericInfo, parentClasses: Traversable[String])

case class VocabularyProperty(info: GenericInfo, domain: Option[VocabularyClass], range: Option[VocabularyClass])

case class GenericInfo(uri: String, label: Option[String] = None, description: Option[String] = None)

object GenericInfo {

  implicit object GenericInfoFormat extends XmlFormat[GenericInfo] {

    def read(node: Node)(implicit readContext: ReadContext): GenericInfo = {
      GenericInfo(
        uri = (node \ "@uri").text,
        label = (node \ "@label").headOption.map(_.text).filter(_.nonEmpty),
        description = if (node.text.trim.nonEmpty) Some(node.text.trim) else None
      )
    }

    def write(desc: GenericInfo)(implicit writeContext: WriteContext[Node]): Node = {
      <Info uri={desc.uri} label={desc.label.getOrElse("")}>
        {desc.description.getOrElse("")}
      </Info>
    }
  }

}

object VocabularyClass {

  implicit object VocabularyClassXmlFormat extends XmlFormat[VocabularyClass] {
    override def read(classNode: Node)(implicit readContext: ReadContext): VocabularyClass = {
      val parentClasses = (classNode \ "ParentClasses" \ "Uri").map(_.text)
      VocabularyClass(GenericInfoFormat.read((classNode \ Vocabulary.INFO).head), parentClasses)
    }

    override def write(value: VocabularyClass)(implicit writeContext: WriteContext[Node]): Node = {
      <VocabularyClass>
        {GenericInfoFormat.write(value.info)}
        <ParentClasses>
        {for (parentClassURI <- value.parentClasses) yield {
          <Uri>{parentClassURI}</Uri>
        }}
        </ParentClasses>
      </VocabularyClass>
    }
  }
}

object VocabularyProperty {
  private val log: Logger = Logger.getLogger(this.getClass.getName)
  private var loggedWarning = false

  implicit object VocabularyPropertyXmlFormat extends XmlFormat[VocabularyProperty] {
    override def read(propertyNode: Node)(implicit readContext: ReadContext): VocabularyProperty = {
      if(!loggedWarning) {
        log.warning("De-serializing VocabularyProperty object with default XML format. This is not safe to do, because domain and range are discarded! " +
        "This warning will not be repeated again.")
        loggedWarning = true
      }
      VocabularyProperty(
        info = GenericInfoFormat.read((propertyNode \ Vocabulary.INFO).head),
        domain = None,
        range = None
      )
    }

    override def write(prop: VocabularyProperty)(implicit writeContext: WriteContext[Node]): Node = {
      <VocabularyProperty domain={prop.domain.map(_.info.uri).getOrElse("")} range={prop.range.map(_.info.uri).getOrElse("")}>
        {GenericInfoFormat.write(prop.info)}
      </VocabularyProperty>
    }
  }
}