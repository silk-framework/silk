package org.silkframework.rule

import org.silkframework.config.MetaData.MetaDataXmlFormat
import org.silkframework.config.{HasMetaData, MetaData, Prefixes}
import org.silkframework.dataset.TypedProperty
import org.silkframework.entity._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.rule.MappingRules.MappingRulesFormat
import org.silkframework.rule.MappingTarget.MappingTargetFormat
import org.silkframework.rule.TransformRule.RDF_TYPE
import org.silkframework.rule.input.{Input, PathInput, TransformInput, Value}
import org.silkframework.rule.plugins.transformer.combine.ConcatTransformer
import org.silkframework.rule.plugins.transformer.normalize.{UriFixTransformer, UrlEncodeTransformer}
import org.silkframework.rule.plugins.transformer.value.{ConstantTransformer, ConstantUriTransformer, EmptyValueTransformer}
import org.silkframework.rule.util.UriPatternParser
import org.silkframework.rule.util.UriPatternParser.{ConstantPart, PathPart}
import org.silkframework.runtime.plugin.PluginObjectParameterNoSchema
import org.silkframework.runtime.serialization.XmlSerialization.{fromXml, toXml}
import org.silkframework.runtime.serialization._
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util._
import org.silkframework.workspace.annotation.UiAnnotations

import java.net.URI
import scala.language.implicitConversions
import scala.util.Try
import scala.xml.{Node, Null}

/**
  * A transformation rule.
  * A transformations rule generates property values from based on an arbitrary operator tree consisting of property paths and transformations.
  * Sub classes are defined for special cases, such as direct mappings.
  */
sealed trait TransformRule extends Operator with HasMetaData {

  /** The name of this rule. */
  def id: Identifier

  /** The meta data for this rule. */
  def metaData: MetaData

  /** The same rule with different meta data */
  def withMetaData(metaData: MetaData): TransformRule

  /** The input operator tree. */
  def operator: Input

  /** The target property URI. */
  def target: Option[MappingTarget]

  /** String representation of rule type */
  def typeString: String

  def rules: MappingRules = MappingRules.empty

  assert(rules.allRules.forall(!_.isInstanceOf[RootMappingRule]), "No root mapping rule allowed as child of another rule!")

  /**
    * Generates the transformed values.
    *
    * @param entity The source entity.
    * @return The transformed values.
    * @throws ValidationException If a value failed to be transformed or a generated value doesn't match the target type.
    */
  def apply(entity: Entity): Value = {
    val values = operator(entity)
    // Validate values
    target.foreach(_.validate(values.values))
    values
  }

  /**
    * Generates a label for this rule.
    * Will return the user-defined label, if any is defined.
    * In case no label is defined, it falls back to the target property.
    * If no target property is defined, the id will be returned.
    */
  override def label(maxLength: Int = MetaData.DEFAULT_LABEL_MAX_LENGTH)(implicit prefixes: Prefixes): String = {
    val defaultLabel = target.map(_.propertyUri.serialize).filter(_.nonEmpty).getOrElse(id.toString)
    metaData.formattedLabel(defaultLabel, maxLength)
  }

  /**
    * Collects all input paths in this rule.
    */
  def sourcePaths: Seq[TypedPath] = {
    def collectPaths(param: Input): Seq[TypedPath] = param match {
      case p: PathInput if p.path.operators.isEmpty => Seq()
      case PathInput(_, path: TypedPath) => Seq(path)
      case PathInput(_, path: UntypedPath) => Seq(TypedPath(path, ValueType.UNTYPED, isAttribute = false))
      case p: TransformInput => p.inputs.flatMap(collectPaths)
    }

    collectPaths(operator).distinct
  }

  /** Throws ValidationException if this transform rule is not valid. */
  protected def validate(): Unit = {
    validateTargetUri()
    // Validate that the operator tree uses unique identifiers
    operator.validateIds()
    // Validate all child transform rules
    rules.foreach(_.validate())
  }

  /**
    * Validates that the target URI is a valid URI.
    */
  private def validateTargetUri(): Unit = {
    target foreach { mt =>
      val failure = Try {
        new URI(mt.propertyUri.uri)
      }.isFailure
      if(failure) {
        throw new ValidationException("Not a valid mapping target property: '" + mt.propertyUri.toString + "'")
      }
    }
  }

  /**
    * The children operators.
    */
  def children: Seq[Operator] = rules.allRules

  /**
    * Generates the same operator with new children.
    */
  override def withChildren(newChildren: Seq[Operator]): TransformRule = {
    if (newChildren.isEmpty) {
      this
    } else {
      throw new IllegalArgumentException(s"$this cannot have any children")
    }
  }

  def representsDefaultUriRule: Boolean = {
    false
  }
}

/**
  * Base trait for all rules that can have child rules.
  */
sealed trait ContainerTransformRule extends TransformRule {
  override def label(maxLength: Int)(implicit prefixes: Prefixes): String = {
    val typeLabel = rules.typeRules.map(typeUri => prefixes.shorten(typeUri.typeUri.uri)).mkString(", ")
    if(typeLabel.nonEmpty) {
      typeLabel
    } else {
      super.label(maxLength)
    }
  }
}

/**
  * Base trait for all rule that generate a value and do not have any child rules.
  */
sealed trait ValueTransformRule extends TransformRule {
  /** A complex mapping representation of the value transform rule. */
  def asComplexMapping: ComplexMapping = {
    ComplexMapping(
      id = id,
      operator = operator,
      target = target,
      metaData = metaData,
      layout = layout,
      uiAnnotations = uiAnnotations
    )
  }

  def layout: RuleLayout = RuleLayout()

  def uiAnnotations: UiAnnotations = UiAnnotations()
}

/**
  * The root mapping rule.
  *
  * @param id        The identifier of this mapping.
  * @param rules     The rules of this mapping.
  * @param metaData  The metadata.
  */
case class RootMappingRule(override val rules: MappingRules,
                           id: Identifier = RootMappingRule.defaultId,
                           mappingTarget: MappingTarget = RootMappingRule.defaultMappingTarget,
                           metaData: MetaData = MetaData.empty) extends ContainerTransformRule with PluginObjectParameterNoSchema {

  override def label(maxLength: Int)(implicit prefixes: Prefixes): String = {
    if(metaData.label.isEmpty && rules.typeRules.isEmpty) {
      "Mapping"
    } else {
      super.label(maxLength)
    }
  }

  override def withMetaData(metaData: MetaData): TransformRule = this.copy(metaData = metaData)

  validate()
  validateIds()

  /**
    * The children operators.
    */
  override def children: Seq[Operator] = rules.allRules

  /**
    * Generates the same operator with new children.
    */
  override def withChildren(newChildren: Seq[Operator]): TransformRule = {
    val newRules = newChildren.map(_.asInstanceOf[TransformRule])
    this.copy(rules = MappingRules.fromSeq(newRules))
  }

  /** The input operator tree. */
  override def operator: Input = TransformInput("root", EmptyValueTransformer())

  /** The the mapping target */
  override def target: Option[MappingTarget] = Some(mappingTarget)

  /** String representation of rule type */
  override def typeString: String = "Root"
}

object RootMappingRule {

  def defaultId: String = "root"

  def defaultMappingTarget: MappingTarget = MappingTarget(propertyUri = "", valueType = ValueType.URI)

  final val empty: RootMappingRule = RootMappingRule(MappingRules.empty)

  /**
    * XML serialization format.
    */
  implicit object RootMappingRuleFormat extends XmlFormat[RootMappingRule] {
    /**
      * Deserializes a value.
      */
    override def read(value: Node)(implicit readContext: ReadContext): RootMappingRule = {
      RootMappingRule(
        id = (value \ "@id").text,
        rules = MappingRulesFormat.read((value \ "MappingRules").head),
        mappingTarget = (value \ "MappingTarget").headOption.map(fromXml[MappingTarget]).getOrElse(RootMappingRule.defaultMappingTarget),
        metaData = (value \ "MetaData").headOption.map(MetaDataXmlFormat.read).getOrElse(MetaData.empty)
      )
    }

    /**
      * Serializes a value.
      */
    override def write(value: RootMappingRule)(implicit writeContext: WriteContext[Node]): Node = {
      <RootMappingRule id={value.id}>
        { MappingTargetFormat.write(value.mappingTarget) }
        { MappingRulesFormat.write(value.rules) }
        { MetaDataXmlFormat.write(value.metaData) }
      </RootMappingRule>
    }
  }

  implicit def toTypedProperty(mt: MappingTarget): TypedProperty = TypedProperty(mt.propertyUri.uri, mt.valueType, mt.isBackwardProperty, mt.isAttribute)

}

/**
  * A direct mapping between two properties.
  *
  * @param id             The name of this mapping. For direct mappings usually just the property that is mapped.
  * @param sourcePath     The source path
  * @param mappingTarget  The target property
  * @param metaData       Meta data
  * @param inputId        Identifier of the generated input path operator. If not defined, the mapping `id` will be used.
  */
case class DirectMapping(id: Identifier = "sourcePath",
                         sourcePath: UntypedPath = UntypedPath(Nil),
                         mappingTarget: MappingTarget = MappingTarget("http://www.w3.org/2000/01/rdf-schema#label"),
                         metaData: MetaData = MetaData.empty,
                         inputId: Option[Identifier] = None) extends ValueTransformRule {

  override val operator = PathInput(inputId.getOrElse(id), sourcePath.asUntypedPath)

  override val target = Some(mappingTarget)

  override val typeString = "Direct"

  override def withMetaData(metaData: MetaData): TransformRule = this.copy(metaData = metaData)
}

/**
  * Assigns a new URI to each mapped entity.
  */
trait UriMapping extends ValueTransformRule

/**
  * Assigns a new URI to each mapped entity based on a pattern.
  *
  * @param id      The name of this mapping
  * @param pattern A template pattern for generating the URIs based on the entity properties
  */
case class PatternUriMapping(id: Identifier = "URI",
                             pattern: String = "http://example.org/{ID}",
                             metaData: MetaData = MetaData.empty,
                             prefixes: Prefixes = Prefixes.empty) extends UriMapping {

  override lazy val operator: Input = UriPattern.parse(pattern.trim())(prefixes)

  override val target: Option[MappingTarget] = None

  override val typeString = "URI"

  override def withMetaData(metaData: MetaData): TransformRule = this.copy(metaData = metaData)
}


/**
  * Generates a URI based on an arbitrary rule tree.
  *
  * @param id      The name of this mapping
  * @param operator The operator tree that generates the URI.
  */
case class ComplexUriMapping(id: Identifier = "complexURI",
                             operator: Input,
                             metaData: MetaData = MetaData.empty,
                             override val layout: RuleLayout = RuleLayout(),
                             override val uiAnnotations: UiAnnotations = UiAnnotations()
                            ) extends UriMapping with ValueTransformRule {

  override val target: Option[MappingTarget] = None

  override val typeString = "ComplexURI"

  override def withMetaData(metaData: MetaData): TransformRule = this.copy(metaData = metaData)
}

/**
  * A type mapping, which assigns a type to each entity.
  *
  * @param id      The name of this mapping
  * @param typeUri The type URI.
  */
case class TypeMapping(id: Identifier = "type",
                       typeUri: Uri = "http://www.w3.org/2002/07/owl#Thing",
                       metaData: MetaData = MetaData.empty) extends ValueTransformRule {

  override val operator = TransformInput("generateType", ConstantUriTransformer(typeUri))

  override val target = Some(MappingTarget(RDF_TYPE, ValueType.URI))

  override val typeString = "Type"

  override def withMetaData(metaData: MetaData): TransformRule = this.copy(metaData = metaData)

}

/**
  * A complex mapping, which generates property values from based on an arbitrary operator tree consisting of property paths and transformations.
  *
  * @param id       The name of this mapping
  * @param operator The input operator tree
  * @param target   The target property URI
  */
case class ComplexMapping(id: Identifier = "mapping",
                          operator: Input,
                          target: Option[MappingTarget] = None,
                          metaData: MetaData = MetaData.empty,
                          override val layout: RuleLayout = RuleLayout(),
                          override val uiAnnotations: UiAnnotations = UiAnnotations()
                         ) extends ValueTransformRule {

  override val typeString = "Complex"

  override def withMetaData(metaData: MetaData): TransformRule = this.copy(metaData = metaData)
}

/**
  * A hierarchical mapping.
  *
  * Generates child entities that are connected to the parent entity using the targetProperty.
  * The properties of the child entities are mapped by the child mappings.
  *
  * @param id The name of this mapping.
  * @param sourcePath The relative input path to locate the child entities in the source.
  * @param target The property that is used to attach the child entities.
  * @param rules The child rules.
  */
case class ObjectMapping(id: Identifier = "mapping",
                         sourcePath: UntypedPath = UntypedPath(Nil),
                         target: Option[MappingTarget] = Some(MappingTarget("http://www.w3.org/2002/07/owl#sameAs", ValueType.URI)),
                         override val rules: MappingRules,
                         metaData: MetaData = MetaData.empty,
                         prefixes: Prefixes = Prefixes.empty) extends ContainerTransformRule {
  override val typeString = "Object"

  def uriRule(pathPrefix: UntypedPath = UntypedPath.empty): Option[UriMapping] = {
    target match {
      case Some(prop) =>
        rules.uriRule match {
          case Some (rule) => {
            val rewrittenInput = Input.rewriteSourcePaths(rule.operator, path => {
              UntypedPath(sourcePath.operators ++ path.operators)
            })
            Some(ComplexUriMapping(rule.id, rewrittenInput, rule.metaData, RuleLayout(),UiAnnotations()))
          }
          case None if sourcePath.isEmpty =>
            Some(PatternUriMapping(pattern = s"{}/$id", prefixes = prefixes))
          case None =>
            Some(PatternUriMapping(pattern = s"{${pathPrefix.normalizedSerialization}}/$id", prefixes = prefixes))
        }
      case None =>
        None
    }
  }

  override lazy val operator: Input = {
    uriRule(sourcePath) match {
      case Some(rule) =>
        rule.operator
      case None =>
        PathInput(path = UntypedPath.empty)
    }
  }

  /**
    * Generates the same operator with new children.
    */
  override def withChildren(newChildren: Seq[Operator]): TransformRule = {
    val newRules = newChildren.map(_.asInstanceOf[TransformRule])
    this.copy(rules = MappingRules.fromSeq(newRules))
  }

  def fillEmptyUriRule: ObjectMapping = {
    copy(rules = rules.copy(uriRule = rules.uriRule.orElse(uriRule())))
  }

  override def representsDefaultUriRule: Boolean = rules.uriRule.isEmpty

  override def withMetaData(metaData: MetaData): TransformRule = this.copy(metaData = metaData)
}

/**
  * Creates new transform rules.
  */
object TransformRule {
  val RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"

  /**
    * XML serialization format.
    */
  implicit object TransformRuleFormat extends XmlFormat[TransformRule] {

    import XmlSerialization._

    def read(node: Node)(implicit readContext: ReadContext): TransformRule = {
      ValidatingXMLReader.validate(node, "org/silkframework/LinkSpecificationLanguage.xsd")

      node.label match {
        case "ObjectMapping" => readObjectMapping(node)
        case "TransformRule" => readTransformRule(node)
      }
    }

    private def readObjectMapping(node: Node)(implicit readContext: ReadContext): ObjectMapping = {
      ObjectMapping(
        id = (node \ "@name").text,
        sourcePath = UntypedPath.parse((node \ "@relativePath").text),
        target = (node \ "MappingTarget").headOption.map(fromXml[MappingTarget]),
        rules = MappingRules.fromSeq((node \ "MappingRules" \ "_").map(read)),
        metaData = (node \ "MetaData").headOption.map(MetaDataXmlFormat.read).getOrElse(MetaData.empty),
        readContext.prefixes)
    }

    private def readTransformRule(node: Node)(implicit readContext: ReadContext): TransformRule = {
      // First test new target serialization, else old one
      val target = (node \ "MappingTarget").headOption.
        map(tp => Some(fromXml[MappingTarget](tp))).
        getOrElse {
          val targetProperty = (node \ "@targetProperty").text
          if (targetProperty.isEmpty) {
            None
          } else {
            Some(MappingTarget(Uri.parse(targetProperty, readContext.prefixes)))
          }
        }

      val metaData = (node \ "MetaData").headOption.map(MetaDataXmlFormat.read).getOrElse(MetaData.empty)

      val complex =
        ComplexMapping(
          id = (node \ "@name").text,
          operator = fromXml[Input]((node \ "Input" ++ node \ "TransformInput").head),
          target = target,
          metaData = metaData,
          layout = (node \ "RuleLayout").headOption.map(rl => XmlSerialization.fromXml[RuleLayout](rl)).getOrElse(RuleLayout()),
          uiAnnotations = (node \ "UiAnnotations").headOption.map(rl => XmlSerialization.fromXml[UiAnnotations](rl)).getOrElse(UiAnnotations())
        )
      simplify(complex)(readContext.prefixes)
    }

    def write(value: TransformRule)(implicit writeContext: WriteContext[Node]): Node = {
      value match {
        case ObjectMapping(name, relativePath, target, childRules, metaData, _) =>
          <ObjectMapping name={name} relativePath={relativePath.normalizedSerialization} >
            {MetaDataXmlFormat.write(metaData)}
            {MappingRulesFormat.write(childRules)}
            { target.map(toXml[MappingTarget]).toSeq }
          </ObjectMapping>
        case _ =>
          // At the moment, all other types are serialized generically
          <TransformRule name={value.id}>
            {MetaDataXmlFormat.write(value.metaData)}
            {toXml(value.operator)}{value.target.map(toXml[MappingTarget]).getOrElse(Null)}
            {layout(value)}
            {uiAnnotation(value)}
          </TransformRule>
      }
    }
  }

  private def layout(transformRule: TransformRule) = {
    transformRule match {
      case withLayout: ValueTransformRule =>
        toXml(withLayout.layout)
      case _ =>
        Null
    }
  }

  private def uiAnnotation(transformRule: TransformRule) = {
    transformRule match {
      case withLayout: ValueTransformRule =>
        toXml(withLayout.uiAnnotations)
      case _ =>
        Null
    }
  }


  /**
    * Tries to express a complex mapping as a basic mapping, such as a direct mapping.
    */
  def simplify(complexMapping: ComplexMapping)(implicit prefixes: Prefixes): TransformRule = complexMapping match {
    // Direct Mapping
    case ComplexMapping(id, PathInput(pathId, path), Some(target), metaData, _, uiAnnotations) if uiAnnotations.stickyNotes.isEmpty =>
      DirectMapping(id, path.asUntypedPath, target, metaData, Some(pathId))
    // Rule with annotations or layout info is always treated as complex (URI) mapping rule
    case ComplexMapping(id, operator, targetOpt, metaData, layout, uiAnnotations) if layout.nodePositions.nonEmpty || uiAnnotations.stickyNotes.nonEmpty =>
      if(targetOpt.isEmpty) {
        ComplexUriMapping(id, operator, metaData, layout, uiAnnotations)
      } else {
        complexMapping
      }
    // Pattern URI Mapping
    case ComplexMapping(id, UriPattern(pattern), None, metaData, _, _) =>
      PatternUriMapping(id, pattern, metaData, prefixes = prefixes)
    // Complex URI mapping
    case ComplexMapping(id, operator, None, metaData, layout, uiAnnotations) =>
      ComplexUriMapping(id, operator, metaData, layout, uiAnnotations)
    // Object Mapping (old style, to be removed)
    case ComplexMapping(id, TransformInput(_, ConcatTransformer("", false), inputs), Some(target), metaData, _, _) if UriPattern.isPattern(inputs) && target.valueType == ValueType.URI =>
      ObjectMapping(id, UntypedPath.empty, Some(target), MappingRules(uriRule = Some(PatternUriMapping(id + "uri", UriPattern.build(inputs)))), metaData, prefixes = prefixes)
    // Type Mapping
    case ComplexMapping(id, TransformInput(_, ConstantTransformer(typeUri), Nil), Some(MappingTarget(Uri(RDF_TYPE), _, false, _)), metaData, _, _) =>
      TypeMapping(id, typeUri, metaData)
    // Type Mapping (old style, to be removed)
    case ComplexMapping(id, TransformInput(_, ConstantUriTransformer(typeUri), Nil), Some(MappingTarget(Uri(RDF_TYPE), _, false, _)), metaData, _, _) =>
      TypeMapping(id, typeUri, metaData)
    // Complex Mapping
    case _ => complexMapping
  }


}

/**
  * Handles URI patterns of the form http://example.org/{ID}.
  */
private object UriPattern {

  /**
    * Parses a URI pattern into an input operator tree.
    */
  def parse(pattern: String)(implicit prefixes: Prefixes): Input = {
    val segments = UriPatternParser.parseIntoSegments(pattern, allowIncompletePattern = false).segments
    val inputs = {
      if(segments == IndexedSeq(PathPart("", _))) {
        IndexedSeq(TransformInput("uri", UriFixTransformer(), IndexedSeq(PathInput("path", UntypedPath.empty))))
      } else {
        segments.zipWithIndex.map { case (segment, idx) =>
          segment match {
            case PathPart(serializedPath, _) if idx == 0 =>
              // There is a path at the start of the URI pattern, this value needs to become a valid URI
              TransformInput("fixUri" + idx, UriFixTransformer(), IndexedSeq(PathInput("path" + idx, UntypedPath.parse(serializedPath))))
            case PathPart(serializedPath, _) =>
              TransformInput("encode" + idx, UrlEncodeTransformer(), IndexedSeq(PathInput("path" + idx, UntypedPath.parse(serializedPath))))
            case ConstantPart(value, _) =>
              TransformInput("constant" + idx, ConstantTransformer(value))
          }
        }
      }
    }

    TransformInput(id = "buildUri",transformer = ConcatTransformer(), inputs = inputs)
  }

  /**
    * Reconstructs a URI pattern from an input operator tree, if possible.
    */
  def unapply(input: Input): Option[String] = {
    input match {
      case TransformInput(_, ConcatTransformer("", false), inputs) if isPattern(inputs) =>
        Some(build(inputs))
      case _ =>
        None
    }
  }

  def isPattern(inputs: Seq[Input]): Boolean = {
    inputs.forall {
      case PathInput(id, path) => true
      case TransformInput(id, UriFixTransformer(_), Seq(PathInput(_, path))) => true
      case TransformInput(id, UrlEncodeTransformer(_), Seq(PathInput(_, path))) => true
      case TransformInput(id, ConstantTransformer(constant), Nil) => true
      case _ => false
    }
  }

  def build(inputs: Seq[Input]): String = {
    inputs.map {
      case PathInput(id, path) => "{" + path.serialize() + "}"
      case TransformInput(id, UriFixTransformer(_), Seq(PathInput(_, path))) => "{" + path.serialize() + "}"
      case TransformInput(id, UrlEncodeTransformer(_), Seq(PathInput(_, path))) => "{" + path.serialize() + "}"
      case TransformInput(id, ConstantTransformer(constant), Nil) => constant
    }.mkString("")
  }
}