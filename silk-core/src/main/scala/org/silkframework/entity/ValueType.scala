package org.silkframework.entity


import javax.xml.datatype.{DatatypeConstants, DatatypeFactory, XMLGregorianCalendar}
import org.silkframework.config.Prefixes
import org.silkframework.entity.ValueType.XSD
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.util.Uri

import scala.util.Try
import scala.util.matching.Regex
import scala.xml.Node

/**
  * The type of a value.
  * (Atomic Types)
  */
sealed trait ValueType {

  /** The unique ID of this value type. This will be used for serialization and deserialization */
  def id: String

  /**
    * A human-readable label for this type.
    */
  def label: String

  /** returns true if the lexical string is a representation of this type */
  def validate(lexicalString: String): Boolean

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  def uri: Option[String]

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  def ordering: Ordering[String]

  /**
    * Extends equals to return true if either one of the comparators is an UntypedValueType
    * @param vt - the other ValueType
    */
  def equalsOrIndifferentTo(vt: ValueType): Boolean = {
    vt match{
      case UntypedValueType => true
      case _ if this == UntypedValueType => true
      case v => v == this
    }
  }
}

object ValueType {
  final val XSD = "http://www.w3.org/2001/XMLSchema#"
  final val CUSTOM_VALUE_TYPE = "CustomValueType"
  final val LANGUAGE_VALUE_TYPE = "LanguageValueType"
  final val OUTDATED_AUTO_DETECT = "AutoDetectValueType"

  val DefaultOrdering: Ordering[String] = Ordering.String
  val GregorianCalendarOrdering: Ordering[XMLGregorianCalendar] = Ordering.fromLessThan[XMLGregorianCalendar]((date1: XMLGregorianCalendar, date2: XMLGregorianCalendar) =>{
    date1.compare(date2) < 0
  })

  implicit object ValueTypeFormat extends XmlFormat[ValueType] {
    /**
      * Deserializes a value.
      */
    override def read(value: Node)(implicit readContext: ReadContext): ValueType = {
      (value \ "@nodeType").headOption match {
        case Some(nodeTypeNode) =>
          val nodeType = nodeTypeNode.text.trim
          valueTypeById(nodeType) match {
            case Left(_) =>
              readClassValueTypes(value, nodeType, readContext.prefixes)
            case Right(valueType) =>
              valueType
          }
        case None =>
          throw new IllegalArgumentException("@nodeType not existing in node")
      }
    }

    /**
      * Serializes a value.
      */
    override def write(value: ValueType)(implicit writeContext: WriteContext[Node]): Node = {
      val typeId = valueTypeId(value)
      value match {
        case CustomValueType(typeUri) => <ValueType nodeType={typeId} uri={typeUri}/>
        case LanguageValueType(lang) => <ValueType nodeType={typeId} lang={lang}/>
        case objValueType: ValueType => <ValueType nodeType={typeId}/>
      }
    }
  }

  def valueTypeById(valueTypeId: String): Either[Class[_], ValueType] = {
    // The stripping of '$' at the end is for being backward compatible to older project data
    valueTypeMapByStringId.get(valueTypeId.stripSuffix("$")) match {
      case None =>
        throw new IllegalArgumentException("Invalid value type ID: " + valueTypeId + ". Valid values: " +
            valueTypeMapByStringId.keys.toSeq.sortWith(_ < _).mkString(", "))
      case Some(vt) => vt
    }
  }

  def valueTypeId(valueType: ValueType): String = {
    valueTypeIdMapByClass.get(valueType.getClass) match {
      case Some(valueTypeId) =>
        valueTypeId
      case None =>
        throw new RuntimeException("ValueType serialization for " + valueType.getClass + " is not supported!")
    }
  }

  // Handle the ValueType class cases
  private def readClassValueTypes(value: Node,
                                  nodeType: String,
                                  prefixes: Prefixes): ValueType = {
    nodeType.replace("$", "") match {
      case OUTDATED_AUTO_DETECT => UntypedValueType
      case CUSTOM_VALUE_TYPE =>
        (value \ "@uri").headOption match {
          case Some(typeUri) =>
            val uri = Uri.parse(typeUri.text.trim, prefixes)
            CustomValueType(uri.uri)
          case None =>
            throw new IllegalArgumentException("'uri' attribute not existing in node")
        }
      case LANGUAGE_VALUE_TYPE =>
        (value \ "@lang").headOption match {
          case Some(lang) =>
            LanguageValueType(lang.text.trim)
          case None =>
            throw new IllegalArgumentException("'lang' attribute not existing in node")
        }
    }
  }

  /** All [[ValueType]] classes/singletons */
  val allValueType: Seq[Either[(String, Class[_ <: ValueType]), ValueType]] = Seq(
    Left((CUSTOM_VALUE_TYPE, classOf[CustomValueType])),
    Left((LANGUAGE_VALUE_TYPE, classOf[LanguageValueType])),
    // this type string is a left over from the previous name of UntypedValueType.
    // Since many project configs in tests still feature the old type, this is a valid workaround.
    Left((OUTDATED_AUTO_DETECT, UntypedValueType.getClass.asInstanceOf[Class[_ <: ValueType]])),
    Right(IntValueType),
    Right(LongValueType),
    Right(StringValueType),
    Right(FloatValueType),
    Right(DoubleValueType),
    Right(BooleanValueType),
    Right(IntegerValueType),
    Right(UriValueType),
    Right(UntypedValueType),
    Right(BlankNodeValueType),
    Right(DateValueType),
    Right(DateTimeValueType)
  )

  val valueTypeMapByStringId: Map[String, Either[Class[_], ValueType]] = allValueType.map {
    case Left((id, clazz)) => (id, Left(clazz))
    case Right(obj) => (obj.id, Right(obj))
  }.toMap

  val valueTypeIdMapByClass: Map[Class[_], String] = valueTypeMapByStringId.filterNot(x => x._1 == OUTDATED_AUTO_DETECT).map(ei =>
    ei._2 match {
      case Left(clazz) => (clazz, ei._1)
      case Right(obj) => (obj.getClass, ei._1)
    }
  ).toMap
}

/**
  * If this value type is set, then the values can be transformed to any valid value that can be inferred from the
  * lexical form, e.g. "1" can be an Int, but also a String.
  */
case object UntypedValueType extends ValueType with Serializable {

  override def label = "Untyped"

  /** returns true if the lexical string is a representation of this type */
  override def validate(lexicalString: String): Boolean = true

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = None

  override def id: String = "UntypedValueType"

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = ValueType.DefaultOrdering
}

/** A custom type that is used for all types not covered by any other types. */
case class CustomValueType(typeUri: String) extends ValueType {

  override def label = "Custom Type"

  override def validate(lexicalString: String): Boolean = {
    true // No validation for custom types
  }

  override def uri: Option[String] = Some(typeUri)

  override def id: String = ValueType.CUSTOM_VALUE_TYPE

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = ValueType.DefaultOrdering
}

/** Represents language tagged strings. */
case class LanguageValueType(language: String) extends ValueType {

  override def label: String = "@" + language

  override def validate(lexicalString: String): Boolean = true // No validation needed

  override def uri: Option[String] = None // These are always strings

  override def id: String = ValueType.LANGUAGE_VALUE_TYPE

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = ValueType.DefaultOrdering
}

case object IntValueType extends ValueType with Serializable {

  override def label = "Int"

  override def validate(lexicalString: String): Boolean = {
    Try(lexicalString.toInt).isSuccess
  }

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = Some(XSD + "int")

  override def id: String = "IntValueType"

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = Ordering.by((str: String) => str.toInt)
}

case object LongValueType extends ValueType with Serializable {

  override def label = "Long"

  override def validate(lexicalString: String): Boolean = {
    Try(lexicalString.toLong).isSuccess
  }

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = Some(XSD + "long")

  override def id: String = "LongValueType"

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = Ordering.by((str: String) => str.toLong)
}

case object StringValueType extends ValueType with Serializable {

  override def label = "String"

  /** returns true if the lexical string is a representation of this type */
  override def validate(lexicalString: String): Boolean = true // Always true

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = Some(XSD + "string") // In RDF this can be omitted

  override def id: String = "StringValueType"

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = ValueType.DefaultOrdering
}

case object FloatValueType extends ValueType with Serializable {

  override def label = "Float"

  override def validate(lexicalString: String): Boolean = {
    Try(lexicalString.toFloat).isSuccess
  }

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = Some(XSD + "float")

  override def id: String = "FloatValueType"

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = Ordering.by((str: String) => str.toFloat)
}

case object DoubleValueType extends ValueType with Serializable {

  override def label = "Double"

  override def validate(lexicalString: String): Boolean = {
    Try(lexicalString.toDouble).isSuccess
  }

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = Some(XSD + "double")

  override def id: String = "DoubleValueType"

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = Ordering.by((str: String) => str.toDouble)
}

case object BooleanValueType extends ValueType with Serializable {

  override def label = "Boolean"

  override def validate(lexicalString: String): Boolean = {
    Try(lexicalString.toBoolean).isSuccess
  }

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = Some(XSD + "boolean")

  override def id: String = "BooleanValueType"

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = Ordering.by((str: String) => str.toBoolean)
}

case object IntegerValueType extends ValueType with Serializable {

  override def label = "Integer"

  val integerRegex: Regex = """^[+-]?(([1-9][0-9]*)|(0))$""".r

  override def validate(lexicalString: String): Boolean = {
    integerRegex.findFirstMatchIn(lexicalString).isDefined
  }

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = Some(XSD + "integer")

  override def id: String = "IntegerValueType"

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = Ordering.by((str: String) => str.toInt)
}

case object UriValueType extends ValueType with Serializable {

  override def label = "Uri"

  override def validate(lexicalString: String): Boolean = {
    new Uri(lexicalString).isValidUri
  }

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = None

  override def id: String = "UriValueType"

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = ValueType.DefaultOrdering
}

case object BlankNodeValueType extends ValueType with Serializable {

  override def label = "Blank Node"

  override def validate(lexicalString: String): Boolean = true // FIXME: No blank node lexical validation

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = None

  override def id: String = "BlankNodeValueType"

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = ValueType.DefaultOrdering
}

case object DateValueType extends ValueType with Serializable {

  @transient lazy private val datatypeFactory = DatatypeFactory.newInstance()

  override def label = "Date"

  override def validate(lexicalString: String): Boolean = {
    try {
      val date = datatypeFactory.newXMLGregorianCalendar(lexicalString)
      date.getXMLSchemaType match {
        case DatatypeConstants.DATE => true
        case DatatypeConstants.GYEARMONTH => true
        case DatatypeConstants.GMONTHDAY => true
        case DatatypeConstants.GYEAR => true
        case DatatypeConstants.GMONTH => true
        case DatatypeConstants.GDAY => true
        case _ => false
      }
    } catch {
      case ex: IllegalArgumentException =>
        false
    }
  }

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = Some(XSD + "date")

  override def id: String = "DateValueType"

  /**
    * Returns the URI of the XML Schema date/time type that a lexical string actually has.
    */
  def xmlSchemaType(lexicalString: String): String = {
    val qName = datatypeFactory.newXMLGregorianCalendar(lexicalString).getXMLSchemaType
    qName.getNamespaceURI + "#" + qName.getLocalPart
  }

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = Ordering.by((str: String) => datatypeFactory.newXMLGregorianCalendar(str))(ValueType.GregorianCalendarOrdering)
}

case object DateTimeValueType extends ValueType with Serializable {

  @transient lazy private val datatypeFactory = DatatypeFactory.newInstance()

  override def label = "DateTime"

  override def validate(lexicalString: String): Boolean = {
    Try(datatypeFactory.newXMLGregorianCalendar(lexicalString)).isSuccess
  }

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = Some(XSD + "dateTime")

  override def id: String = "DateTimeValueType"

  /**
    * Returns the URI of the XML Schema date/time type that a lexical string actually has.
    */
  def xmlSchemaType(lexicalString: String): String = {
    val qName = datatypeFactory.newXMLGregorianCalendar(lexicalString).getXMLSchemaType
    qName.getNamespaceURI + "#" + qName.getLocalPart
  }

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = Ordering.by((str: String) => datatypeFactory.newXMLGregorianCalendar(str))(ValueType.GregorianCalendarOrdering)
}
