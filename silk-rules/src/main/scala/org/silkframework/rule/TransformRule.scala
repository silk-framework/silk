package org.silkframework.rule

import org.silkframework.dataset.TypedProperty
import org.silkframework.entity._
import org.silkframework.rule.input.{Input, PathInput, TransformInput}
import org.silkframework.rule.plugins.transformer.combine.ConcatTransformer
import org.silkframework.rule.plugins.transformer.value.{ConstantTransformer, ConstantUriTransformer}
import org.silkframework.runtime.serialization._
import org.silkframework.util._
import scala.language.implicitConversions

import scala.xml.{Node, Null}

/**
  * A transformation rule.
  * A transformations rule generates property values from based on an arbitrary operator tree consisting of property paths and transformations.
  * Sub classes are defined for special cases, such as direct mappings.
  */
sealed trait TransformRule {

  /** The name of this rule. */
  def name: Identifier

  /** The input operator tree. */
  def operator: Input

  /** The target property URI. */
  def target: Option[MappingTarget]

  /** String representation of rule type */
  def typeString: String

  /**
    * Generates the transformed values.
    *
    * @param entity The source entity.
    * @return The transformed values.
    */
  def apply(entity: Entity): Seq[String] = {
    operator(entity)
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
}

case class MappingTarget(propertyUri: Uri, valueType: ValueType = AutoDetectValueType) {

  override def toString: String = {
    if(valueType == AutoDetectValueType)
      propertyUri.toString
    else
      s"$propertyUri ($valueType)"
  }

}

object MappingTarget {

  implicit object MappingTargetFormat extends XmlFormat[MappingTarget] {

    import XmlSerialization._

    /**
      * Deserializes a value.
      */
    override def read(value: Node)(implicit readContext: ReadContext): MappingTarget = {
      val uri = (value \ "@uri").text.trim
      val valueTypeNode = (value \ "ValueType").head
      MappingTarget(Uri.parse(uri, readContext.prefixes), fromXml[ValueType](valueTypeNode))
    }

    /**
      * Serializes a value.
      */
    override def write(value: MappingTarget)(implicit writeContext: WriteContext[Node]): Node = {
      <MappingTarget uri={value.propertyUri.uri}>
        {toXml[ValueType](value.valueType)}
      </MappingTarget>
    }
  }

  implicit def toTypedProperty(mt: MappingTarget): TypedProperty = TypedProperty(mt.propertyUri.uri, mt.valueType)

}

/**
  * A direct mapping between two properties.
  *
  * @param name           The name of this mapping. For direct mappings usually just the property that is mapped.
  * @param sourcePath     The source path
  * @param mappingTarget  The target property
  */
case class DirectMapping(name: Identifier = "sourcePath",
                         sourcePath: Path = Path(Nil),
                         mappingTarget: MappingTarget = MappingTarget("http://www.w3.org/2000/01/rdf-schema#label")) extends TransformRule {

  override val operator = PathInput(name, sourcePath)

  override val target = Some(mappingTarget)

  override val typeString = "Direct"
}

/**
  * Assigns a new URI to each mapped entity.
  *
  * @param name    The name of this mapping
  * @param pattern A template pattern for generating the URIs based on the entity properties
  */
case class UriMapping(name: Identifier = "uri", pattern: String = "http://example.org/{ID}") extends TransformRule {

  override val operator = {
    val inputs =
      for ((str, i) <- pattern.split("[\\{\\}]").toList.zipWithIndex) yield {
        if (i % 2 == 0)
          TransformInput("constant" + i, ConstantTransformer(str))
        else
          PathInput("path" + i, Path.parse(str))
      }
    TransformInput(transformer = ConcatTransformer(""), inputs = inputs)
  }

  override val target = None

  override val typeString = "URI"
}

/**
  * Generates a link to another entity.
  *
  * @param name    The name of this mapping
  * @param pattern A template pattern for generating the URIs based on the entity properties
  */
case class ObjectMapping(name: Identifier = "object",
                         pattern: String = "http://example.org/{ID}",
                         targetProperty: MappingTarget = MappingTarget("http://www.w3.org/2002/07/owl#sameAs", UriValueType)) extends TransformRule {

  override val operator = {
    val inputs =
      for ((str, i) <- pattern.split("[\\{\\}]").toList.zipWithIndex) yield {
        if (i % 2 == 0)
          TransformInput("constant" + i, ConstantTransformer(str))
        else
          PathInput("path" + i, Path.parse(str))
      }
    TransformInput(transformer = ConcatTransformer(""), inputs = inputs)
  }

  override val target = Some(targetProperty)

  override val typeString = "Object"

}

/**
  * A type mapping, which assigns a type to each entitity.
  *
  * @param name    The name of this mapping
  * @param typeUri The type URI.
  */
case class TypeMapping(name: Identifier = "type", typeUri: Uri = "http://www.w3.org/2002/07/owl#Thing") extends TransformRule {

  override val operator = TransformInput("generateType", ConstantUriTransformer(typeUri))

  override val target = Some(MappingTarget("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", UriValueType))

  override val typeString = "Type"

}

/**
  * A complex mapping, which generates property values from based on an arbitrary operator tree consisting of property paths and transformations.
  *
  * @param name     The name of this mapping
  * @param operator The input operator tree
  * @param target   The target property URI
  */
case class ComplexMapping(name: Identifier = "mapping", operator: Input, target: Option[MappingTarget] = None) extends TransformRule {

  override val typeString = "Complex"

}

/**
  * Creates new transform rules.
  */
object TransformRule {

  /**
    * XML serialization format.
    */
  implicit object TransformRuleFormat extends XmlFormat[TransformRule] {

    import XmlSerialization._

    def read(node: Node)(implicit readContext: ReadContext): TransformRule = {
      ValidatingXMLReader.validate(node, "org/silkframework/LinkSpecificationLanguage.xsd")
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
      val complex =
        ComplexMapping(
          name = (node \ "@name").text,
          operator = fromXml[Input]((node \ "_").head),
          target = target
        )
      simplify(complex)
    }

    def write(value: TransformRule)(implicit writeContext: WriteContext[Node]): Node = {
      <TransformRule name={value.name}>
        {toXml(value.operator)}{value.target.map(toXml[MappingTarget]).getOrElse(Null)}
      </TransformRule>
    }
  }

  /**
    * Tries to express a complex mapping as a basic mapping, such as a direct mapping.
    */
  def simplify(complexMapping: ComplexMapping): TransformRule = complexMapping match {
    // Direct Mapping
    case ComplexMapping(id, PathInput(_, path), Some(target)) =>
      DirectMapping(id, path, target)
    // URI Mapping
    case ComplexMapping(id, TransformInput(_, ConcatTransformer(""), inputs), None) if isPattern(inputs) =>
      UriMapping(id, buildPattern(inputs))
    // Object Mapping
    case ComplexMapping(id, TransformInput(_, ConcatTransformer(""), inputs), Some(target)) if isPattern(inputs) =>
      ObjectMapping(id, buildPattern(inputs), target)
    // Type Mapping
    case ComplexMapping(id, TransformInput(_, ConstantTransformer(typeUri), Nil),
    Some(MappingTarget(Uri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), _))) =>
      TypeMapping(id, typeUri)
    // Type Mapping (old style, to be removed)
    case ComplexMapping(id, TransformInput(_, ConstantUriTransformer(typeUri), Nil),
    Some(MappingTarget(Uri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), _))) =>
      TypeMapping(id, typeUri)
    // Complex Mapping
    case _ => complexMapping
  }

  private def isPattern(inputs: Seq[Input]) = {
    inputs.forall {
      case PathInput(id, path) => true
      case TransformInput(id, ConstantTransformer(constant), Nil) => true
      case _ => false
    }
  }

  private def buildPattern(inputs: Seq[Input]) = {
    inputs.map {
      case PathInput(id, path) => "{" + path.serializeSimplified() + "}"
      case TransformInput(id, ConstantTransformer(constant), Nil) => constant
    }.mkString("")
  }
}
