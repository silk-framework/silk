package org.silkframework.serialization.json

import org.silkframework.config.{PlainTask, Task, TaskSpec}
import org.silkframework.dataset.{Dataset, DatasetTask}
import org.silkframework.entity._
import org.silkframework.rule._
import org.silkframework.rule.input.{Input, PathInput, TransformInput, Transformer}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.serialization.json.InputJsonSerializer._
import org.silkframework.serialization.json.JsonSerializers._
import org.silkframework.util.{Identifier, Uri}
import play.api.libs.json._

/**
  * Serializers for JSON.
  */
object JsonSerializers {

  final val ID = "id"
  final val TYPE = "type"
  final val PARAMETERS = "parameters"
  final val URI = "uri"

  implicit object JsonDatasetTaskFormat extends JsonFormat[DatasetTask] {

    override def read(value: JsValue)(implicit readContext: ReadContext): DatasetTask = {
      implicit val prefixes = readContext.prefixes
      implicit val resource = readContext.resources
      new DatasetTask(
        id = (value \ ID).as[JsString].value,
        plugin =
            Dataset(
              id = (value \ TYPE).as[JsString].value,
              params = (value \ PARAMETERS).as[JsObject].value.mapValues(_.as[JsString].value).asInstanceOf[Map[String, String]]
            )
      )
    }

    override def write(value: DatasetTask)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(
        ID -> JsString(value.id.toString) ::
            TYPE -> JsString(value.plugin.plugin.id.toString) ::
            PARAMETERS -> Json.toJson(value.plugin.parameters) :: Nil
      )

    }
  }

  def mustBeJsObject[T](jsValue: JsValue)(block: JsObject => T): T = {
    jsValue match {
      case jsObject: JsObject => block(jsObject)
      case _ => throw JsonParseException("Error while parsing. JSON value is not JSON object!")
    }
  }

  def mustBeJsArray[T](jsValue: JsValue)(block: JsArray => T): T = {
    jsValue match {
      case jsArray: JsArray => block(jsArray)
      case _ => throw JsonParseException("Error while parsing. JSON value is not a JSON array!")
    }
  }

  def stringValue(json: JsValue, attributeName: String): String = {
    (json \ attributeName).toOption match {
      case Some(jsString: JsString) =>
        jsString.value
      case Some(_) =>
        throw JsonParseException("Value for attribute " + attributeName + " is not a String!")
      case None =>
        throw JsonParseException("Attribute " + attributeName + " not found!")
    }
  }

  def optionalValue(json: JsValue, attributeName: String): Option[JsValue] = {
    (json \ attributeName).toOption.filterNot(_ == JsNull)
  }

  def silkPath(id: String, pathStr: String)(implicit readContext: ReadContext): Path = {
    try {
      Path.parse(pathStr)(readContext.prefixes)
    } catch {
      case ex: Exception => throw new ValidationException(ex.getMessage, id, "path")
    }
  }

  /**
    * PathInput
    */
  implicit object PathInputJsonFormat extends JsonFormat[PathInput] {
    final val PATH = "path"

    override def read(value: JsValue)(implicit readContext: ReadContext): PathInput = {
      val id = stringValue(value, ID)
      val pathStr = stringValue(value, PATH)
      PathInput(id, silkPath(id, pathStr))
    }

    override def write(value: PathInput)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(
        Seq(
          TYPE -> JsString(PATH_INPUT),
          ID -> JsString(value.id.toString),
          PATH -> JsString(value.path.serialize(writeContext.prefixes))
        )
      )
    }
  }

  def mustBeDefined(value: JsValue, attributeName: String): JsValue = {
    (value \ attributeName).toOption.
        getOrElse(throw JsonParseException("No attribute with name " + attributeName + " found!"))
  }

  /**
    * Transform Input
    */
  implicit object TransformInputFormat extends JsonFormat[TransformInput] {
    final val INPUTS = "inputs"
    final val FUNCTION = "function"

    override def read(value: JsValue)(implicit readContext: ReadContext): TransformInput = {
      val id = stringValue(value, ID)
      val inputs = mustBeJsArray(mustBeDefined(value, INPUTS)) { jsArray =>
        jsArray.value.map(fromJson[Input](_)(InputJsonSerializer.InputJsonFormat, readContext))
      }
      implicit val prefixes = readContext.prefixes
      implicit val resourceManager = readContext.resources
      try {
        val transformer = Transformer(stringValue(value, FUNCTION), readParameters(value))
        TransformInput(id, transformer, inputs.toList)
      } catch {
        case ex: Exception => throw new ValidationException(ex.getMessage, id, "Transformation")
      }
    }

    override def write(value: TransformInput)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(
        Seq(
          TYPE -> JsString(TRANSFORM_INPUT),
          ID -> JsString(value.id),
          FUNCTION -> JsString(value.transformer.plugin.id),
          INPUTS -> JsArray(value.inputs.map(toJson[Input])),
          PARAMETERS -> Json.toJson(value.transformer.parameters)
        )
      )
    }
  }

  def readParameters(value: JsValue): Map[String, String] = {
    mustBeJsObject(mustBeDefined(value, PARAMETERS)) { array =>
      Json.fromJson[Map[String, String]](array).get
    }
  }

  /**
    * Value Type
    */
  implicit object ValueTypeJsonFormat extends JsonFormat[ValueType] {
    final val LANG = "lang"
    final val NODE_TYPE = "nodeType"

    override def read(value: JsValue)(implicit readContext: ReadContext): ValueType = {
      val nodeType = stringValue(value, NODE_TYPE)
      ValueType.valueTypeById(nodeType) match {
        case Left(_) =>
          nodeType match {
            case "CustomValueType" =>
              val uriString = stringValue(value, URI)
              val uri = Uri.parse(uriString, readContext.prefixes)
              CustomValueType(uri.uri)
            case "LanguageValueType" =>
              val lang = stringValue(value, LANG)
              LanguageValueType(lang)
          }
        case Right(valueType) =>
          valueType

      }
    }

    override def write(value: ValueType)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      val typeId = ValueType.valueTypeId(value)
      val additionalAttributes = value match {
        case CustomValueType(typeUri) =>
          Some(URI -> JsString(typeUri))
        case LanguageValueType(lang) =>
          Some(LANG -> JsString(lang))
        case _ =>
          None
      }
      JsObject(
        Seq(
          NODE_TYPE -> JsString(typeId)
        ) ++ additionalAttributes
      )
    }
  }

  /**
    * Mapping Target JSON format
    */
  implicit object MappingTargetJsonFormat extends JsonFormat[MappingTarget] {
    final val VALUE_TYPE = "valueType"

    override def read(value: JsValue)(implicit readContext: ReadContext): MappingTarget = {
      val uri = stringValue(value, URI)
      val valueType = fromJson[ValueType](mustBeDefined(value, VALUE_TYPE))
      MappingTarget(Uri.parse(uri, readContext.prefixes), valueType)
    }

    override def write(value: MappingTarget)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(
        Seq(
          URI -> JsString(value.propertyUri.serialize(writeContext.prefixes)),
          VALUE_TYPE -> toJson(value.valueType)
        )
      )
    }
  }

  /**
    * Mapping Rules
    */
  implicit object MappingRulesJsonFormat extends JsonFormat[MappingRules] {
    final val URI_RULE: String = "uriRule"
    final val TYPE_RULES: String = "typeRules"
    final val PROPERTY_RULES: String = "propertyRules"

    /**
      * Deserializes a value.
      */
    override def read(value: JsValue)(implicit readContext: ReadContext): MappingRules = {
      val uriRule = optionalValue(value, URI_RULE).map(UriMappingJsonFormat.read)
      val typeRules = mustBeJsArray(mustBeDefined(value, TYPE_RULES)) { array =>
        array.value.map(TypeMappingJsonFormat.read)
      }
      val propertyRules = mustBeJsArray(mustBeDefined(value, PROPERTY_RULES)) { array =>
        array.value.map(TransformRuleJsonFormat.read)
      }

      MappingRules(uriRule, typeRules, propertyRules)
    }

    /**
      * Serializes a value.
      */
    override def write(value: MappingRules)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        URI_RULE -> value.uriRule.map(toJson[UriMapping]),
        TYPE_RULES -> JsArray(value.typeRules.map(toJson[TransformRule])),
        PROPERTY_RULES -> JsArray(value.propertyRules.map(toJson[TransformRule]))
      )
    }
  }

  /**
    * Root Mapping
    */
  implicit object RootMappingRuleJsonFormat extends JsonFormat[RootMappingRule] {
    final val RULES_PROPERTY: String = "rules"

    /**
      * Deserializes a value.
      */
    override def read(value: JsValue)(implicit readContext: ReadContext): RootMappingRule = {
      RootMappingRule(fromJson[MappingRules]((value \ RULES_PROPERTY).get))
    }

    /**
      * Serializes a value.
      */
    override def write(value: RootMappingRule)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(
        Seq(
          TYPE -> JsString("root"),
          ID -> JsString(value.id),
          RULES_PROPERTY -> toJson(value.rules)
        )
      )
    }
  }

  /**
    * Type Mapping
    */
  implicit object TypeMappingJsonFormat extends JsonFormat[TypeMapping] {
    final val TYPE_PROPERTY: String = "typeUri"

    /**
      * Deserializes a value.
      */
    override def read(value: JsValue)(implicit readContext: ReadContext): TypeMapping = {
      val name = stringValue(value, ID)
      val typeUri = stringValue(value, TYPE_PROPERTY)
      TypeMapping(name, Uri.parse(typeUri, readContext.prefixes))
    }

    /**
      * Serializes a value.
      */
    override def write(value: TypeMapping)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(
        Seq(
          TYPE -> JsString("type"),
          ID -> JsString(value.id),
          TYPE_PROPERTY -> JsString(value.typeUri.serialize(writeContext.prefixes))
        )
      )
    }
  }

  /**
    * URI Mapping
    */
  implicit object UriMappingJsonFormat extends JsonFormat[UriMapping] {
    final val PATTERN_PROPERTY: String = "pattern"

    /**
      * Deserializes a value.
      */
    override def read(value: JsValue)(implicit readContext: ReadContext): UriMapping = {
      val name = stringValue(value, ID)
      val pattern = stringValue(value, PATTERN_PROPERTY)
      UriMapping(name, pattern)
    }

    /**
      * Serializes a value.
      */
    override def write(value: UriMapping)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(
        Seq(
          TYPE -> JsString("uri"),
          ID -> JsString(value.id),
          PATTERN_PROPERTY -> JsString(value.pattern)
        )
      )
    }
  }

  /**
    * Direct Mapping
    */
  implicit object DirectMappingJsonFormat extends JsonFormat[DirectMapping] {
    final val SOURCE_PATH_PROPERTY: String = "sourcePath"
    final val MAPPING_TARGET_PROPERTY: String = "mappingTarget"

    /**
      * Deserializes a value.
      */
    override def read(value: JsValue)(implicit readContext: ReadContext): DirectMapping = {
      val name = stringValue(value, ID)
      val sourcePath = silkPath(name, stringValue(value, SOURCE_PATH_PROPERTY))
      val mappingTarget = fromJson[MappingTarget]((value \ MAPPING_TARGET_PROPERTY).get)
      DirectMapping(name, sourcePath, mappingTarget)
    }

    /**
      * Serializes a value.
      */
    override def write(value: DirectMapping)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(
        Seq(
          TYPE -> JsString("direct"),
          ID -> JsString(value.id),
          SOURCE_PATH_PROPERTY -> JsString(value.sourcePath.serialize(writeContext.prefixes)),
          MAPPING_TARGET_PROPERTY -> toJson(value.mappingTarget)
        )
      )
    }
  }

  /**
    * Object Mapping
    */
  implicit object ObjectMappingJsonFormat extends JsonFormat[ObjectMapping] {
    final val PATTERN_PROPERTY: String = "pattern"
    final val TARGET_PROPERTY: String = "mappingTarget"

    /**
      * Deserializes a value.
      */
    override def read(value: JsValue)(implicit readContext: ReadContext): ObjectMapping = {
      val name = stringValue(value, ID)
      val pattern = stringValue(value, PATTERN_PROPERTY)
      val mappingTarget = fromJson[MappingTarget]((value \ TARGET_PROPERTY).get)
      ObjectMapping(name, pattern, mappingTarget)
    }

    /**
      * Serializes a value.
      */
    override def write(value: ObjectMapping)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(
        Seq(
          TYPE -> JsString("object"),
          ID -> JsString(value.id),
          PATTERN_PROPERTY -> JsString(value.pattern),
          TARGET_PROPERTY -> toJson(value.mappingTarget)
        )
      )
    }
  }

  /**
    * Hierarchical Mapping
    */
  implicit object HierarchicalMappingJsonFormat extends JsonFormat[HierarchicalMapping] {
    final val SOURCE_PATH: String = "sourcePath"
    final val TARGET_PROPERTY: String = "mappingTarget"
    final val RULES: String = "rules"

    /**
      * Deserializes a value.
      */
    override def read(value: JsValue)(implicit readContext: ReadContext): HierarchicalMapping = {
      val name = stringValue(value, ID)
      val sourcePath = silkPath(name, stringValue(value, SOURCE_PATH))
      val mappingTarget = optionalValue(value, TARGET_PROPERTY).map(fromJson[MappingTarget])
      val children = fromJson[MappingRules](mustBeDefined(value, RULES))
      HierarchicalMapping(name, sourcePath, mappingTarget.map(_.propertyUri), children)
    }

    /**
      * Serializes a value.
      */
    override def write(value: HierarchicalMapping)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        TYPE -> JsString("hierarchical"),
        ID -> JsString(value.id),
        SOURCE_PATH -> JsString(value.sourcePath.serialize(writeContext.prefixes)),
        TARGET_PROPERTY -> value.target.map(toJson(_)).getOrElse(JsNull).asInstanceOf[JsValue],
        RULES -> toJson(value.rules)
      )
    }
  }

  /**
    * Transform Rule JSON Format
    */
  implicit object TransformRuleJsonFormat extends JsonFormat[TransformRule] {
    /**
      * Deserializes a value.
      */
    override def read(jsValue: JsValue)(implicit readContext: ReadContext): TransformRule = {
      stringValue(jsValue, TYPE) match {
        case "root" =>
          fromJson[RootMappingRule](jsValue)
        case "type" =>
          fromJson[TypeMapping](jsValue)
        case "uri" =>
          fromJson[UriMapping](jsValue)
        case "direct" =>
          fromJson[DirectMapping](jsValue)
        case "object" =>
          fromJson[ObjectMapping](jsValue)
        case "hierarchical" =>
          fromJson[HierarchicalMapping](jsValue)
        case "complex" =>
          readTransformRule(jsValue)
      }
    }

    private def readTransformRule(jsValue: JsValue)
                                 (implicit readContext: ReadContext)= {
      val mappingTarget = (jsValue \ "mappingTarget").
          toOption.
          map(fromJson[MappingTarget])
      val complex = ComplexMapping(
        id = stringValue(jsValue, ID),
        operator = fromJson[Input]((jsValue \ "operator").get),
        target = mappingTarget
      )
      TransformRule.simplify(complex)
    }

    /**
      * Serializes a value. curl -i -H 'accept: application/json' -XGET http://localhost:9000/transform/tasks/BoschJSON/product-feed-transform/rule/name
      */
    override def write(rule: TransformRule)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      rule match {
        case t: RootMappingRule =>
          toJson(t)
        case t: TypeMapping =>
          toJson(t)
        case u: UriMapping =>
          toJson(u)
        case d: DirectMapping =>
          toJson(d)
        case o: ObjectMapping =>
          toJson(o)
        case h: HierarchicalMapping =>
          toJson(h)
        case _ =>
          writeTransformRule(rule)
      }
    }

    private def writeTransformRule(rule: TransformRule)(implicit writeContext: WriteContext[JsValue]) = {
      JsObject(
        Seq(
          TYPE -> JsString("complex"),
          ID -> JsString(rule.id),
          "operator" -> toJson(rule.operator)
        ) ++
            rule.target.map("mappingTarget" -> toJson(_))
      )
    }
  }

  /**
    * Dataset selection.
    */
  implicit object DatasetSelectionJsonFormat extends JsonFormat[DatasetSelection] {
    final val INPUT_ID: String = "inputId"
    final val TYPE_URI: String = "typeUri"
    final val RESTRICTION: String = "restriction"

    /**
      * Deserializes a value.
      */
    override def read(value: JsValue)(implicit readContext: ReadContext): DatasetSelection = {
      DatasetSelection(
        inputId = stringValue(value, INPUT_ID),
        typeUri = stringValue(value, TYPE_URI),
        restriction = Restriction.parse(stringValue(value, RESTRICTION))(readContext.prefixes)
      )
    }

    /**
      * Serializes a value.
      */
    override def write(value: DatasetSelection)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        INPUT_ID -> value.inputId.toString,
        TYPE_URI -> value.typeUri.uri,
        RESTRICTION -> value.restriction.serialize
      )
    }
  }

  /**
    * Transform Specification
    */
  implicit object TransformSpecJsonFormat extends JsonFormat[TransformSpec] {
    final val SELECTION = "selection"
    final val RULES_PROPERTY: String = "root"
    final val OUTPUTS: String = "outputs"
    final val TARGET_VOCABULARIES: String = "targetVocabularies"

    /**
      * Deserializes a value.
      */
    override def read(value: JsValue)(implicit readContext: ReadContext): TransformSpec = {
      TransformSpec(
        selection = fromJson[DatasetSelection](mustBeDefined(value, SELECTION)),
        mappingRule = fromJson[RootMappingRule](mustBeDefined(value, RULES_PROPERTY)),
        outputs = mustBeJsArray(mustBeDefined(value, OUTPUTS))(_.value.map(v => Identifier(v.toString()))),
        targetVocabularies = mustBeJsArray(mustBeDefined(value, TARGET_VOCABULARIES))(_.value.map(_.toString))
      )
    }

    /**
      * Serializes a value.
      */
    override def write(value: TransformSpec)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        SELECTION -> toJson(value.selection),
        RULES_PROPERTY -> toJson(value.rules),
        OUTPUTS -> JsArray(value.outputs.map(id => JsString(id.toString))),
        TARGET_VOCABULARIES -> JsArray(value.targetVocabularies.toSeq.map(JsString))

      )
    }
  }

  /**
    * Transform Task
    */
  object TransformTaskFormat extends TaskJsonFormat[TransformSpec]

  /**
    * Task
    */
  class TaskJsonFormat[T <: TaskSpec](implicit dataFormat: JsonFormat[T]) extends JsonFormat[Task[T]] {
    /**
      * Deserializes a value.
      */
    override def read(value: JsValue)(implicit readContext: ReadContext): Task[T] = {
      PlainTask(
        id = stringValue(value, ID),
        data = fromJson[T](value)
      )
    }

    /**
      * Serializes a value.
      */
    override def write(value:  Task[T])(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        ID -> JsString(value.id.toString)
      ) ++ toJson(value.data).as[JsObject]
    }
  }

  def toJson[T](value: T)(implicit format: JsonFormat[T], writeContext: WriteContext[JsValue] = WriteContext[JsValue]()): JsValue = {
    format.write(value)
  }

  def fromJson[T](node: JsValue)(implicit format: JsonFormat[T], readContext: ReadContext): T = {
    format.read(node)
  }
}

object InputJsonSerializer {
  final val PATH_INPUT: String ="pathInput"
  final val TRANSFORM_INPUT: String = "transformInput"

  implicit object InputJsonFormat extends JsonFormat[Input] {

    override def read(value: JsValue)(implicit readContext: ReadContext): Input = {
      mustBeJsObject(value) { jsObject =>
        jsObject.value.get(TYPE) match {
          case Some(typ: JsString) =>
            typ.value match {
              case PATH_INPUT =>
                fromJson[PathInput](jsObject)
              case TRANSFORM_INPUT =>
                fromJson[TransformInput](jsObject)
            }
          case _ =>
            throw JsonParseException("Input JSON object has no 'type' attribute! Instead found: " + jsObject.value.keys.mkString(", "))
        }
      }
    }

    override def write(value: Input)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      value match {
        case path: PathInput => toJson(path)
        case transform: TransformInput => toJson(transform)
      }
    }
  }

}

case class JsonParseException(msg: String, cause: Throwable = null) extends RuntimeException(msg, cause)
