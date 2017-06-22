package org.silkframework.rule

import org.silkframework.config.MetaData
import org.silkframework.config.MetaData.MetaDataFormat
import org.silkframework.dataset.TypedProperty
import org.silkframework.entity._
import org.silkframework.rule.MappingRules.MappingRulesFormat
import org.silkframework.rule.MappingTarget.MappingTargetFormat
import org.silkframework.rule.TransformRule.TransformRuleFormat.write
import org.silkframework.rule.input.{Input, PathInput, TransformInput}
import org.silkframework.rule.plugins.transformer.combine.ConcatTransformer
import org.silkframework.rule.plugins.transformer.normalize.UrlEncodeTransformer
import org.silkframework.rule.plugins.transformer.value.{ConstantTransformer, ConstantUriTransformer, EmptyValueTransformer}
import org.silkframework.runtime.serialization._
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util._
import TransformRule.RDF_TYPE

import scala.language.implicitConversions
import scala.xml.{Node, Null}

/**
  * A transformation rule.
  * A transformations rule generates property values from based on an arbitrary operator tree consisting of property paths and transformations.
  * Sub classes are defined for special cases, such as direct mappings.
  */
sealed trait TransformRule extends Operator {

  /** The name of this rule. */
  def id: Identifier

  /** The meta data for this rule. */
  def metaData: MetaData

  /** The input operator tree. */
  def operator: Input

  /** The target property URI. */
  def target: Option[MappingTarget]

  /** String representation of rule type */
  def typeString: String

  def rules: MappingRules = MappingRules.empty

  /**
    * Generates the transformed values.
    *
    * @param entity The source entity.
    * @return The transformed values.
    * @throws ValidationException If a value failed to be transformed or a generated value doesn't match the target type.
    */
  def apply(entity: Entity): Seq[String] = {
    val values = operator(entity)
    // Validate values
    for {
      valueType <- target.map(_.valueType) if valueType != AutoDetectValueType
      value <- values
    } {
      if(!valueType.validate(value)) {
        throw new ValidationException(s"Value '$value' is not a valid ${valueType.label}")
      }
    }
    values
  }

  /**
    * Collects all paths in this rule.
    */
  def paths: Seq[Path] = {
    def collectPaths(param: Input): Seq[Path] = param match {
      case p: PathInput if p.path.operators.isEmpty => Seq()
      case p: PathInput => Seq(p.path)
      case p: TransformInput => p.inputs.flatMap(collectPaths)
    }

    collectPaths(operator).distinct
  }

  /**
    * The children operators.
    */
  def children: Seq[Operator] = rules.allRules

  /**
    * Generates the same operator with new children.
    */
  override def withChildren(newChildren: Seq[Operator]): Operator = {
    if(newChildren.isEmpty) {
      this
    } else {
      throw new IllegalArgumentException(s"$this cannot have any children")
    }
  }
}

case class RootMappingRule(id: Identifier, override val rules: MappingRules, metaData: MetaData = MetaData.empty) extends TransformRule {

  /**
    * The children operators.
    */
  override def children: Seq[Operator] = rules.allRules

  /**
    * Generates the same operator with new children.
    */
  override def withChildren(newChildren: Seq[Operator]): Operator = {
    val newRules = newChildren.map(_.asInstanceOf[TransformRule])
    this.copy(rules = MappingRules.fromSeq(newRules))
  }

  /** The input operator tree. */
  override def operator: Input = TransformInput("root", EmptyValueTransformer())

  /** The target property URI. */
  override def target: Option[MappingTarget] = None

  /** String representation of rule type */
  override def typeString: String = "Root"
}

object RootMappingRule {

  def apply(rules: MappingRules): RootMappingRule = RootMappingRule("root", rules)

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
        metaData = (value \ "MetaData").headOption.map(MetaDataFormat.read).getOrElse(MetaData.empty)
      )
    }

    /**
      * Serializes a value.
      */
    override def write(value: RootMappingRule)(implicit writeContext: WriteContext[Node]): Node = {
      <RootMappingRule id={value.id}>
        { MappingRulesFormat.write(value.rules) }
        { MetaDataFormat.write(value.metaData) }
      </RootMappingRule>
    }
  }

  implicit def toTypedProperty(mt: MappingTarget): TypedProperty = TypedProperty(mt.propertyUri.uri, mt.valueType, mt.isBackwardProperty)

}

/**
  * A direct mapping between two properties.
  *
  * @param id             The name of this mapping. For direct mappings usually just the property that is mapped.
  * @param sourcePath     The source path
  * @param mappingTarget  The target property
  */
case class DirectMapping(id: Identifier = "sourcePath",
                         sourcePath: Path = Path(Nil),
                         mappingTarget: MappingTarget = MappingTarget("http://www.w3.org/2000/01/rdf-schema#label"),
                         metaData: MetaData = MetaData.empty) extends TransformRule {

  override val operator = PathInput(id, sourcePath)

  override val target = Some(mappingTarget)

  override val typeString = "Direct"
}

/**
  * Assigns a new URI to each mapped entity.
  *
  * @param id      The name of this mapping
  * @param pattern A template pattern for generating the URIs based on the entity properties
  */
case class UriMapping(id: Identifier = "uri", pattern: String = "http://example.org/{ID}", metaData: MetaData = MetaData.empty) extends TransformRule {

  override val operator: Input = {
    val inputs =
      for ((str, i) <- pattern.split("[\\{\\}]").toList.zipWithIndex) yield {
        if (i % 2 == 0)
          TransformInput("constant" + i, ConstantTransformer(str))
        else
          TransformInput("encode" + i, UrlEncodeTransformer(), Seq(PathInput("path" + i, Path.parse(str))))
      }
    TransformInput(transformer = ConcatTransformer(""), inputs = inputs)
  }

  override val target = None

  override val typeString = "URI"
}

/**
  * A type mapping, which assigns a type to each entitity.
  *
  * @param id      The name of this mapping
  * @param typeUri The type URI.
  */
case class TypeMapping(id: Identifier = "type", typeUri: Uri = "http://www.w3.org/2002/07/owl#Thing", metaData: MetaData = MetaData.empty) extends TransformRule {

  override val operator = TransformInput("generateType", ConstantUriTransformer(typeUri))

  override val target = Some(MappingTarget(RDF_TYPE, UriValueType))

  override val typeString = "Type"

}

/**
  * A complex mapping, which generates property values from based on an arbitrary operator tree consisting of property paths and transformations.
  *
  * @param id       The name of this mapping
  * @param operator The input operator tree
  * @param target   The target property URI
  */
case class ComplexMapping(id: Identifier = "mapping", operator: Input, target: Option[MappingTarget] = None, metaData: MetaData = MetaData.empty) extends TransformRule {

  override val typeString = "Complex"

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
                         sourcePath: Path = Path(Nil),
                         target: Option[MappingTarget] = Some(MappingTarget("http://www.w3.org/2002/07/owl#sameAs", UriValueType)),
                         override val rules: MappingRules,
                         metaData: MetaData = MetaData.empty) extends TransformRule {

  override val typeString = "Object"

  override val operator = {
    target match {
      case Some(prop) =>
        rules.uriRule match {
          case Some (rule) => rule.operator
          case None => PathInput (path = sourcePath)
        }
      case None =>
        TransformInput(transformer = EmptyValueTransformer())
    }
  }

  /**
    * Generates the same operator with new children.
    */
  override def withChildren(newChildren: Seq[Operator]): Operator = {
    val newRules = newChildren.map(_.asInstanceOf[TransformRule])
    this.copy(rules = MappingRules.fromSeq(newRules))
  }

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
        sourcePath = Path.parse((node \ "@relativePath").text),
        target = (node \ "MappingTarget").headOption.map(fromXml[MappingTarget]),
        rules = MappingRules.fromSeq((node \ "MappingRules" \ "_").map(read)),
        metaData = (node \ "MetaData").headOption.map(MetaDataFormat.read).getOrElse(MetaData.empty)
      )
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

      val metaData = (node \ "MetaData").headOption.map(MetaDataFormat.read).getOrElse(MetaData.empty)

      val complex =
        ComplexMapping(
          id = (node \ "@name").text,
          operator = fromXml[Input]((node \ "Input" ++ node \ "TransformInput").head),
          target = target,
          metaData = metaData
        )
      simplify(complex)
    }

    def write(value: TransformRule)(implicit writeContext: WriteContext[Node]): Node = {
      value match {
        case ObjectMapping(name, relativePath, target, childRules, metaData) =>
          <ObjectMapping name={name} relativePath={relativePath.serialize} >
            {MetaDataFormat.write(metaData)}
            {MappingRulesFormat.write(childRules)}
            { target.map(toXml[MappingTarget]).toSeq }
          </ObjectMapping>
        case _ =>
          // At the moment, all other types are serialized generically
          <TransformRule name={value.id}>
            {MetaDataFormat.write(value.metaData)}
            {toXml(value.operator)}{value.target.map(toXml[MappingTarget]).getOrElse(Null)}
          </TransformRule>
      }
    }
  }

  /**
    * Tries to express a complex mapping as a basic mapping, such as a direct mapping.
    */
  def simplify(complexMapping: ComplexMapping): TransformRule = complexMapping match {
    // Direct Mapping
    case ComplexMapping(id, PathInput(_, path), Some(target), metaData) =>
      DirectMapping(id, path, target, metaData)
    // URI Mapping
    case ComplexMapping(id, TransformInput(_, ConcatTransformer(""), inputs), None, metaData) if isPattern(inputs) =>
      UriMapping(id, buildPattern(inputs), metaData)
    // Object Mapping (old style, to be removed)
    case ComplexMapping(id, TransformInput(_, ConcatTransformer(""), inputs), Some(target), metaData) if isPattern(inputs) && target.valueType == UriValueType =>
      ObjectMapping(id, Path.empty, Some(target), MappingRules(uriRule = Some(UriMapping(id + "uri", buildPattern(inputs)))), metaData)
    // Type Mapping
    case ComplexMapping(id, TransformInput(_, ConstantTransformer(typeUri), Nil),
    Some(MappingTarget(Uri(RDF_TYPE), _, false)), metaData) =>
      TypeMapping(id, typeUri, metaData)
    // Type Mapping (old style, to be removed)
    case ComplexMapping(id, TransformInput(_, ConstantUriTransformer(typeUri), Nil),
    Some(MappingTarget(Uri(RDF_TYPE), _, false)), metaData) =>
      TypeMapping(id, typeUri, metaData)
    // Complex Mapping
    case _ => complexMapping
  }

  private def isPattern(inputs: Seq[Input]) = {
    inputs.forall {
      case PathInput(id, path) => true
      case TransformInput(id, UrlEncodeTransformer(_), Seq(PathInput(_, path))) => true
      case TransformInput(id, ConstantTransformer(constant), Nil) => true
      case _ => false
    }
  }

  private def buildPattern(inputs: Seq[Input]) = {
    inputs.map {
      case PathInput(id, path) => "{" + path.serializeSimplified() + "}"
      case TransformInput(id, UrlEncodeTransformer(_), Seq(PathInput(_, path))) => "{" + path.serializeSimplified() + "}"
      case TransformInput(id, ConstantTransformer(constant), Nil) => constant
    }.mkString("")
  }
}
