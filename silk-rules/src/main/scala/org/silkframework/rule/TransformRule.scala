package org.silkframework.rule

import java.net.URI

import org.silkframework.config.MetaData.MetaDataXmlFormat
import org.silkframework.config.{MetaData, Prefixes}
import org.silkframework.dataset.TypedProperty
import org.silkframework.entity._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.rule.MappingRules.MappingRulesFormat
import org.silkframework.rule.MappingTarget.MappingTargetFormat
import org.silkframework.rule.TransformRule.RDF_TYPE
import org.silkframework.rule.input.{Input, PathInput, TransformInput}
import org.silkframework.rule.plugins.transformer.combine.ConcatTransformer
import org.silkframework.rule.plugins.transformer.normalize.{UriFixTransformer, UrlEncodeTransformer}
import org.silkframework.rule.plugins.transformer.value.{ConstantTransformer, ConstantUriTransformer, EmptyValueTransformer}
import org.silkframework.runtime.serialization._
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util._

import scala.language.implicitConversions
import scala.util.Try
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
  def apply(entity: Entity): Seq[String] = {
    val values = operator(entity)
    // Validate values
    for {
      valueType <- target.map(_.valueType)
      value <- values
    } {
      if(!valueType.validate(value)) {
        throw new ValidationException(s"Value '$value' is not a valid ${valueType.label}")
      }
    }
    values
  }

  /**
    * Collects all input paths in this rule.
    */
  def sourcePaths: Seq[TypedPath] = {
    def collectPaths(param: Input): Seq[TypedPath] = param match {
      case p: PathInput if p.path.operators.isEmpty => Seq()
      case PathInput(_, path: TypedPath) => Seq(path)
      case PathInput(_, path: UntypedPath) => Seq(TypedPath(path, UntypedValueType, isAttribute = false))
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
  override def withChildren(newChildren: Seq[Operator]): Operator = {
    if(newChildren.isEmpty) {
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
sealed trait ContainerTransformRule extends TransformRule

/**
  * Base trait for all rule that generate a value and do not have any child rules.
  */
sealed trait ValueTransformRule extends TransformRule

/**
  * The root mapping rule.
  *
  * @param id        The identifier of this mapping.
  * @param rules     The rules of this mapping.
  * @param metaData  The metadata.
  */
case class RootMappingRule(override val rules: MappingRules,
                           id: Identifier = RootMappingRule.defaultId,
                           metaData: MetaData = MetaData(RootMappingRule.defaultLabel)) extends ContainerTransformRule {

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

  def defaultId: String = "root"

  def defaultLabel: String = "Root Mapping"

  def empty: RootMappingRule = RootMappingRule(MappingRules.empty)

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
        metaData = (value \ "MetaData").headOption.map(MetaDataXmlFormat.read).getOrElse(MetaData.empty)
      )
    }

    /**
      * Serializes a value.
      */
    override def write(value: RootMappingRule)(implicit writeContext: WriteContext[Node]): Node = {
      <RootMappingRule id={value.id}>
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
  */
case class DirectMapping(id: Identifier = "sourcePath",
                         sourcePath: UntypedPath = UntypedPath(Nil),
                         mappingTarget: MappingTarget = MappingTarget("http://www.w3.org/2000/01/rdf-schema#label"),
                         metaData: MetaData = MetaData.empty) extends ValueTransformRule {

  override val operator = PathInput(id, sourcePath.asUntypedPath)

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
case class PatternUriMapping(id: Identifier = "uri",
                             pattern: String = "http://example.org/{ID}",
                             metaData: MetaData = MetaData.empty,
                             prefixes: Prefixes = Prefixes.empty) extends UriMapping {

  override val operator: Input = UriPattern.parse(pattern.trim())(prefixes)

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
case class ComplexUriMapping(id: Identifier = "complexUri",
                             operator: Input,
                             metaData: MetaData = MetaData.empty) extends UriMapping {

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

  override val target = Some(MappingTarget(RDF_TYPE, UriValueType))

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
                          metaData: MetaData = MetaData.empty) extends ValueTransformRule {

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
                         target: Option[MappingTarget] = Some(MappingTarget("http://www.w3.org/2002/07/owl#sameAs", UriValueType)),
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
            Some(ComplexUriMapping(rule.id, rewrittenInput, rule.metaData))
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

  override val operator: Input = {
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
  override def withChildren(newChildren: Seq[Operator]): Operator = {
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
          metaData = metaData
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
          </TransformRule>
      }
    }
  }

  /**
    * Tries to express a complex mapping as a basic mapping, such as a direct mapping.
    */
  def simplify(complexMapping: ComplexMapping)(implicit prefixes: Prefixes): TransformRule = complexMapping match {
    // Direct Mapping
    case ComplexMapping(id, PathInput(_, path), Some(target), metaData) =>
      DirectMapping(id, path.asUntypedPath, target, metaData)
    // Pattern URI Mapping
    case ComplexMapping(id, TransformInput(_, ConcatTransformer(""), inputs), None, metaData) if UriPattern.isPattern(inputs) =>
      PatternUriMapping(id, UriPattern.build(inputs), metaData, prefixes = prefixes)
    // Complex URI mapping
    case ComplexMapping(id, operator, None, metaData) =>
      ComplexUriMapping(id, operator, metaData)
    // Object Mapping (old style, to be removed)
    case ComplexMapping(id, TransformInput(_, ConcatTransformer(""), inputs), Some(target), metaData) if UriPattern.isPattern(inputs) && target.valueType == UriValueType =>
      ObjectMapping(id, UntypedPath.empty, Some(target), MappingRules(uriRule = Some(PatternUriMapping(id + "uri", UriPattern.build(inputs)))), metaData, prefixes = prefixes)
    // Type Mapping
    case ComplexMapping(id, TransformInput(_, ConstantTransformer(typeUri), Nil), Some(MappingTarget(Uri(RDF_TYPE), _, false, _)), metaData) =>
      TypeMapping(id, typeUri, metaData)
    // Type Mapping (old style, to be removed)
    case ComplexMapping(id, TransformInput(_, ConstantUriTransformer(typeUri), Nil), Some(MappingTarget(Uri(RDF_TYPE), _, false, _)), metaData) =>
      TypeMapping(id, typeUri, metaData)
    // Complex Mapping
    case _ => complexMapping
  }


}

/**
  * Handles URI patterns of the form http://example.org/{ID}.
  */
private object UriPattern {

  def parse(pattern: String)(implicit prefixes: Prefixes): Input = {
    // FIXME we should write a real parser for this
    val inputs = {
      if(pattern == "{}") {
        Seq(TransformInput("uri", UriFixTransformer(), Seq(PathInput("path", UntypedPath.empty))))
      } else {
        var firstConstant: String = ""
        for ((str, i) <- pattern.split("[\\{\\}]").toList.zipWithIndex) yield {
          if (i % 2 == 0) {
            if(i == 0) {
              firstConstant = str
            }
            TransformInput("constant" + i, ConstantTransformer(str))
          } else {
            if(i == 1 && firstConstant == "") {
              // There is a path at the start of the URI pattern, this value needs to become a valid URI
              TransformInput("fixUri" + i, UriFixTransformer(), Seq(PathInput("path" + i, UntypedPath.parse(str))))
            } else {
              TransformInput("encode" + i, UrlEncodeTransformer(), Seq(PathInput("path" + i, UntypedPath.parse(str))))
            }
          }
        }
      }
    }

    TransformInput(id = "buildUri",transformer = ConcatTransformer(), inputs = inputs)
  }

  def isPattern(inputs: Seq[Input]): Boolean = {
    inputs.forall {
      case PathInput(id, path) => true
      case TransformInput(id, UrlEncodeTransformer(_), Seq(PathInput(_, path))) => true
      case TransformInput(id, ConstantTransformer(constant), Nil) => true
      case _ => false
    }
  }

  def build(inputs: Seq[Input]): String = {
    inputs.map {
      case PathInput(id, path) => "{" + path.serialize() + "}"
      case TransformInput(id, UrlEncodeTransformer(_), Seq(PathInput(_, path))) => "{" + path.serialize() + "}"
      case TransformInput(id, ConstantTransformer(constant), Nil) => constant
    }.mkString("")
  }

}
