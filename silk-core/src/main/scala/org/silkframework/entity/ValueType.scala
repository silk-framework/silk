package org.silkframework.entity


import javax.xml.datatype.{DatatypeConstants, DatatypeFactory, Duration, XMLGregorianCalendar}
import javax.xml.namespace.QName
import org.silkframework.config.Prefixes
import org.silkframework.entity.ValueType.XSD
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.plugin.AnyPlugin
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.util.Uri

import scala.util.Try
import scala.util.matching.Regex
import scala.xml.Node

/**
  * The type of a value.
  * (Atomic Types)
  */
sealed trait ValueType extends AnyPlugin {

  /** The unique ID of this value type. This will be used for serialization and deserialization */
  def id: String = pluginSpec.id

  /**
    * A human-readable label for this type.
    */
  def label: String = pluginSpec.label

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
    Set(this, vt).contains(UntypedValueType()) ||
        vt == this
  }
}

object ValueType {

  final val CUSTOM_VALUE_TYPE_ID = "CustomValueType"
  final val LANGUAGE_VALUE_TYPE_ID = "LanguageValueType"
  final val OUTDATED_AUTO_DETECT_ID = "AutoDetectValueType"

  final val UNTYPED: ValueType = UntypedValueType()
  final val STRING: ValueType = StringValueType()
  final val URI: ValueType = UriValueType()

  final val INTEGER: ValueType = IntegerValueType()
  final val INT: ValueType = IntValueType()
  final val LONG: ValueType = LongValueType()
  final val FLOAT: ValueType = FloatValueType()
  final val DOUBLE: ValueType = DoubleValueType()
  final val DECIMAL: ValueType = DecimalValueType()
  final val BOOLEAN: ValueType = BooleanValueType()

  final val BLANK_NODE: ValueType = BlankNodeValueType()

  final val DATE: DateValueType = GeneralDateValueType()
  final val YEAR: DateValueType = YearDateValueType()
  final val YEAR_MONTH: DateValueType = YearMonthDateValueType()
  final val MONTH_DAY: DateValueType = MonthDayDateValueType()
  final val DAY: DateValueType = DayDateValueType()
  final val MONTH: DateValueType = MonthDateValueType()
  final val DATE_TIME: DateTimeValueType = DateTimeValueType()

  final val XSD = "http://www.w3.org/2001/XMLSchema#"
  final lazy val xmlDatatypeFactory = DatatypeFactory.newInstance()

  /** All [[ValueType]] classes/singletons */
  val allValueType: Seq[Either[(String, Class[_ <: ValueType]), ValueType]] = Seq(
    Left((CUSTOM_VALUE_TYPE_ID, classOf[CustomValueType])),
    Left((LANGUAGE_VALUE_TYPE_ID, classOf[LanguageValueType])),
    // this type string is a left over from the previous name of UntypedValueType.
    // Since many project configs in tests still feature the old type, this is a valid workaround.
    Left((OUTDATED_AUTO_DETECT_ID, STRING.getClass)),
    Right(UNTYPED),
    Right(STRING),
    Right(URI),
    Right(INTEGER),
    Right(INT),
    Right(LONG),
    Right(FLOAT),
    Right(DOUBLE),
    Right(DECIMAL),
    Right(BOOLEAN),
    Right(BLANK_NODE),
    Right(DATE),
    Right(YEAR),
    Right(YEAR_MONTH),
    Right(MONTH_DAY),
    Right(DAY),
    Right(MONTH),
    Right(DATE_TIME)
  )

  lazy val valueTypeMapByStringId: Map[String, Either[Class[_], ValueType]] = allValueType.map {
    case Left((id, clazz)) => (id, Left(clazz))
    case Right(obj) => (obj.id, Right(obj))
  }.toMap

  lazy val valueTypeIdMapByClass: Map[Class[_], String] = valueTypeMapByStringId.filterNot(x => x._1 == OUTDATED_AUTO_DETECT_ID).map(ei =>  // we have to remove the outdated name
    ei._2 match {
      case Left(clazz) => (clazz, ei._1)
      case Right(obj) => (obj.getClass, ei._1)
    }
  ).toMap

  val DefaultOrdering: Ordering[String] = Ordering.String
  val GregorianCalendarOrdering: Ordering[XMLGregorianCalendar] = Ordering.fromLessThan[XMLGregorianCalendar]((date1: XMLGregorianCalendar, date2: XMLGregorianCalendar) =>{
    date1.compare(date2) < 0
  })
  val DurationOrdering: Ordering[Duration] = Ordering.fromLessThan[Duration]((d1: Duration, d2: Duration) =>{
    d1.compare(d2) < 0
  })

  def valueTypeById(valueTypeId: String): Either[Class[_], ValueType] = {
    // The stripping of '$' at the end is for being backward compatible to older project data
    valueTypeMapByStringId.get(valueTypeId.stripSuffix("$")) match {
      case None =>
        throw new IllegalArgumentException("Invalid value type ID: " + valueTypeId + ". Valid values: " +
            valueTypeMapByStringId.keys.toSeq.sortWith(_ < _).mkString(", "))
      case Some(vt) => vt
    }
  }

  /**
    * XML serialization of value types.
    */
  implicit object ValueTypeXmlFormat extends XmlFormat[ValueType] {
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
      val typeId = value.id
      value match {
        case CustomValueType(typeUri) => <ValueType nodeType={typeId} uri={typeUri}/>
        case LanguageValueType(lang) => <ValueType nodeType={typeId} lang={lang}/>
        case objValueType: ValueType => <ValueType nodeType={typeId}/>
      }
    }

    // Handle the ValueType class cases
    private def readClassValueTypes(value: Node,
                                    nodeType: String,
                                    prefixes: Prefixes): ValueType = {
      nodeType.replace("$", "") match {
        case OUTDATED_AUTO_DETECT_ID => ValueType.STRING //for backward compatibility
        case CUSTOM_VALUE_TYPE_ID =>
          (value \ "@uri").headOption match {
            case Some(typeUri) =>
              val uri = Uri.parse(typeUri.text.trim, prefixes)
              CustomValueType(uri.uri)
            case None =>
              throw new IllegalArgumentException("'uri' attribute not existing in node")
          }
        case LANGUAGE_VALUE_TYPE_ID =>
          (value \ "@lang").headOption match {
            case Some(lang) =>
              LanguageValueType(lang.text.trim)
            case None =>
              throw new IllegalArgumentException("'lang' attribute not existing in node")
          }
      }
    }
  }
}

/**
  * Special type that signals that the actual type is unknown.
  */
@Plugin(
  id = "UntypedValueType",
  label = "Untyped",
  description = "The data type is decided automatically, based on the lexical form of each value."
)
case class UntypedValueType() extends ValueType with Serializable {//renamed from AutoDetectValueType

  /** returns true if the lexical string is a representation of this type */
  override def validate(lexicalString: String): Boolean = true

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = None

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = ValueType.DefaultOrdering
}

/** A custom type that is used for all types not covered by any other types. */
@Plugin(
  id = ValueType.CUSTOM_VALUE_TYPE_ID,
  label = "Custom Type"
)
case class CustomValueType(typeUri: String) extends ValueType {

  override def validate(lexicalString: String): Boolean = {
    true // No validation for custom types
  }

  override def uri: Option[String] = Some(typeUri)

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = ValueType.DefaultOrdering
}

/** Represents language tagged strings. */
@Plugin(
  id = ValueType.LANGUAGE_VALUE_TYPE_ID,
  label = "Language Tagged",
  description = "Suited for texts that are in a specific language."
)
case class LanguageValueType(language: String) extends ValueType {

  override def label: String = "@" + language

  override def validate(lexicalString: String): Boolean = true // No validation needed

  override def uri: Option[String] = None // These are always strings

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = ValueType.DefaultOrdering
}

@Plugin(
  id = "IntegerValueType",
  label = "Integer",
  description = "Numeric values without a fractional component, unbounded."
)
case class IntegerValueType() extends ValueType with Serializable {

  val integerRegex: Regex = """^[+-]?(([1-9][0-9]*)|(0))$""".r

  override def validate(lexicalString: String): Boolean = {
    integerRegex.findFirstMatchIn(lexicalString).isDefined
  }

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = Some(XSD + "integer")

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = Ordering.by((str: String) => str.toInt)
}

@Plugin(
  id = "IntValueType",
  label = "Int",
  description = "Numeric values without a fractional component, represented as 32-bit signed integers"
)
case class IntValueType() extends ValueType with Serializable {

  override def validate(lexicalString: String): Boolean = {
    Try(lexicalString.toInt).isSuccess
  }

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = Some(XSD + "int")

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = Ordering.by((str: String) => str.toInt)
}

@Plugin(
  id = "LongValueType",
  label = "Long",
  description = "Numeric values without a fractional component, represented as 64-bit signed integers."
)
case class LongValueType() extends ValueType with Serializable {

  override def validate(lexicalString: String): Boolean = {
    Try(lexicalString.toLong).isSuccess
  }

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = Some(XSD + "long")

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = Ordering.by((str: String) => str.toLong)
}

@Plugin(
  id = "StringValueType",
  label = "String",
  description = "Suited for values which contain text"
)
case class StringValueType() extends ValueType with Serializable {

  /** returns true if the lexical string is a representation of this type */
  override def validate(lexicalString: String): Boolean = true // Always true

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = Some(XSD + "string") // In RDF this can be omitted

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = ValueType.DefaultOrdering
}

@Plugin(
  id = "FloatValueType",
  label = "Float",
  description = "Numeric values which have a fractional value, represented as IEEE single-precision 32-bit floating point numbers."
)
case class FloatValueType() extends ValueType with Serializable {

  override def validate(lexicalString: String): Boolean = {
    Try(lexicalString.toFloat).isSuccess
  }

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = Some(XSD + "float")

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = Ordering.by((str: String) => str.toFloat)
}

@Plugin(
  id = "DoubleValueType",
  label = "Double",
  description = "Numeric values which have a fractional value, represented as IEEE double-precision 64-bit floating point numbers."
)
case class DoubleValueType() extends ValueType with Serializable {

  override def validate(lexicalString: String): Boolean = {
    Try(lexicalString.toDouble).isSuccess
  }

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = Some(XSD + "double")

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = Ordering.by((str: String) => str.toDouble)
}

@Plugin(
  id = "DecimalValueType",
  label = "Decimal",
  description = "Decimal values."
)
case class DecimalValueType() extends ValueType with Serializable {

  override def validate(lexicalString: String): Boolean = {
    Try(BigDecimal(lexicalString)).isSuccess
  }

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = Some(XSD + "decimal")

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = Ordering.by((str: String) => BigDecimal(str))
}

@Plugin(
  id = "BooleanValueType",
  label = "Boolean",
  description = "Suited for values which are either true or false"
)
case class BooleanValueType() extends ValueType with Serializable {

  override def validate(lexicalString: String): Boolean = {
    Try(lexicalString.toBoolean).isSuccess
  }

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = Some(XSD + "boolean")

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = Ordering.by((str: String) => str.toBoolean)
}

@Plugin(
  id = "UriValueType",
  label = "Uri",
  description = "Suited for values which are Unique Resource Identifiers"
)
case class UriValueType() extends ValueType with Serializable {

  override def validate(lexicalString: String): Boolean = {
    new Uri(lexicalString).isValidUri
  }

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = None

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = ValueType.DefaultOrdering
}

@Plugin(
  id = "BlankNodeValueType",
  label = "Blank Node",
  description = "RDF blank nodes"
)
case class BlankNodeValueType() extends ValueType with Serializable {

  override def validate(lexicalString: String): Boolean = true // FIXME: No blank node lexical validation

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = None

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = ValueType.DefaultOrdering
}

abstract class DateValueType extends ValueType with Serializable {

  def allowedXsdTypes: Set[QName]

  override def validate(lexicalString: String): Boolean = {
    try {
      val date = ValueType.xmlDatatypeFactory.newXMLGregorianCalendar(lexicalString)
      allowedXsdTypes.contains(date.getXMLSchemaType)
    } catch {
      case ex: IllegalArgumentException =>
        false
    }
  }

  /**
    * Returns the URI of the XML Schema date/time type that a lexical string actually has.
    */
  def xmlSchemaType(lexicalString: String): String = {
    val qName = ValueType.xmlDatatypeFactory.newXMLGregorianCalendar(lexicalString).getXMLSchemaType
    qName.getNamespaceURI + "#" + qName.getLocalPart
  }

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = Ordering.by((str: String) => ValueType.xmlDatatypeFactory.newXMLGregorianCalendar(str))(ValueType.GregorianCalendarOrdering)
}

@Plugin(
  id = "DateValueType",
  label = "Date",
  description = "Suited for XML Schema dates. Accepts values in the the following formats: xsd:date, xsd:gDay, xsd:gMonth, xsd:gMonthDay, xsd:gYear, xsd:gYearMonth."
)
case class GeneralDateValueType() extends DateValueType {
  override def uri: Option[String] = Some(XSD + "date")
  override def allowedXsdTypes: Set[QName] = {
    Set(
      DatatypeConstants.DATE,
      DatatypeConstants.GYEARMONTH,
      DatatypeConstants.GMONTHDAY,
      DatatypeConstants.GYEAR,
      DatatypeConstants.GMONTH,
      DatatypeConstants.GDAY
    )
  }
}

@Plugin(
  id = "YearValueType",
  label = "Year"
)
case class YearDateValueType() extends DateValueType {
  override def uri: Option[String] = Some(XSD + "gYear")
  override def allowedXsdTypes: Set[QName] = Set(DatatypeConstants.GYEAR)
}

@Plugin(
  id = "YeahMonthValueType",
  label = "YearMonth"
)
case class YearMonthDateValueType() extends DateValueType {
  override def uri: Option[String] = Some(XSD + "gYearMonth")
  override def allowedXsdTypes: Set[QName] = Set(DatatypeConstants.GYEARMONTH)
}

@Plugin(
  id = "MonthDayValueType",
  label = "MonthDay"
)
case class MonthDayDateValueType() extends DateValueType {
  override def uri: Option[String] = Some(XSD + "gMonthDay")
  override def allowedXsdTypes: Set[QName] = Set(DatatypeConstants.GMONTHDAY)
}

@Plugin(
  id = "DayValueType",
  label = "Day"
)
case class DayDateValueType() extends DateValueType {
  override def uri: Option[String] = Some(XSD + "gDay")
  override def allowedXsdTypes: Set[QName] = Set(DatatypeConstants.GDAY)
}

@Plugin(
  id = "MonthValueType",
  label = "Month"
)
case class MonthDateValueType() extends DateValueType {
  override def uri: Option[String] = Some(XSD + "gMonth")
  override def allowedXsdTypes: Set[QName] = Set(DatatypeConstants.GMONTH)
}

@Plugin(
  id = "DurationValueType",
  label = "Duration"
)
case class DurationValueType() extends ValueType with Serializable {

  override def validate(lexicalString: String): Boolean = {
    try {
      val date = ValueType.xmlDatatypeFactory.newDuration(lexicalString)
      date.getXMLSchemaType match {
        case DatatypeConstants.DURATION => true
        case _ => false
      }
    } catch {
      case ex: IllegalArgumentException =>
        false
    }
  }

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = Some(XSD + "duration")

  /**
    * Returns the URI of the XML Schema date/time type that a lexical string actually has.
    */
  def xmlSchemaType(lexicalString: String): String = {
    val qName = ValueType.xmlDatatypeFactory.newDuration(lexicalString).getXMLSchemaType
    qName.getNamespaceURI + "#" + qName.getLocalPart
  }

  /** Optional provisioning of an [[Ordering]] associated with the portrayed type */
  override def ordering: Ordering[String] = Ordering.by((str: String) => ValueType.xmlDatatypeFactory.newDuration(str))(ValueType.DurationOrdering)
}

@Plugin(
  id = "DateTimeValueType",
  label = "DateTime",
  description = "Suited for XML Schema dates and times. Accepts values in the the following formats: xsd:date, xsd:dateTime, xsd:gDay, xsd:gMonth, xsd:gMonthDay, xsd:gYear, xsd:gYearMonth, xsd:time."
)
case class DateTimeValueType() extends ValueType with Serializable {

  @transient lazy private val datatypeFactory = DatatypeFactory.newInstance()

  override def validate(lexicalString: String): Boolean = {
    Try(datatypeFactory.newXMLGregorianCalendar(lexicalString)).isSuccess
  }

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = Some(XSD + "dateTime")

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
