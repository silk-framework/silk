package org.silkframework.rule.vocab

import java.util.logging.Logger

import org.silkframework.rule.vocab.GenericInfo.GenericInfoFormat
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}

import scala.xml.{Node, Null}

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
      val classMap = classes.map(c => (c.info.uri, c)).toMap.withDefault(uri => VocabularyClass(GenericInfo(uri, altLabels = Seq.empty), Seq.empty))
      for (propertyNode <- node \ "Properties" \ "VocabularyProperty") yield {
        val propertyTypeStr = (propertyNode \ "@type").headOption.map(_.text).getOrElse("")
        val propertyType = PropertyType.idToTypeMap.getOrElse(propertyTypeStr, BasePropertyType)
        VocabularyProperty(
          info = GenericInfoFormat.read((propertyNode \ INFO).head),
          domain = (propertyNode \ "@domain").headOption.map(_.text).filter(_.nonEmpty).map(classMap),
          range = (propertyNode \ "@range").headOption.map(_.text).filter(_.nonEmpty).map(classMap),
          propertyType = propertyType
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

sealed trait PropertyType {
  /** Unique ID */
  def id: String

  /** URI of this type */
  def uri: String

  /** preference, higher is better */
  def preference: Double
}

object BasePropertyType extends PropertyType {
  override val id: String = "Property"

  override def uri: String = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property"

  override def preference: Double = 1
}

object ObjectPropertyType extends PropertyType {
  override val id: String = "ObjectProperty"

  override def uri: String = "http://www.w3.org/2002/07/owl#ObjectProperty"

  override def preference: Double = 2
}

object DatatypePropertyType extends PropertyType {
  override val id: String = "DatatypeProperty"

  override def uri: String = "http://www.w3.org/2002/07/owl#DatatypeProperty"

  override def preference: Double = 3
}

object PropertyType {
  val propertyTypes = Seq(BasePropertyType, ObjectPropertyType, DatatypePropertyType)
  def idToTypeMap: Map[String, PropertyType] = {
    val seq = propertyTypes.map { t =>
      (t.id, t)
    }
    seq.toMap
  }

  def uriToTypeMap: Map[String, PropertyType] = {
    val seq = propertyTypes.map { t =>
      (t.uri, t)
    }
    seq.toMap
  }
}

case class VocabularyProperty(info: GenericInfo, propertyType: PropertyType, domain: Option[VocabularyClass], range: Option[VocabularyClass])

case class GenericInfo(uri: String, label: Option[String] = None, description: Option[String] = None, altLabels: Seq[String] = Seq.empty)

object GenericInfo {

  implicit object GenericInfoFormat extends XmlFormat[GenericInfo] {

    def read(node: Node)(implicit readContext: ReadContext): GenericInfo = {
      GenericInfo(
        uri = (node \ "@uri").text,
        label = (node \ "@label").headOption.map(_.text).filter(_.nonEmpty),
        description = (node \ "Description").headOption.map(_.text).filter(_.nonEmpty),
        altLabels = (node \ "AltLabel").map(_.text)
      )
    }

    def write(desc: GenericInfo)(implicit writeContext: WriteContext[Node]): Node = {
      <Info uri={desc.uri} label={desc.label.getOrElse("")}>
        {desc.description.map(desc => <Description>{desc}</Description>).getOrElse(Null)}
        {desc.altLabels map (l => <AltLabel>{l}</AltLabel>)}
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
      val propertyTypeStr = (propertyNode \ "@type").headOption.map(_.text).getOrElse("")
      val propertyType = PropertyType.idToTypeMap.getOrElse(propertyTypeStr, BasePropertyType)
      VocabularyProperty(
        info = GenericInfoFormat.read((propertyNode \ Vocabulary.INFO).head),
        domain = None,
        range = None,
        propertyType = propertyType
      )
    }

    override def write(prop: VocabularyProperty)(implicit writeContext: WriteContext[Node]): Node = {
      <VocabularyProperty domain={prop.domain.map(_.info.uri).getOrElse("")} range={prop.range.map(_.info.uri).getOrElse("")} type={prop.propertyType.id}>
        {GenericInfoFormat.write(prop.info)}
      </VocabularyProperty>
    }
  }
}