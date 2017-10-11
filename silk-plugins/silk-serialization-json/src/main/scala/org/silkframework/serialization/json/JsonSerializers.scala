package org.silkframework.serialization.json

import java.net.HttpURLConnection
import org.silkframework.config.{MetaData, PlainTask, Task, TaskSpec}
import org.silkframework.dataset.{Dataset, DatasetTask}
import org.silkframework.entity._
import org.silkframework.rule._
import org.silkframework.rule.input.{Input, PathInput, TransformInput, Transformer}
import org.silkframework.rule.vocab.{GenericInfo, VocabularyClass, VocabularyProperty}
import org.silkframework.rule.{MappingTarget, TransformRule}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.runtime.validation.{RequestException, ValidationException}
import org.silkframework.serialization.json.InputJsonSerializer._
import org.silkframework.serialization.json.JsonSerializers.TransformRuleJsonFormat.readTransformRule
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
  final val METADATA = "metadata"
  final val OPERATOR = "operator"

  implicit object UriJsonFormat extends JsonFormat[Uri] {
    /**
      * Deserializes a value.
      */
    override def read(value: JsValue)(implicit readContext: ReadContext): Uri = {
      Uri.parse(value.as[JsString].value, readContext.prefixes)
    }

    /**
      * Serializes a value.
      */
    override def write(value: Uri)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsString(writeContext.prefixes.shorten(value.uri))
    }
  }

  implicit object JsonMetaDataFormat extends JsonFormat[MetaData] {

    final val LABEL = "label"
    final val DESCRIPTION = "description"

    override def read(value: JsValue)(implicit readContext: ReadContext): MetaData = {
      MetaData(
        label = stringValueOption(value, LABEL).getOrElse(""),
        description = stringValueOption(value, DESCRIPTION).getOrElse("")
      )
    }

    override def write(value: MetaData)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        LABEL -> JsString(value.label),
        DESCRIPTION -> JsString(value.description)
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
    stringValueOption(json, attributeName) match {
      case Some(value) =>
        value
      case None =>
        throw JsonParseException("Attribute '" + attributeName + "' not found!")
    }
  }

  def booleanValueOption(json: JsValue, attributeName: String): Option[Boolean] = {
    (json \ attributeName).toOption match {
      case Some(jsBoolean: JsBoolean) =>
        Some(jsBoolean.value)
      case Some(_) =>
        throw JsonParseException("Value for attribute '" + attributeName + "' is not a boolean!")
      case None =>
        None
    }
  }

  def stringValueOption(json: JsValue, attributeName: String): Option[String] = {
    (json \ attributeName).toOption match {
      case Some(jsString: JsString) =>
        Some(jsString.value)
      case Some(_) =>
        throw JsonParseException("Value for attribute '" + attributeName + "' is not a String!")
      case None =>
        None
    }
  }

  def optionalValue(json: JsValue, attributeName: String): Option[JsValue] = {
    (json \ attributeName).toOption.filterNot(_ == JsNull)
  }

  def requiredValue(json: JsValue, attributeName: String): JsValue = {
    json \ attributeName match {
      case JsDefined(value) if value != JsNull =>
        value
      case _ =>
        throw JsonParseException("Attribute '" + attributeName + "' not found!")
    }
  }

  def silkPath(id: String, pathStr: String)(implicit readContext: ReadContext): Path = {
    try {
      Path.parse(pathStr)(readContext.prefixes)
    } catch {
      case ex: Exception => throw new ValidationException(ex.getMessage, id, "path")
    }
  }

  def identifier(json: JsValue, defaultId: String)(implicit readContext: ReadContext): Identifier = {
    optionalValue(json, ID) match {
      case Some(JsString(id)) =>
        id
      case Some(_) =>
        throw JsonParseException("Value for attribute '" + ID + "' is not a String!")
      case None =>
        readContext.identifierGenerator.generate(defaultId)
    }
  }

  def metaData(json: JsValue)(implicit readContext: ReadContext): MetaData = {
    optionalValue(json, METADATA) match {
      case Some(metaDataJson) =>
        JsonMetaDataFormat.read(metaDataJson)
      case None =>
        MetaData.empty
    }
  }

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
          ),
        metaData = metaData(value))
    }

    override def write(value: DatasetTask)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        ID -> JsString(value.id.toString),
        TYPE -> JsString(value.plugin.plugin.id.toString),
        PARAMETERS -> Json.toJson(value.plugin.parameters),
        METADATA -> toJson(value.metaData)
      )

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
  implicit object TransformInputJsonFormat extends JsonFormat[TransformInput] {
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
    final val IS_BACKWARD_PROPERTY = "isBackwardProperty"

    override def read(value: JsValue)(implicit readContext: ReadContext): MappingTarget = {
      val uri = stringValue(value, URI)
      val valueType = fromJson[ValueType](mustBeDefined(value, VALUE_TYPE))
      val isBackwardProperty = booleanValueOption(value, IS_BACKWARD_PROPERTY).getOrElse(false)
      MappingTarget(Uri.parse(uri, readContext.prefixes), valueType, isBackwardProperty = isBackwardProperty)
    }

    override def write(value: MappingTarget)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(
        Seq(
          URI -> JsString(value.propertyUri.serialize(writeContext.prefixes)),
          VALUE_TYPE -> toJson(value.valueType),
          IS_BACKWARD_PROPERTY -> JsBoolean(value.isBackwardProperty)
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
      RootMappingRule(identifier(value, "root"), fromJson[MappingRules](mustBeDefined(value, RULES_PROPERTY)), metaData(value))
    }

    /**
      * Serializes a value.
      */
    override def write(value: RootMappingRule)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(
        Seq(
          TYPE -> JsString("root"),
          ID -> JsString(value.id),
          RULES_PROPERTY -> toJson(value.rules),
          METADATA -> toJson(value.metaData)
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
      val name = identifier(value, "type")
      val typeUri = stringValue(value, TYPE_PROPERTY)
      TypeMapping(name, Uri.parse(typeUri, readContext.prefixes), metaData(value))
    }

    /**
      * Serializes a value.
      */
    override def write(value: TypeMapping)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(
        Seq(
          TYPE -> JsString("type"),
          ID -> JsString(value.id),
          TYPE_PROPERTY -> JsString(value.typeUri.serialize(writeContext.prefixes)),
          METADATA -> toJson(value.metaData)
        )
      )
    }
  }

  /**
    * Pattern URI Mapping
    */
  implicit object PatternUriMappingJsonFormat extends JsonFormat[PatternUriMapping] {
    final val PATTERN_PROPERTY: String = "pattern"

    /**
      * Deserializes a value.
      */
    override def read(value: JsValue)(implicit readContext: ReadContext): PatternUriMapping = {
      val name = identifier(value, "uri")
      val pattern = stringValue(value, PATTERN_PROPERTY)
      PatternUriMapping(name, pattern, metaData(value))(readContext.prefixes)
    }

    /**
      * Serializes a value.
      */
    override def write(value: PatternUriMapping)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(
        Seq(
          TYPE -> JsString("uri"),
          ID -> JsString(value.id),
          PATTERN_PROPERTY -> JsString(value.pattern),
          METADATA -> toJson(value.metaData)
        )
      )
    }
  }

  /**
    * Complex URI Mapping
    */
  implicit object ComplexUriMappingJsonFormat extends JsonFormat[ComplexUriMapping] {

    /**
      * Deserializes a value.
      */
    override def read(value: JsValue)(implicit readContext: ReadContext): ComplexUriMapping = {
      ComplexUriMapping(
        id = identifier(value, "uri"),
        operator = fromJson[Input]((value \ OPERATOR).get),
        metaData(value)
      )
    }

    /**
      * Serializes a value.
      */
    override def write(rule: ComplexUriMapping)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(
        Seq(
          TYPE -> JsString("complexUri"),
          ID -> JsString(rule.id),
          OPERATOR -> toJson(rule.operator),
          METADATA -> toJson(rule.metaData)
        )
      )
    }
  }

  /**
    * URI Mapping.
    * Delegates serialization to the corresponding actual sub type.
    */
  implicit object UriMappingJsonFormat extends JsonFormat[UriMapping] {

    override def read(value: JsValue)(implicit readContext: ReadContext): UriMapping = {
      stringValue(value, TYPE) match {
        case "uri" =>
          fromJson[PatternUriMapping](value)
        case "complexUri" =>
          fromJson[ComplexUriMapping](value)
        case mappingType: String =>
          throw new ValidationException(s"Only 'uri' and 'complexUri' mapping types are allowed to be used as URI mappings. Got: '$mappingType'.")
      }
    }

    override def write(value: UriMapping)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      value match {
        case pattern: PatternUriMapping =>
          toJson[PatternUriMapping](pattern)
        case complex: ComplexUriMapping =>
          toJson[ComplexUriMapping](complex)
      }
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
      val name = identifier(value, "direct")
      val sourcePath = silkPath(name, stringValue(value, SOURCE_PATH_PROPERTY))
      val mappingTarget = fromJson[MappingTarget](requiredValue(value, MAPPING_TARGET_PROPERTY))
      DirectMapping(name, sourcePath, mappingTarget, metaData(value))
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
          MAPPING_TARGET_PROPERTY -> toJson(value.mappingTarget),
          METADATA -> toJson(value.metaData)
        )
      )
    }
  }

  /**
    * Object Mapping
    */
  implicit object ObjectMappingJsonFormat extends JsonFormat[ObjectMapping] {
    final val SOURCE_PATH: String = "sourcePath"
    final val MAPPING_TARGET: String = "mappingTarget"
    final val RULES: String = "rules"

    /**
      * Deserializes a value.
      */
    override def read(value: JsValue)(implicit readContext: ReadContext): ObjectMapping = {
      val name = identifier(value, "object")
      val sourcePath = silkPath(name, stringValue(value, SOURCE_PATH))
      val mappingTarget = optionalValue(value, MAPPING_TARGET).map(fromJson[MappingTarget])
      val children = fromJson[MappingRules](mustBeDefined(value, RULES))
      ObjectMapping(name, sourcePath, mappingTarget, children, metaData(value))(readContext.prefixes)
    }

    /**
      * Serializes a value.
      */
    override def write(value: ObjectMapping)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        TYPE -> JsString("object"),
        ID -> JsString(value.id),
        SOURCE_PATH -> JsString(value.sourcePath.serialize(writeContext.prefixes)),
        MAPPING_TARGET -> value.target.map(toJson(_)).getOrElse(JsNull).asInstanceOf[JsValue],
        RULES -> toJson(value.rules),
        METADATA -> toJson(value.metaData)
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
          fromJson[PatternUriMapping](jsValue)
        case "complexUri" =>
          fromJson[ComplexUriMapping](jsValue)
        case "direct" =>
          fromJson[DirectMapping](jsValue)
        case "object" =>
          fromJson[ObjectMapping](jsValue)
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
        id = identifier(jsValue, "complex"),
        operator = fromJson[Input]((jsValue \ OPERATOR).get),
        target = mappingTarget,
        metaData(jsValue)
      )
      TransformRule.simplify(complex)(readContext.prefixes)
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
        case u: PatternUriMapping =>
          toJson(u)
        case u: ComplexUriMapping =>
          toJson(u)
        case d: DirectMapping =>
          toJson(d)
        case o: ObjectMapping =>
          toJson(o)
        case _ =>
          writeTransformRule(rule)
      }
    }

    private def writeTransformRule(rule: TransformRule)(implicit writeContext: WriteContext[JsValue]) = {
      JsObject(
        Seq(
          TYPE -> JsString("complex"),
          ID -> JsString(rule.id),
          OPERATOR -> toJson(rule.operator),
          "sourcePaths" -> JsArray(rule.sourcePaths.map(_.serialize(writeContext.prefixes)).map(JsString)),
          METADATA -> toJson(rule.metaData)
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
        mappingRule = optionalValue(value, RULES_PROPERTY).map(fromJson[RootMappingRule]).getOrElse(RootMappingRule.empty),
        outputs = mustBeJsArray(mustBeDefined(value, OUTPUTS))(_.value.map(v => Identifier(v.toString()))),
        targetVocabularies = mustBeJsArray(mustBeDefined(value, TARGET_VOCABULARIES))(_.value.map(_.as[JsString].value))
      )
    }

    /**
      * Serializes a value.
      */
    override def write(value: TransformSpec)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        SELECTION -> toJson(value.selection),
        RULES_PROPERTY -> toJson(value.mappingRule),
        OUTPUTS -> JsArray(value.outputs.map(id => JsString(id.toString))),
        TARGET_VOCABULARIES -> JsArray(value.targetVocabularies.toSeq.map(JsString))

      )
    }
  }

  /**
    * Transform Task
    */
  implicit object TransformTaskFormat extends TaskJsonFormat[TransformSpec]

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

  /** Vocabulary */
  implicit object GenericInfoJsonFormat extends JsonFormat[GenericInfo] {
    final val DESCRIPTION: String = "description"
    final val LABEL: String = "label"

    override def read(value: JsValue)(implicit readContext: ReadContext): GenericInfo = {
      GenericInfo(
        uri = stringValue(value, URI),
        label = stringValueOption(value, LABEL),
        description = stringValueOption(value, DESCRIPTION)
      )
    }

    override def write(value: GenericInfo)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(
        Seq(
          URI -> UriJsonFormat.write(value.uri)
        ) ++ value.label.map { l =>
          LABEL -> JsString(l)
        } ++ value.description.map { d =>
          DESCRIPTION -> JsString(d)
        }
      )
    }
  }

  /** VocabularyProperty */
  implicit object VocabularyPropertyJsonFormat extends JsonFormat[VocabularyProperty] {
    override def read(value: JsValue)(implicit readContext: ReadContext): VocabularyProperty = {
      throw new RuntimeException("De-serializing VocabularyProperty JSON strings is not supported!")
    }

    override def write(value: VocabularyProperty)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(
        Seq(
          "genericInfo" -> GenericInfoJsonFormat.write(value.info)
        ) ++ value.domain.map { d =>
          "domain" -> UriJsonFormat.write(d.info.uri)
        } ++ value.range.map { r =>
          "range" -> UriJsonFormat.write(r.info.uri)
        }
      )
    }
  }

  /** VocabularyClass */
  implicit object VocabularyClassJsonFormat extends JsonFormat[VocabularyClass] {
    override def read(value: JsValue)(implicit readContext: ReadContext): VocabularyClass = {
      throw new RuntimeException("De-serializing VocabularyClass JSON strings is not supported!")
    }

    override def write(value: VocabularyClass)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(
        Seq(
          "genericInfo" -> GenericInfoJsonFormat.write(value.info),
          "parentClasses" -> JsArray(value.parentClasses.map(uri => UriJsonFormat.write(uri)).toSeq)
        )
      )
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

case class JsonParseException(msg: String, cause: Option[Throwable] = None) extends RequestException(msg, cause) {
  /**
    * A short description of the error type.
    */
  override def errorTitle: String = "Could not parse JSON"

  /**
    * The HTTP error code that fits best to the given error type.
    */
  override def httpErrorCode: Option[Int] = Some(HttpURLConnection.HTTP_BAD_REQUEST)
}
