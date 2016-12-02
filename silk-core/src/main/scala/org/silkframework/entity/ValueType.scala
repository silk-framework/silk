package org.silkframework.entity

import java.net.URI

import org.silkframework.config.Prefixes
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.util.Uri

import scala.util.Try
import scala.xml.Node
import ValueType.XSD

/**
  * The type of a value.
  */
sealed trait ValueType {
  /** returns true if the lexical string is a representation of this type */
  def validate(lexicalString: String): Boolean

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  def uri: Option[String]
}

object ValueType {
  final val XSD = "http://www.w3.org/2001/XMLSchema#"

  implicit object ValueTypeFormat extends XmlFormat[ValueType] {
    /**
      * Deserializes a value.
      */
    override def read(value: Node)(implicit readContext: ReadContext): ValueType = {
      (value \ "@nodeType").headOption match {
        case Some(nodeTypeNode) =>
          val nodeType = nodeTypeNode.text.trim
          valueTypeById(nodeType) match {
            case Left(clazz) =>
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
        case CustomValueType(typeUri) =>
            <ValueType nodeType={typeId} uri={typeUri}/>
        case LanguageValueType(lang) =>
            <ValueType nodeType={typeId} lang={lang}/>
        case objValueType: ValueType =>
            <ValueType nodeType={typeId}/>
      }
    }
  }

  def valueTypeById(valueTypeId: String): Either[Class[_], ValueType] = {
    valueTypeMapByStringId.get(valueTypeId) match {
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
    nodeType match {
      case "CustomValueType" =>
        (value \ "@uri").headOption match {
          case Some(typeUri) =>
            val uri = Uri.parse(typeUri.text.trim, prefixes)
            CustomValueType(uri.uri)
          case None =>
            throw new IllegalArgumentException("Uri element not existing in node")
        }
      case "LanguageValueType" =>
        (value \ "@lang").headOption match {
          case Some(lang) =>
            LanguageValueType(lang.text.trim)
          case None =>
            throw new IllegalArgumentException("Uri element not existing in node")
        }
    }
  }

  /** All [[ValueType]] classes/singletons */
  val allValueType = Seq[Either[Class[_], ValueType]](
    Left(classOf[CustomValueType]),
    Left(classOf[LanguageValueType]),
    Right(IntValueType),
    Right(LongValueType),
    Right(StringValueType),
    Right(FloatValueType),
    Right(DoubleValueType),
    Right(BooleanValueType),
    Right(IntegerValueType),
    Right(UriValueType),
    Right(AutoDetectValueType),
    Right(BlankNodeValueType)
  )

  val valueTypeMapByStringId: Map[String, Either[Class[_], ValueType]] = allValueType.map {
    case l@Left(clazz) => (clazz.getName.split("\\.").last, l)
    case r@Right(obj) => (obj.getClass.getName.split("\\.").last, r)
  }.toMap

  val valueTypeIdMapByClass: Map[Class[_], String] = valueTypeMapByStringId.map { case (id, classOrObj) =>
    classOrObj match {
      case Left(clazz) => (clazz, id)
      case Right(obj) => (obj.getClass, id)
    }
  }.toMap
}

/**
  * If this value type is set, then the values can be transformed to any valid value that can be inferred from the
  * lexical form, e.g. "1" can be an Int, but also a String.
  */
object AutoDetectValueType extends ValueType with Serializable {
  /** returns true if the lexical string is a representation of this type */
  override def validate(lexicalString: String): Boolean = true

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = None
}

/** A custom type that is used for all types not covered by any other types. */
case class CustomValueType(typeUri: String) extends ValueType {
  override def validate(lexicalString: String): Boolean = {
    true // No validation for custom types
  }

  override def uri: Option[String] = Some(typeUri)
}

/** Represents language tagged strings. */
case class LanguageValueType(language: String) extends ValueType {
  override def validate(lexicalString: String): Boolean = true // No validation needed

  override def uri: Option[String] = None // These are always strings
}

object IntValueType extends ValueType with Serializable {
  override def validate(lexicalString: String): Boolean = {
    Try(lexicalString.toInt).isSuccess
  }

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = Some(XSD + "int")
}

object LongValueType extends ValueType with Serializable {
  override def validate(lexicalString: String): Boolean = {
    Try(lexicalString.toLong).isSuccess
  }

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = Some(XSD + "long")
}

object StringValueType extends ValueType with Serializable {
  /** returns true if the lexical string is a representation of this type */
  override def validate(lexicalString: String): Boolean = true // Always true

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = Some(XSD + "string") // In RDF this can be omitted
}

object FloatValueType extends ValueType with Serializable {
  override def validate(lexicalString: String): Boolean = {
    Try(lexicalString.toFloat).isSuccess
  }

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = Some(XSD + "float")
}

object DoubleValueType extends ValueType with Serializable {
  override def validate(lexicalString: String): Boolean = {
    Try(lexicalString.toDouble).isSuccess
  }

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = Some(XSD + "double")
}

object BooleanValueType extends ValueType with Serializable {
  override def validate(lexicalString: String): Boolean = {
    Try(lexicalString.toBoolean).isSuccess
  }

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = Some(XSD + "boolean")
}

object IntegerValueType extends ValueType with Serializable {
  val integerRegex = """^[+-]?(([1-9][0-9]*)|(0))$""".r

  override def validate(lexicalString: String): Boolean = {
    integerRegex.findFirstMatchIn(lexicalString).isDefined
  }

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = Some(XSD + "integer")
}

object UriValueType extends ValueType with Serializable {
  override def validate(lexicalString: String): Boolean = {
    Try(new URI(lexicalString)).isSuccess
  }

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = None
}

object BlankNodeValueType extends ValueType with Serializable {
  override def validate(lexicalString: String): Boolean = true // FIXME: No blank node lexical validation

  /** if None then this type has no URI, if Some then this is the type URI that can also be set in e.g. RDF */
  override def uri: Option[String] = None
}