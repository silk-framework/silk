package org.silkframework.serialization.json

import java.time.Instant

import org.silkframework.config._
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.{Dataset, DatasetSpec, DatasetTask}
import org.silkframework.entity._
import org.silkframework.rule.evaluation.ReferenceLinks
import org.silkframework.rule.input.{Input, PathInput, TransformInput, Transformer}
import org.silkframework.rule.similarity._
import org.silkframework.rule.vocab.{GenericInfo, VocabularyClass, VocabularyProperty}
import org.silkframework.rule.{MappingTarget, TransformRule, _}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.serialization.{ReadContext, Serialization, WriteContext}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.serialization.json.InputJsonSerializer._
import org.silkframework.serialization.json.JsonHelpers._
import org.silkframework.serialization.json.JsonSerializers._
import org.silkframework.util.{DPair, Identifier, Uri}
import play.api.libs.json._

/**
  * Serializers for JSON.
  */
object JsonSerializers {

  final val ID = "id"
  final val TYPE = "type"
  final val TASKTYPE = "taskType"
  final val PARAMETERS = "parameters"
  final val URI = "uri"
  final val METADATA = "metadata"
  final val OPERATOR = "operator"

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

  implicit object MetaDataJsonFormat extends JsonFormat[MetaData] {

    final val LABEL = "label"
    final val DESCRIPTION = "description"
    final val MODIFIED = "modified"

    override def read(value: JsValue)(implicit readContext: ReadContext): MetaData = {
      MetaData(
        label = stringValueOption(value, LABEL).getOrElse(""),
        description = stringValueOption(value, DESCRIPTION).getOrElse(""),
        modified = stringValueOption(value, MODIFIED).map(Instant.parse)
      )
    }

    override def write(value: MetaData)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      var json =
        Json.obj(
          LABEL -> JsString(value.label),
          DESCRIPTION -> JsString(value.description)
        )
      for(modified <- value.modified) {
        json += MODIFIED -> JsString(modified.toString)
      }
      json
    }
  }

  class PairJsonFormat[T](implicit dataFormat: JsonFormat[T]) extends JsonFormat[DPair[T]] {

    private val SOURCE = "source"
    private val TARGET = "target"

    override def read(value: JsValue)(implicit readContext: ReadContext): DPair[T] = {
      DPair[T](
        source = dataFormat.read(mustBeDefined(value, SOURCE)),
        target = dataFormat.read(mustBeDefined(value, TARGET))
      )
    }

    override def write(value: DPair[T])(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        SOURCE -> dataFormat.write(value.source),
        TARGET -> dataFormat.write(value.target)
      )
    }
  }

  implicit object JsonDatasetSpecFormat extends JsonFormat[GenericDatasetSpec] {

    private val URI_PROPERTY = "uriProperty"

    override def typeNames: Set[String] = Set("Dataset")

    override def read(value: JsValue)(implicit readContext: ReadContext): GenericDatasetSpec = {
      implicit val prefixes = readContext.prefixes
      implicit val resource = readContext.resources
      new DatasetSpec(
        plugin =
          Dataset(
            id = (value \ TYPE).as[JsString].value,
            params = (value \ PARAMETERS).as[JsObject].value.mapValues(_.as[JsString].value).asInstanceOf[Map[String, String]]
          ),
        uriProperty = stringValueOption(value, URI_PROPERTY).filter(_.trim.nonEmpty).map(v => Uri(v.trim))
      )
    }

    override def write(value: GenericDatasetSpec)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      var json =
        Json.obj(
          TASKTYPE -> JsString("Dataset"),
          TYPE -> JsString(value.plugin.pluginSpec.id.toString),
          PARAMETERS -> Json.toJson(value.plugin.parameters)
        )
      for(property <- value.uriProperty) {
        json += (URI_PROPERTY -> JsString(property.uri))
      }
      json
    }
  }

  implicit object CustomTaskJsonFormat extends JsonFormat[CustomTask] {

    override def typeNames: Set[String] = Set("CustomTask")

    override def read(value: JsValue)(implicit readContext: ReadContext): CustomTask = {
      implicit val prefixes = readContext.prefixes
      implicit val resource = readContext.resources
      CustomTask(
        id = (value \ TYPE).as[JsString].value,
        params = (value \ PARAMETERS).as[JsObject].value.mapValues(_.as[JsString].value).asInstanceOf[Map[String, String]]
      )
    }

    override def write(value: CustomTask)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        TASKTYPE -> JsString("CustomTask"),
        TYPE -> JsString(value.pluginSpec.id.toString),
        PARAMETERS -> Json.toJson(value.parameters)
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
          FUNCTION -> JsString(value.transformer.pluginSpec.id),
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
          "sourcePaths" -> JsArray(rule.sourcePaths.map(_.serialize()(writeContext.prefixes)).map(JsString)),
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
    final val RULES_PROPERTY: String = "root"
    final val OUTPUTS: String = "outputs"
    final val TARGET_VOCABULARIES: String = "targetVocabularies"

    override def typeNames: Set[String] = Set("Transform")

    /**
      * Deserializes a value.
      */
    override def read(value: JsValue)(implicit readContext: ReadContext): TransformSpec = {
      TransformSpec(
        selection = fromJson[DatasetSelection](mustBeDefined(value, SELECTION)),
        mappingRule = optionalValue(value, RULES_PROPERTY).map(fromJson[RootMappingRule]).getOrElse(RootMappingRule.empty),
        outputs = mustBeJsArray(mustBeDefined(value, OUTPUTS))(_.value.map(v => Identifier(v.as[JsString].value))),
        targetVocabularies = mustBeJsArray(mustBeDefined(value, TARGET_VOCABULARIES))(_.value.map(_.as[JsString].value))
      )
    }

    /**
      * Serializes a value.
      */
    override def write(value: TransformSpec)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        TASKTYPE -> "Transform",
        SELECTION -> toJson(value.selection),
        RULES_PROPERTY -> toJson(value.mappingRule),
        OUTPUTS -> JsArray(value.outputs.map(id => JsString(id.toString))),
        TARGET_VOCABULARIES -> JsArray(value.targetVocabularies.toSeq.map(JsString))
      )
    }
  }

  implicit object LinkJsonFormat extends JsonFormat[Link] {
    final val SOURCE = "source"
    final val TARGET = "target"
    final val CONFIDENCE = "confidence"

    override def read(value: JsValue)(implicit readContext: ReadContext): Link = {
      new Link(
        source = stringValue(value, SOURCE),
        target = stringValue(value, TARGET),
        confidence = numberValueOption(value, CONFIDENCE).map(_.doubleValue)
      )
    }

    override def write(value: Link)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        SOURCE -> value.source,
        TARGET -> value.target,
        CONFIDENCE -> value.confidence.map(JsNumber(_))
      )
    }
  }

  implicit object ReferenceLinksJsonFormat extends JsonFormat[ReferenceLinks] {
    final val POSITIVE = "positive"
    final val NEGATIVE = "negative"
    final val UNLABELED = "unlabeled"

    override def read(value: JsValue)(implicit readContext: ReadContext): ReferenceLinks = {
      ReferenceLinks(
        positive = mustBeJsArray(mustBeDefined(value, POSITIVE))(_.value.map(fromJson[Link])).toSet,
        negative = mustBeJsArray(mustBeDefined(value, POSITIVE))(_.value.map(fromJson[Link])).toSet,
        unlabeled = mustBeJsArray(mustBeDefined(value, POSITIVE))(_.value.map(fromJson[Link])).toSet
      )
    }

    override def write(value: ReferenceLinks)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        POSITIVE -> JsArray(value.positive.toSeq.map(toJson(_))),
        NEGATIVE -> JsArray(value.negative.toSeq.map(toJson(_))),
        UNLABELED -> JsArray(value.unlabeled.toSeq.map(toJson(_)))
      )
    }
  }

  implicit object ComparisonJsonFormat extends JsonFormat[Comparison] {
    final val REQUIRED = "required"
    final val WEIGHT = "weight"
    final val THRESHOLD = "threshold"
    final val INDEXING = "indexing"
    final val METRIC = "metric"
    final val SOURCEINPUT = "sourceInput"
    final val TARGETINPUT = "targetInput"
    final val COMPARISON_TYPE = "Comparison"

    override def typeNames: Set[String] = Set(COMPARISON_TYPE)

    override def read(value: JsValue)(implicit readContext: ReadContext): Comparison = {
      implicit val prefixes = readContext.prefixes
      implicit val resourceManager = readContext.resources
      val metric = DistanceMeasure(stringValue(value, METRIC), readParameters(value))

      Comparison(
        id = identifier(value, "comparison"),
        required = booleanValue(value, REQUIRED),
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
        REQUIRED -> value.required,
        WEIGHT -> value.weight,
        THRESHOLD -> value.threshold,
        INDEXING -> value.indexing,
        METRIC -> value.metric.pluginSpec.id.toString,
        PARAMETERS -> Json.toJson(value.metric.parameters),
        SOURCEINPUT -> toJson(value.inputs.source),
        TARGETINPUT -> toJson(value.inputs.target)
      )
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
      implicit val prefixes = readContext.prefixes
      implicit val resourceManager = readContext.resources
      val aggregator = Aggregator(stringValue(value, AGGREGATOR), readParameters(value))
      val inputs = mustBeJsArray(mustBeDefined(value, OPERATORS)) { jsArray =>
        jsArray.value.map(fromJson[SimilarityOperator](_)(SimilarityOperatorJsonFormat, readContext))
      }

      Aggregation(
        id = identifier(value, "aggregation"),
        required = booleanValue(value, REQUIRED),
        weight = numberValue(value, WEIGHT).intValue,
        aggregator = aggregator,
        operators = inputs
      )
    }

    override def write(value: Aggregation)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        ID -> value.id.toString,
        TYPE -> AGGREGATION_TYPE,
        REQUIRED -> value.required,
        WEIGHT -> value.weight,
        AGGREGATOR -> value.aggregator.pluginSpec.id.toString,
        PARAMETERS -> Json.toJson(value.aggregator.parameters),
        OPERATORS -> value.operators.map(toJson(_))
      )
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
        threshold = numberValueOption(value, THRESHOLD).map(_.doubleValue),
        unambiguous = booleanValueOption(value, UNAMBIGUOUS)
      )
    }

    override def write(value: LinkFilter)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        LIMIT -> value.limit.map(JsNumber(_)),
        THRESHOLD -> value.threshold.map(JsNumber(_)),
        UNAMBIGUOUS -> value.unambiguous.map(JsBoolean)
      )
    }
  }

  implicit object LinkageRuleJsonFormat extends JsonFormat[LinkageRule] {
    final val OPERATOR = "operator"
    final val FILTER = "filter"
    final val LINKTYPE = "linkType"

    override def read(value: JsValue)(implicit readContext: ReadContext): LinkageRule = {
      LinkageRule(
        operator = optionalValue(value, OPERATOR).map(fromJson[SimilarityOperator]),
        filter = fromJson[LinkFilter](mustBeDefined(value, FILTER)),
        linkType = fromJson[Uri](mustBeDefined(value, LINKTYPE))
      )
    }

    override def write(value: LinkageRule)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        OPERATOR -> value.operator.map(toJson(_)),
        FILTER -> toJson(value.filter),
        LINKTYPE -> toJson(value.linkType)
      )
    }
  }

  implicit object LinkSpecJsonFormat extends JsonFormat[LinkSpec] {
    final val SOURCE = "source"
    final val TARGET = "target"
    final val RULE = "rule"
    final val OUTPUTS = "outputs"
    final val REFERENCE_LINKS = "referenceLinks"

    override def typeNames: Set[String] = Set("Linking")

    override def read(value: JsValue)(implicit readContext: ReadContext): LinkSpec = {
      LinkSpec(
        dataSelections =
          DPair(
            fromJson[DatasetSelection](mustBeDefined(value, SOURCE)),
            fromJson[DatasetSelection](mustBeDefined(value, TARGET))
          ),
        rule = optionalValue(value, RULE).map(fromJson[LinkageRule]).getOrElse(LinkageRule()),
        outputs = mustBeJsArray(mustBeDefined(value, OUTPUTS))(_.value.map(v => Identifier(v.as[JsString].value))),
        referenceLinks = optionalValue(value, REFERENCE_LINKS).map(fromJson[ReferenceLinks]).getOrElse(ReferenceLinks.empty)
      )
    }

    override def write(value: LinkSpec)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        TASKTYPE -> "Linking",
        SOURCE -> toJson(value.dataSelections.source),
        TARGET -> toJson(value.dataSelections.target),
        RULE -> toJson(value.rule),
        OUTPUTS -> JsArray(value.outputs.map(id => JsString(id.toString))),
        REFERENCE_LINKS -> toJson(value.referenceLinks)
      )
    }
  }

  /**
    * Task
    */
  implicit object TaskSpecJsonFormat extends JsonFormat[TaskSpec] {

    // Holds all JSON formats for sub classes of TaskSpec.
    private lazy val taskSpecFormats: Seq[JsonFormat[TaskSpec]] = {
      Serialization.availableFormats.filter(f => f.isInstanceOf[JsonFormat[_]] && classOf[TaskSpec].isAssignableFrom(f.valueType) && f != this)
        .map(_.asInstanceOf[JsonFormat[TaskSpec]])
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
      taskSpecFormats.find(_.valueType.isAssignableFrom(value.getClass)) match {
        case Some(format) =>
          format.write(value)
        case None =>
          throw new ValidationException(s"No serialization format found for class ${value.getClass.getName}")
      }
    }
  }

  /**
    * Task
    */
  class TaskJsonFormat[T <: TaskSpec](options: TaskFormatOptions = TaskFormatOptions(),
                                      userContext: Option[UserContext] = None)(implicit dataFormat: JsonFormat[T]) extends JsonFormat[Task[T]] {

    final val PROJECT = "project"
    final val DATA = "data"
    final val PROPERTIES = "properties"
    final val RELATIONS = "relations"
    final val SCHEMATA = "schemata"
    final val KEY = "key"
    final val VALUE = "value"


    /**
      * Deserializes a value.
      */
    override def read(value: JsValue)(implicit readContext: ReadContext): Task[T] = {
      // In older serializations the task data has been directly attached to this JSON object
      val dataJson = optionalValue(value, DATA).getOrElse(value)
      PlainTask(
        id = stringValue(value, ID),
        data = fromJson[T](dataJson),
        metaData = metaData(value)
      )
    }

    /**
      * Serializes a value.
      */
    override def write(task:  Task[T])(implicit writeContext: WriteContext[JsValue]): JsValue = {
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
        json += RELATIONS -> writeTaskRelations(task)
      }
      if(options.includeSchemata.getOrElse(false)) {
        json += SCHEMATA -> writeTaskSchemata(task)
      }

      json
    }

    private def writeTaskProperties(task: Task[T])(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsArray(
        for((key, value) <- task.data.properties(writeContext.prefixes)) yield {
          Json.obj(KEY -> key, VALUE -> value)
        }
      )
    }

    private def writeTaskRelations(task: Task[T])
                                  (implicit writeContext: WriteContext[JsValue],
                                   userContext: UserContext): JsValue = {
      Json.obj(
        "inputTasks" -> JsArray(task.data.inputTasks.toSeq.map(JsString(_))),
        "outputTasks" -> JsArray(task.data.outputTasks.toSeq.map(JsString(_))),
        "referencedTasks" -> JsArray(task.data.referencedTasks.toSeq.map(JsString(_))),
        "dependentTasksDirect" -> JsArray(task.findDependentTasks(recursive = false).map(JsString(_)).toSeq),
        "dependentTasksAll" -> JsArray(task.findDependentTasks(recursive = true).map(JsString(_)).toSeq)
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
      val paths = for(typedPath <- schema.typedPaths) yield JsString(typedPath.normalizedSerialization)
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
  case class TaskFormatOptions(includeMetaData: Option[Boolean] = None,
                               includeTaskData: Option[Boolean] = None,
                               includeTaskProperties: Option[Boolean] = None,
                               includeRelations: Option[Boolean] = None,
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

  def toJson[T](value: T)(implicit format: JsonFormat[T], writeContext: WriteContext[JsValue] = WriteContext[JsValue](projectId = None)): JsValue = {
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
