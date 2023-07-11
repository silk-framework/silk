package org.silkframework.serialization.json

import io.swagger.v3.oas.annotations.media.Schema
import org.silkframework.config._
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.{Dataset, DatasetSpec, DatasetTask}
import org.silkframework.entity._
import org.silkframework.rule.TransformSpec.TargetVocabularyListParameter
import org.silkframework.rule._
import org.silkframework.rule.evaluation.ReferenceLinks
import org.silkframework.rule.input.{Input, PathInput, TransformInput, Transformer}
import org.silkframework.rule.similarity._
import org.silkframework.rule.util.UriPatternParser
import org.silkframework.rule.vocab.{GenericInfo, Vocabulary, VocabularyClass, VocabularyProperty}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin._
import org.silkframework.runtime.serialization.{ReadContext, Serialization, WriteContext}
import org.silkframework.runtime.validation.{BadUserInputException, ValidationException}
import org.silkframework.serialization.json.EntitySerializers.EntitySchemaJsonFormat
import org.silkframework.serialization.json.InputJsonSerializer._
import org.silkframework.serialization.json.JsonHelpers._
import org.silkframework.serialization.json.JsonSerializers.ObjectMappingJsonFormat.MAPPING_TARGET
import org.silkframework.serialization.json.JsonSerializers.{ID, _}
import org.silkframework.serialization.json.LinkingSerializers._
import org.silkframework.serialization.json.MetaDataSerializers._
import org.silkframework.serialization.json.PluginSerializers.{ParameterValuesJsonFormat, PluginJsonFormat}
import org.silkframework.util.{DPair, Identifier, IdentifierUtils, Uri}
import org.silkframework.workspace.{LoadedTask, TaskLoadingError}
import org.silkframework.workspace.activity.transform.{CachedEntitySchemata, VocabularyCacheValue}
import org.silkframework.workspace.annotation.{StickyNote, UiAnnotations}
import play.api.libs.json._

import scala.reflect.ClassTag
import scala.util.control.NonFatal

/**
  * Serializers for JSON.
  */
object JsonSerializers {

  final val ID = "id"
  final val TYPE = "type"
  final val DATA = "data"
  final val GENERIC_INFO = "genericInfo"
  final val PARAMETERS = "parameters"
  final val TEMPLATES = "templates"
  final val URI = "uri"
  final val METADATA = "metadata"
  final val OPERATOR = "operator"
  // Task types
  final val TASKTYPE = "taskType"
  final val TASK_TYPE_DATASET = "Dataset"
  final val TASK_TYPE_CUSTOM_TASK = "CustomTask"
  final val TASK_TYPE_TRANSFORM = "Transform"
  final val TASK_TYPE_LINKING = "Linking"
  final val TASK_TYPE_WORKFLOW = "Workflow"
  // Plugin types
  final val PLUGIN_TYPE = "pluginType"
  final val AGGREGATION_OPERATOR = "AggregationOperator"
  final val TRANSFORM_OPERATOR = "TransformOperator"
  final val COMPARISON_OPERATOR = "ComparisonOperator"
  // Rule tasks
  final val LAYOUT = "layout"
  final val UI_ANNOTATIONS = "uiAnnotations"


  /** Sticky note */
  implicit object StickyNoteJsonFormat extends JsonFormat[StickyNote] {
    final val CONTENT = "content"
    final val COLOR = "color"
    final val POSITION = "position"
    final val DIMENSION = "dimension"

    override def read(json: JsValue)(implicit readContext: ReadContext): StickyNote = {
      val id = stringValue(json, ID)
      val content = stringValue(json, CONTENT)
      val color = stringValue(json, COLOR)
      val position = fromJsonValidated[(Double, Double)](mustBeDefined(json, POSITION))
      val dimension = fromJsonValidated[(Double, Double)](mustBeDefined(json, DIMENSION))
      StickyNote(id, content, color, position, dimension)
    }

    override def write(stickyNote: StickyNote)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        ID -> stickyNote.id,
        CONTENT -> stickyNote.content,
        COLOR -> stickyNote.color,
        POSITION -> Json.toJson(stickyNote.position),
        DIMENSION -> Json.toJson(stickyNote.dimension)
      )
    }
  }

  implicit object UiAnnotationsJsonFormat extends JsonFormat[UiAnnotations] {
    final val STICKY_NOTES = "stickyNotes"

    override def read(value: JsValue)(implicit readContext: ReadContext): UiAnnotations = {
      val stickyNotesJson = arrayValue(value, STICKY_NOTES)
      val stickyNotes = stickyNotesJson.value.map(value => fromJson[StickyNote](value)).toIndexedSeq
      UiAnnotations(stickyNotes)
    }

    override def write(value: UiAnnotations)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        STICKY_NOTES -> JsArray(value.stickyNotes.map(toJson[StickyNote]))
      )
    }
  }



  implicit object StringJsonFormat extends JsonFormat[String] {
    /**
      * Deserializes a value.
      */
    override def read(value: JsValue)(implicit readContext: ReadContext): String = {
      value.as[JsString].value
    }

    /**
      * Serializes a value.
      */
    override def write(value: String)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsString(value)
    }
  }

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

  implicit object DatasetSpecJsonFormat extends JsonFormat[GenericDatasetSpec] {

    private val URI_PROPERTY = "uriProperty"
    private val READ_ONLY = "readOnly"

    private val pluginFormat = new PluginJsonFormat[Dataset]

    override def typeNames: Set[String] = Set(JsonSerializers.TASK_TYPE_DATASET)

    override def read(value: JsValue)(implicit readContext: ReadContext): GenericDatasetSpec = {
      new DatasetSpec(
        plugin = pluginFormat.read(value),
        uriAttribute = stringValueOption(value, URI_PROPERTY).filter(_.trim.nonEmpty).map(v => Uri(v.trim)),
        readOnly = booleanValueOption(value, READ_ONLY).getOrElse(false)
      )
    }

    override def write(value: GenericDatasetSpec)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      var json =
        Json.obj(
          TASKTYPE -> JsString(JsonSerializers.TASK_TYPE_DATASET),
          READ_ONLY -> value.readOnly
        )
      for(property <- value.uriAttribute) {
        json += (URI_PROPERTY -> JsString(property.uri))
      }
      json ++= pluginFormat.write(value.plugin)
      json
    }
  }

  implicit object CustomTaskJsonFormat extends JsonFormat[CustomTask] {

    private val pluginFormat = new PluginJsonFormat[CustomTask]

    override def typeNames: Set[String] = Set(TASK_TYPE_CUSTOM_TASK)

    override def read(value: JsValue)(implicit readContext: ReadContext): CustomTask = {
      pluginFormat.read(value)
    }

    override def write(value: CustomTask)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        TASKTYPE -> JsString(TASK_TYPE_CUSTOM_TASK),
      ) ++ pluginFormat.write(value)
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
          PATH -> JsString(value.path.serialize()(writeContext.prefixes))
        )
      )
    }
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
      try {
        val transformerPluginId = stringValue(value, FUNCTION)
        val transformer = Transformer(PluginBackwardCompatibility.transformerIdMapping.getOrElse(transformerPluginId, transformerPluginId), ParameterValuesJsonFormat.read(value))
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
          FUNCTION -> JsString(value.transformer.pluginSpec.id),
          INPUTS -> JsArray(value.inputs.map(toJson[Input])),
        )
      ) ++ ParameterValuesJsonFormat.write(value.transformer.parameters)
    }
  }

  def readParameters(value: JsValue): Map[String, String] = {
    mustBeJsObject(mustBeDefined(value, PARAMETERS)) { array =>
      Json.fromJson[Map[String, String]](array) match {
        case JsSuccess(arr, _) => arr
        case error @ JsError(_) => throw new ValidationException("Could not read parameters from JSON. Details: " + JsError.toJson(error))
      }
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
            case ValueType.OUTDATED_AUTO_DETECT_ID => ValueType.STRING
            case ValueType.CUSTOM_VALUE_TYPE_ID =>
              val uriString = stringValue(value, URI)
              val uri = Uri.parse(uriString, readContext.prefixes)
              CustomValueType(uri.uri)
            case ValueType.LANGUAGE_VALUE_TYPE_ID =>
              val lang = stringValue(value, LANG)
              LanguageValueType(lang)
          }
        case Right(valueType) =>
          valueType
      }
    }

    override def write(value: ValueType)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      val typeId = value.id
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
    final val IS_ATTRIBUTE = "isAttribute"

    override def read(value: JsValue)(implicit readContext: ReadContext): MappingTarget = {
      val uri = stringValue(value, URI)
      val valueType = fromJson[ValueType](mustBeDefined(value, VALUE_TYPE))
      val isBackwardProperty = booleanValueOption(value, IS_BACKWARD_PROPERTY).getOrElse(false)
      val isAttribute = booleanValueOption(value, IS_ATTRIBUTE).getOrElse(false)
      MappingTarget(Uri.parse(uri, readContext.prefixes), valueType, isBackwardProperty, isAttribute)
    }

    override def write(value: MappingTarget)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(
        Seq(
          URI -> JsString(value.propertyUri.serialize(writeContext.prefixes)),
          VALUE_TYPE -> toJson(value.valueType),
          IS_BACKWARD_PROPERTY -> JsBoolean(value.isBackwardProperty),
          IS_ATTRIBUTE -> JsBoolean(value.isAttribute)
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
        array.value.map(TypeMappingJsonFormat.read).toSeq
      }
      val propertyRules = mustBeJsArray(mustBeDefined(value, PROPERTY_RULES)) { array =>
        array.value.map(TransformRuleJsonFormat.read).toSeq
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
      val mappingRules = fromJson[MappingRules](mustBeDefined(value, RULES_PROPERTY))
      val id = identifier(value, RootMappingRule.defaultId)
      val mappingTarget = optionalValue(value, MAPPING_TARGET).map(fromJson[MappingTarget]).getOrElse(RootMappingRule.defaultMappingTarget)
      RootMappingRule(id = id, rules = mappingRules, mappingTarget = mappingTarget, metaData = metaData(value))
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
          MAPPING_TARGET -> toJson(value.mappingTarget),
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
      val typeUri =  Uri.parse(stringValue(value, TYPE_PROPERTY), readContext.prefixes)
      val typeName = typeUri.localName.getOrElse("type")
      val name = identifier(value, typeName)
      TypeMapping(name,typeUri, metaData(value))
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
      implicit val prefixes: Prefixes = readContext.prefixes
      val name = identifier(value, "uri")
      val pattern = stringValue(value, PATTERN_PROPERTY).trim()
      if(readContext.validationEnabled) {
        UriPatternParser.parseIntoSegments(pattern, allowIncompletePattern = false).validateAndThrow()
      }
      PatternUriMapping(name, pattern.trim(), metaData(value), readContext.prefixes)
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

  /** Rule layout */
  implicit object RuleLayoutJsonFormat extends JsonFormat[RuleLayout] {
    final val NODE_POSITIONS = "nodePositions"

    override def read(value: JsValue)(implicit readContext: ReadContext): RuleLayout = {
      val nodePositions = JsonHelpers.fromJsonValidated[Map[String, (Int, Int)]](mustBeDefined(value, NODE_POSITIONS))
      RuleLayout(nodePositions)
    }

    override def write(value: RuleLayout)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        NODE_POSITIONS -> Json.toJson(value.nodePositions)
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
        metaData(value),
        layout = optionalValue(value, LAYOUT).map(fromJson[RuleLayout]).getOrElse(RuleLayout()),
        uiAnnotations = optionalValue(value, UI_ANNOTATIONS).map(fromJson[UiAnnotations]).getOrElse(UiAnnotations()),
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
          METADATA -> toJson(rule.metaData),
          LAYOUT -> toJson(rule.layout),
          UI_ANNOTATIONS -> toJson(rule.uiAnnotations)
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
      val mappingTarget = fromJson[MappingTarget](requiredValue(value, MAPPING_TARGET_PROPERTY))
      val mappingName = mappingTarget.propertyUri.localName.getOrElse("ValueMapping")
      val id = identifier(value, mappingName)
      val sourcePath = silkPath(id, stringValue(value, SOURCE_PATH_PROPERTY))
      DirectMapping(id, sourcePath, mappingTarget, metaData(value))
    }

    /**
      * Serializes a value.
      */
    override def write(value: DirectMapping)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(
        Seq(
          TYPE -> JsString("direct"),
          ID -> JsString(value.id),
          SOURCE_PATH_PROPERTY -> JsString(value.sourcePath.serialize()(writeContext.prefixes)),
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
      val children = fromJson[MappingRules](mustBeDefined(value, RULES))
      val mappingTarget = optionalValue(value, MAPPING_TARGET).map(fromJson[MappingTarget])
      val mappingName = mappingTarget.flatMap(_.propertyUri.localName).getOrElse("ObjectMapping")
      val id = identifier(value, mappingName)
      val sourcePath = silkPath(id, stringValue(value, SOURCE_PATH))
      ObjectMapping(id, sourcePath, mappingTarget, children, metaData(value), readContext.prefixes)
    }

    /**
      * Serializes a value.
      */
    override def write(value: ObjectMapping)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        TYPE -> JsString("object"),
        ID -> JsString(value.id),
        SOURCE_PATH -> JsString(value.sourcePath.serialize()(writeContext.prefixes)),
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
          readAndConvertComplexTransformRule(jsValue)
      }
    }

    private def readAndConvertComplexTransformRule(jsValue: JsValue)
                                 (implicit readContext: ReadContext)= {
      val complex = ComplexMappingJsonFormat.read(jsValue)
      // Simplify to other value mapping rule if possible
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
        case valueTransformRule: ValueTransformRule =>
          writeTransformRule(valueTransformRule)
      }
    }

    private def writeTransformRule(valueTransformRule: ValueTransformRule)
                                  (implicit writeContext: WriteContext[JsValue]): JsValue = {
      ComplexMappingJsonFormat.writeValueTransformRuleAsComplexRule(valueTransformRule)
    }
  }

  implicit object ComplexMappingJsonFormat extends JsonFormat[ComplexMapping] {
    override def read(jsValue: JsValue)
                     (implicit readContext: ReadContext): ComplexMapping = {
      val mappingTarget = (jsValue \ "mappingTarget").
        toOption.
        map(fromJson[MappingTarget])
      val mappingName = mappingTarget.flatMap(_.propertyUri.localName).getOrElse("ValueMapping")
      val id = identifier(jsValue, mappingName)
      ComplexMapping(
        id = id,
        operator = fromJson[Input]((jsValue \ OPERATOR).get),
        target = mappingTarget,
        metaData(jsValue),
        layout = optionalValue(jsValue, LAYOUT).map(fromJson[RuleLayout]).getOrElse(RuleLayout()),
        uiAnnotations = optionalValue(jsValue, UI_ANNOTATIONS).map(fromJson[UiAnnotations]).getOrElse(UiAnnotations())
      )
    }

    def writeValueTransformRuleAsComplexRule(valueTransformRule: ValueTransformRule)
                                            (implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(
        Seq(
          TYPE -> JsString("complex"),
          ID -> JsString(valueTransformRule.id),
          OPERATOR -> toJson(valueTransformRule.operator),
          "sourcePaths" -> JsArray(valueTransformRule.sourcePaths.map(_.toUntypedPath.serialize()(writeContext.prefixes)).map(JsString)),
          METADATA -> toJson(valueTransformRule.metaData),
          LAYOUT -> toJson(valueTransformRule.layout),
          UI_ANNOTATIONS -> toJson(valueTransformRule.uiAnnotations)
        ) ++
          valueTransformRule.target.map("mappingTarget" -> toJson(_))
      )
    }

    override def write(complexMapping: ComplexMapping)
                      (implicit writeContext: WriteContext[JsValue]): JsValue = {
      writeValueTransformRuleAsComplexRule(complexMapping)
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
        typeUri = Uri.parse(stringValue(value, TYPE_URI), readContext.prefixes),
        restriction = Restriction.parse(stringValue(value, RESTRICTION))(readContext.prefixes)
      )
    }

    /**
      * Serializes a value.
      */
    override def write(value: DatasetSelection)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        INPUT_ID -> value.inputId.toString,
        TYPE_URI -> value.typeUri.serialize(writeContext.prefixes),
        RESTRICTION -> value.restriction.serialize
      )
    }
  }

  /**
    * Transform Specification
    */
  implicit object TransformSpecJsonFormat extends JsonFormat[TransformSpec] {
    final val SELECTION = "selection"
    final val RULES_PROPERTY: String = "mappingRule"
    final val OUTPUT: String = "output"
    final val ERROR_OUTPUT: String = "errorOutput"
    final val TARGET_VOCABULARIES: String = "targetVocabularies"
    final val ABORT_IF_ERRORS_OCCUR: String = "abortIfErrorsOccur"

    /** Deprecated property names */
    final val DEPRECATED_RULES_PROPERTY: String = "root"
    // A transform task can only have one output (outside of a workflow)
    final val DEPRECATED_OUTPUTS: String = "outputs"

    private val pluginFormat = new PluginJsonFormat[TransformSpec](Some(ClassPluginDescription.create(classOf[TransformSpec])))

    override def typeNames: Set[String] = Set(TASK_TYPE_TRANSFORM)

    /**
      * Deserializes a value.
      */
    override def read(value: JsValue)(implicit readContext: ReadContext): TransformSpec = {
      optionalValue(value, PARAMETERS) match {
        case None =>
          readDeprecated(value)
        case _ =>
          val parametersObj = objectValue(value, PARAMETERS)
          val objParameters = ParameterValues(Map(
            RULES_PROPERTY -> ParameterObjectValue(optionalValue(parametersObj, RULES_PROPERTY).map(fromJson[RootMappingRule]).getOrElse(RootMappingRule.empty))
          ))
          pluginFormat.read(value, objParameters)
      }
    }

    // Reads the deprecated JSON model
    private def readDeprecated(value: JsValue)(implicit readContext: ReadContext): TransformSpec = {
      TransformSpec(
        selection = fromJson[DatasetSelection](mustBeDefined(value, SELECTION)),
        mappingRule = optionalValue(value, DEPRECATED_RULES_PROPERTY).map(fromJson[RootMappingRule]).getOrElse(RootMappingRule.empty),
        output = mustBeJsArray(mustBeDefined(value, DEPRECATED_OUTPUTS))(_.value.map(v => Identifier(v.as[JsString].value))).headOption,
        targetVocabularies = TargetVocabularyListParameter(mustBeJsArray(mustBeDefined(value, TARGET_VOCABULARIES))(_.value.map(_.as[JsString].value)))
      )
    }

    /**
      * Serializes a value.
      */
    override def write(value: TransformSpec)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        TASKTYPE -> TASK_TYPE_TRANSFORM,
      ) ++ pluginFormat.write(value)
    }
  }

  implicit object ComparisonJsonFormat extends JsonFormat[Comparison] {
    final val WEIGHT = "weight"
    final val THRESHOLD = "threshold"
    final val INDEXING = "indexing"
    final val METRIC = "metric"
    final val SOURCEINPUT = "sourceInput"
    final val TARGETINPUT = "targetInput"
    final val COMPARISON_TYPE = "Comparison"

    override def typeNames: Set[String] = Set(COMPARISON_TYPE)

    override def read(value: JsValue)(implicit readContext: ReadContext): Comparison = {
      val metricPluginId = stringValue(value, METRIC)
      val metric = DistanceMeasure(PluginBackwardCompatibility.distanceMeasureIdMapping.getOrElse(metricPluginId, metricPluginId), ParameterValuesJsonFormat.read(value))

      Comparison(
        id = identifier(value, "comparison"),
        weight = numberValue(value, WEIGHT).intValue,
        threshold = numberValue(value, THRESHOLD).doubleValue,
        indexing = booleanValue(value, INDEXING),
        metric = metric,
        inputs =
          DPair(
            fromJson[Input](mustBeDefined(value, SOURCEINPUT)),
            fromJson[Input](mustBeDefined(value, TARGETINPUT))
          )
      )
    }

    override def write(value: Comparison)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        ID -> value.id.toString,
        TYPE -> COMPARISON_TYPE,
        WEIGHT -> value.weight,
        THRESHOLD -> value.threshold,
        INDEXING -> value.indexing,
        METRIC -> value.metric.pluginSpec.id.toString,
        SOURCEINPUT -> toJson(value.inputs.source),
        TARGETINPUT -> toJson(value.inputs.target)
      ) ++ ParameterValuesJsonFormat.write(value.metric.parameters)
    }
  }

  implicit object AggregationJsonFormat extends JsonFormat[Aggregation] {
    final val REQUIRED = "required"
    final val WEIGHT = "weight"
    final val AGGREGATOR = "aggregator"
    final val OPERATORS = "inputs"
    final val AGGREGATION_TYPE = "Aggregation"

    override def typeNames: Set[String] = Set(AGGREGATION_TYPE)

    override def read(value: JsValue)(implicit readContext: ReadContext): Aggregation = {
      val aggregator = Aggregator(stringValue(value, AGGREGATOR), ParameterValuesJsonFormat.read(value))
      val inputs = mustBeJsArray(mustBeDefined(value, OPERATORS)) { jsArray =>
        jsArray.value.map(fromJson[SimilarityOperator](_)(SimilarityOperatorJsonFormat, readContext))
      }

      Aggregation(
        id = identifier(value, "aggregation"),
        weight = numberValue(value, WEIGHT).intValue,
        aggregator = aggregator,
        operators = inputs.toIndexedSeq
      )
    }

    override def write(value: Aggregation)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        ID -> value.id.toString,
        TYPE -> AGGREGATION_TYPE,
        WEIGHT -> value.weight,
        AGGREGATOR -> value.aggregator.pluginSpec.id.toString,
        OPERATORS -> value.operators.map(toJson(_))
      ) ++ ParameterValuesJsonFormat.write(value.aggregator.parameters)
    }
  }

  implicit object SimilarityOperatorJsonFormat extends JsonFormat[SimilarityOperator] {

    override def read(value: JsValue)(implicit readContext: ReadContext): SimilarityOperator = {
      stringValue(value, TYPE) match {
        case ComparisonJsonFormat.COMPARISON_TYPE =>
          ComparisonJsonFormat.read(value)
        case AggregationJsonFormat.AGGREGATION_TYPE =>
          AggregationJsonFormat.read(value)
        case typeName =>
          throw JsonParseException(s"Invalid type name $typeName")
      }
    }

    override def write(value: SimilarityOperator)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      value match {
        case comparison: Comparison => toJson(comparison)
        case aggregation: Aggregation => toJson(aggregation)
      }
    }
  }

  implicit object LinkFilterJsonFormat extends JsonFormat[LinkFilter] {
    final val LIMIT = "limit"
    final val THRESHOLD = "threshold"
    final val UNAMBIGUOUS = "unambiguous"

    override def read(value: JsValue)(implicit readContext: ReadContext): LinkFilter = {
      LinkFilter(
        limit = numberValueOption(value, LIMIT).map(_.intValue),
        unambiguous = booleanValueOption(value, UNAMBIGUOUS)
      )
    }

    override def write(value: LinkFilter)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        LIMIT -> value.limit.map(JsNumber(_)),
        UNAMBIGUOUS -> value.unambiguous.map(JsBoolean)
      )
    }
  }


  implicit object LinkageRuleJsonFormat extends JsonFormat[LinkageRule] {
    final val OPERATOR = "operator"
    final val FILTER = "filter"
    final val LINKTYPE = "linkType"
    final val INVERSELINKTYPE = "inverseLinkType"
    final val IS_REFLEXIVE = "isReflexive"
    final val UI_ANNOTATIONS = "uiAnnotations"

    override def read(value: JsValue)(implicit readContext: ReadContext): LinkageRule = {
      LinkageRule(
        operator = optionalValue(value, OPERATOR).map(fromJson[SimilarityOperator]),
        filter = fromJson[LinkFilter](mustBeDefined(value, FILTER)),
        linkType = fromJson[Uri](mustBeDefined(value, LINKTYPE)),
        inverseLinkType = optionalValue(value, INVERSELINKTYPE).map(fromJson[Uri](_)),
        isReflexive = booleanValueOption(value, IS_REFLEXIVE).getOrElse(true),
        layout = optionalValue(value, LAYOUT).map(fromJson[RuleLayout]).getOrElse(RuleLayout()),
        uiAnnotations = optionalValue(value, UI_ANNOTATIONS).map(fromJson[UiAnnotations]).getOrElse(UiAnnotations())
      )
    }

    override def write(value: LinkageRule)(implicit writeContext: WriteContext[JsValue]): JsObject = {
      Json.obj(
        OPERATOR -> value.operator.map(toJson(_)),
        FILTER -> toJson(value.filter),
        LINKTYPE -> toJson(value.linkType),
        INVERSELINKTYPE -> toJsonOpt(value.inverseLinkType),
        IS_REFLEXIVE -> value.isReflexive,
        LAYOUT -> toJson(value.layout),
        UI_ANNOTATIONS -> toJson(value.uiAnnotations)
      )
    }
  }

  implicit object LinkSpecJsonFormat extends JsonFormat[LinkSpec] {
    final val SOURCE = "source"
    final val TARGET = "target"
    final val RULE = "rule"
    final val OUTPUT = "output"
    final val REFERENCE_LINKS = "referenceLinks"
    final val LINK_LIMIT = "linkLimit"
    final val MATCHING_EXECUTION_TIMEOUT = "matchingExecutionTimeout"

    /** Deprecated properties */
    final val DEPRECATED_OUTPUTS = "outputs"

    private val pluginFormat = new PluginJsonFormat[LinkSpec](Some(ClassPluginDescription.create(classOf[LinkSpec])))

    override def typeNames: Set[String] = Set(TASK_TYPE_LINKING)

    override def read(value: JsValue)(implicit readContext: ReadContext): LinkSpec = {
      optionalValue(value, PARAMETERS) match {
        case None =>
          deprecatedRead(value)
        case _ =>
          val jsonParameters = objectValue(value, PARAMETERS)
          val objParameters = ParameterValues(Map(
            RULE -> ParameterObjectValue(optionalValue(jsonParameters, RULE).map(fromJson[LinkageRule]).getOrElse(LinkageRule())),
            REFERENCE_LINKS -> ParameterObjectValue(optionalValue(jsonParameters, REFERENCE_LINKS).map(fromJson[ReferenceLinks]).getOrElse(ReferenceLinks.empty))
          ))
          pluginFormat.read(value, objParameters)
      }
    }

    private def deprecatedRead(value: JsValue)(implicit readContext: ReadContext): LinkSpec = {
      LinkSpec(
        source =
            fromJson[DatasetSelection](mustBeDefined(value, SOURCE)),
        target =
            fromJson[DatasetSelection](mustBeDefined(value, TARGET)),
        rule = optionalValue(value, RULE).map(fromJson[LinkageRule]).getOrElse(LinkageRule()),
        output = mustBeJsArray(mustBeDefined(value, DEPRECATED_OUTPUTS))(_.value.map(v => Identifier(v.as[JsString].value))).headOption,
        referenceLinks = optionalValue(value, REFERENCE_LINKS).map(fromJson[ReferenceLinks]).getOrElse(ReferenceLinks.empty),
        linkLimit = numberValueOption(value, LINK_LIMIT).map(_.intValue).getOrElse(LinkSpec.DEFAULT_LINK_LIMIT),
        matchingExecutionTimeout = numberValueOption(value, MATCHING_EXECUTION_TIMEOUT).map(_.intValue).getOrElse(LinkSpec.DEFAULT_EXECUTION_TIMEOUT_SECONDS)
      )
    }

    override def write(value: LinkSpec)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      val json =
        Json.obj(
          TASKTYPE -> TASK_TYPE_LINKING,
        ) ++ pluginFormat.write(value)
      json
    }
  }

  /**
    * Task
    */
  implicit object TaskSpecJsonFormat extends JsonFormat[TaskSpec] {

    // Holds all JSON formats for sub classes of TaskSpec.
    private lazy val taskSpecFormats: Seq[JsonFormat[TaskSpec]] = {
      Serialization.availableFormats.filter(f => f.isInstanceOf[JsonFormat[_]] && classOf[TaskSpec].isAssignableFrom(f.valueType) && f.valueType != classOf[TaskSpec])
        .map(_.asInstanceOf[JsonFormat[TaskSpec]])
    }

    /**
      * Retrieves the task format for a given task spec.
      *
      * @return The task format, if available. None, otherwise.
      */
    def taskSpecFormat(value: TaskSpec): Option[JsonFormat[TaskSpec]] = {
      taskSpecFormats.find(_.valueType.isAssignableFrom(value.getClass))
    }

    override def read(value: JsValue)(implicit readContext: ReadContext): TaskSpec = {
      val taskType = stringValue(value, TASKTYPE)
      taskSpecFormats.find(_.typeNames.contains(taskType)) match {
        case Some(format) =>
          format.read(value)
        case None =>
          throw new ValidationException(s"The encountered task type $taskType does not correspond to a known task type")
      }
    }

    override def write(value: TaskSpec)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      taskSpecFormat(value) match {
        case Some(format) =>
          format.write(value)
        case None =>
          throw new ValidationException(s"No serialization format found for class ${value.getClass.getName}")
      }
    }
  }

  /**
    * Task
    *
    * @param dependentTaskFormatter Converts dependent tasks to a different JSON format than the string ID.
    */
  class TaskJsonFormat[T <: TaskSpec : ClassTag](options: TaskFormatOptions = TaskFormatOptions(),
                                                 userContext: Option[UserContext] = None,
                                                 dependentTaskFormatter: Option[String => JsValue] = None)(implicit dataFormat: JsonFormat[T]) extends JsonFormat[LoadedTask[T]] {

    final val PROJECT = "project"
    final val PROPERTIES = "properties"
    final val RELATIONS = "relations"
    final val SCHEMATA = "schemata"
    final val KEY = "key"
    final val VALUE = "value"


    /**
      * Deserializes a value.
      */
    override def read(value: JsValue)(implicit readContext: ReadContext): LoadedTask[T] = {
      var taskId = ""
      try {
        val id: Identifier = stringValueOption(value, ID).map(_.trim).filter(_.nonEmpty).map(Identifier.apply).getOrElse {
          // Generate unique ID from label if no ID was supplied
          val md = metaData(value)
          md.label match {
            case Some(label) if label.trim.nonEmpty =>
              IdentifierUtils.generateTaskId(label)
            case _ =>
              throw BadUserInputException("The label must not be empty if no ID is provided!")
          }
        }
        taskId = id.toString
        // In older serializations the task data has been directly attached to this JSON object
        val dataJson = optionalValue(value, DATA).getOrElse(value)
        val task = PlainTask(
          id = id,
          data = fromJson[T](dataJson),
          metaData = metaData(value)
        )

        LoadedTask.success(task)
      } catch {
        case NonFatal(ex) =>
          LoadedTask.failed(TaskLoadingError(readContext.projectId, if(taskId.isEmpty) Identifier("__unknown__") else Identifier(taskId),
            ex, factoryFunction = None, originalParameterValues = None))
      }
    }

    /**
      * Serializes a value.
      */
    override def write(task:  LoadedTask[T])(implicit writeContext: WriteContext[JsValue]): JsValue = {
      // If any of the defaults is changed, the annotations on TaskFormatOptions need to be updated
      var json = Json.obj(ID -> JsString(task.id.toString))

      for(project <- writeContext.projectId) {
        json += PROJECT -> JsString(project)
      }

      if(options.includeMetaData.getOrElse(true)) {
        json += METADATA -> toJson(task.metaData)
      }

      // Serialize task data
      val taskDataJson = toJson(task.data).as[JsObject]
      // We always want to add the type at the top level regardless if the task data is serialized
      for(taskType <- taskDataJson.value.get(TASKTYPE)) {
        json += TASKTYPE -> taskType
      }
      if(options.includeTaskData.getOrElse(true)) {
        json += DATA -> taskDataJson
      }

      if(options.includeTaskProperties.getOrElse(false)) {
        json += PROPERTIES -> writeTaskProperties(task)
      }
      if(options.includeRelations.getOrElse(false) && userContext.isDefined) {
        implicit val uc = userContext.get // User context is needed to fetch dependent tasks
        json += RELATIONS -> writeTaskRelations(task, dependentTaskFormatter)
      }
      if(options.includeSchemata.getOrElse(false)) {
        json += SCHEMATA -> writeTaskSchemata(task)
      }

      json
    }

    private def writeTaskProperties(task: Task[T])(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsArray(
        for((key, value) <- task.data.properties) yield {
          Json.obj(KEY -> key, VALUE -> value)
        }
      )
    }



    private def writeTaskRelations(task: Task[T],
                                   dependentTaskFormatter: Option[String => JsValue])
                                  (implicit writeContext: WriteContext[JsValue],
                                   userContext: UserContext): JsValue = {
      Json.obj(
        "inputTasks" -> JsArray(task.data.inputTasks.toSeq.map(JsString(_))),
        "outputTasks" -> JsArray(task.data.outputTasks.toSeq.map(JsString(_))),
        "referencedTasks" -> JsArray(task.data.referencedTasks.toSeq.map(JsString(_))),
        "dependentTasksDirect" -> {
          val directTasks = task.findDependentTasks(recursive = false)
          dependentTaskFormatter match {
            case Some(jsonFormatter) =>
              directTasks.map(t => jsonFormatter(t))
            case None =>
              directTasks.map(JsString(_)).toSeq
          }
        },
        "dependentTasksAll" -> {
          val allTasks = task.findDependentTasks(recursive = true)
          dependentTaskFormatter match {
            case Some(jsonFormatter) =>
              allTasks.map(t => jsonFormatter(t))
            case None =>
              allTasks.map(JsString(_)).toSeq
          }
        }
      )
    }

    private def writeTaskSchemata(task: Task[T])(implicit writeContext: WriteContext[JsValue]): JsValue = {
      val inputSchemata = task.data.inputSchemataOpt match {
        case Some(schemata) => JsArray(schemata.map(entitySchema))
        case None => JsNull
      }
      val outputSchema = task.data.outputSchemaOpt.map(entitySchema).getOrElse(JsNull)
      Json.obj(
        "input" -> inputSchemata,
        "output" -> outputSchema
      )
    }

    private def entitySchema(schema: EntitySchema) = {
      // TODO: Why is only the Path written instead of the TypedPath, where is serialization read?
      val paths = for(typedPath <- schema.typedPaths) yield JsString(typedPath.toUntypedPath.normalizedSerialization)
      Json.obj(
        "paths" -> JsArray(paths)
      )
    }
  }

  /**
    * Task serialization options.
    * Should use a format that can be serialized with the Play Json library.
    *
    * @param includeMetaData Include the task meta data.
    * @param includeTaskData Include the task data.
    * @param includeTaskProperties Retrieves a list of properties as key-value pairs to be displayed to the user.
    * @param includeRelations Include relations to other tasks.
    * @param includeSchemata Include the input and output schemata of the task.
    */
  case class TaskFormatOptions(@Schema(
                                 description = "Include the task meta data.",
                                 defaultValue = "true",
                                 implementation = classOf[Boolean]
                               )
                               includeMetaData: Option[Boolean] = None,
                               @Schema(
                                 description = "Include the task data.",
                                 defaultValue = "true",
                                 implementation = classOf[Boolean]
                               )
                               includeTaskData: Option[Boolean] = None,
                               @Schema(
                                 description = "Retrieves a list of properties as key-value pairs to be displayed to the user.",
                                 defaultValue = "false",
                                 implementation = classOf[Boolean]
                               )
                               includeTaskProperties: Option[Boolean] = None,
                               @Schema(
                                 description = "Include relations to other tasks.",
                                 defaultValue = "false",
                                 required = false,
                                 implementation = classOf[Boolean]
                               )
                               includeRelations: Option[Boolean] = None,
                               @Schema(
                                 description = "Include the input and output schemata of the task.",
                                 defaultValue = "false",
                                 implementation = classOf[Boolean]
                               )
                               includeSchemata: Option[Boolean] = None)

  /**
    * Dataset Task
    */
  implicit object DatasetTaskJsonFormat extends JsonFormat[DatasetTask] {
    override def read(value: JsValue)(implicit readContext: ReadContext): DatasetTask = {
      val task = new TaskJsonFormat[GenericDatasetSpec].read(value)
      DatasetTask(task.id, task.data, task.metaData)
    }
    override def write(value: DatasetTask)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      new TaskJsonFormat[GenericDatasetSpec].write(value)
    }
  }

  /**
    * Transform Task
    */
  implicit object TransformTaskJsonFormat extends JsonFormat[TransformTask] {
    override def read(value: JsValue)(implicit readContext: ReadContext): TransformTask = {
      val task = new TaskJsonFormat[TransformSpec].read(value)
      TransformTask(task.id, task.data, task.metaData)
    }
    override def write(value: TransformTask)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      new TaskJsonFormat[TransformSpec].write(value)
    }
  }

  /**
    * Generic Task
    */
  implicit object GenericTaskJsonFormat extends JsonFormat[Task[TaskSpec]] {
    override def read(value: JsValue)(implicit readContext: ReadContext): Task[TaskSpec] = {
      new TaskJsonFormat[TaskSpec].read(value)
    }
    override def write(value: Task[TaskSpec])(implicit writeContext: WriteContext[JsValue]): JsValue = {
      new TaskJsonFormat[TaskSpec].write(value)
    }
  }

  /** Vocabulary */
  implicit object GenericInfoJsonFormat extends JsonFormat[GenericInfo] {
    final val DESCRIPTION: String = "description"
    final val LABEL: String = "label"
    final val ALT_LABELS: String = "altLabels"

    override def read(value: JsValue)(implicit readContext: ReadContext): GenericInfo = {
      GenericInfo(
        uri = stringValue(value, URI),
        label = stringValueOption(value, LABEL),
        description = stringValueOption(value, DESCRIPTION),
        altLabels = optionalValue(value, ALT_LABELS).toSeq.flatMap { array =>
          mustBeJsArray(array) { jsArray =>
            jsArray.value.map(_.as[String])
          }
        }
      )
    }

    override def write(value: GenericInfo)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      val altLabels = JsArray(value.altLabels.map(l => JsString(l)))
      JsObject(
        Seq(
          URI -> UriJsonFormat.write(value.uri)
        ) ++ value.label.map { l =>
          LABEL -> JsString(l)
        } ++ value.description.map { d =>
          DESCRIPTION -> JsString(d)
        } ++ Seq(
          ALT_LABELS -> altLabels
        ).filter(_._2.value.nonEmpty)
      )
    }
  }

  implicit object VocabularyCacheValueJsonFormat extends JsonFormat[VocabularyCacheValue] {
    final val VOCABULARIES = "vocabularies"
    override def read(value: JsValue)(implicit readContext: ReadContext): VocabularyCacheValue = {
      throw new RuntimeException("De-serializing VocabularyCacheValue JSON strings is not supported!")
    }

    override def write(value: VocabularyCacheValue)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        VOCABULARIES -> value.vocabularies.map(VocabularyJsonFormat.write)
      )
    }
  }

  implicit object VocabularyJsonFormat extends JsonFormat[Vocabulary] {
    final val CLASSES = "classes"
    final val PROPERTIES = "properties"

    override def read(value: JsValue)(implicit readContext: ReadContext): Vocabulary = {
      throw new RuntimeException("De-serializing Vocabulary JSON strings is not supported!")
    }

    override def write(value: Vocabulary)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        GENERIC_INFO -> GenericInfoJsonFormat.write(value.info),
        CLASSES -> value.classes.map(VocabularyClassJsonFormat.write).toSeq,
        PROPERTIES -> value.properties.map(VocabularyPropertyJsonFormat.write).toSeq
      )
    }
  }

  /** VocabularyProperty */
  implicit object VocabularyPropertyJsonFormat extends JsonFormat[VocabularyProperty] {
    final val DOMAIN = "domain"
    final val RANGE = "range"
    final val PROPERTY_TYPE = "propertyType"

    override def read(value: JsValue)(implicit readContext: ReadContext): VocabularyProperty = {
      throw new RuntimeException("De-serializing VocabularyProperty JSON strings is not supported!")
    }

    override def write(value: VocabularyProperty)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(
        Seq(
          GENERIC_INFO -> GenericInfoJsonFormat.write(value.info),
          PROPERTY_TYPE -> JsString(value.propertyType.id)
        ) ++ value.domain.map { d =>
          DOMAIN -> UriJsonFormat.write(d.info.uri)
        } ++ value.range.map { r =>
          RANGE -> UriJsonFormat.write(r.info.uri)
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

  /**
    * Reads meta data.
    *
    * @param json The json to read the meta data from.
    * @param identifier If no label is provided in the json, use this identifier to generate a label.
    */
  private def metaData(json: JsValue)(implicit readContext: ReadContext): MetaData = {
    optionalValue(json, METADATA) match {
      case Some(metaDataJson) =>
        fromJson[MetaData](metaDataJson)
      case None =>
        MetaData.empty
    }
  }

  def toJson[T](value: T)(implicit format: JsonFormat[T], writeContext: WriteContext[JsValue]): JsValue = {
    format.write(value)
  }

  def toJsonOpt[T](value: Option[T])(implicit format: JsonFormat[T], writeContext: WriteContext[JsValue]): JsValue = {
    value match {
      case Some(v) =>
        format.write(v)
      case None =>
        JsNull
    }
  }

  def toJsonEmptyContext[T](value: T)(implicit format: JsonFormat[T]): JsValue = {
    implicit val emptyWriteContext: WriteContext[JsValue] = WriteContext.empty[JsValue]
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

  implicit object CachedEntitySchemataJsonFormat extends WriteOnlyJsonFormat[CachedEntitySchemata] {

    override def write(value: CachedEntitySchemata)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        "configured" -> EntitySchemaJsonFormat.write(value.configuredSchema),
        "untyped" -> value.untypedSchema.map(EntitySchemaJsonFormat.write)
      )
    }
  }
}
