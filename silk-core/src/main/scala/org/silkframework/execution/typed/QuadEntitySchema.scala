package org.silkframework.execution.typed

import org.silkframework.config.{SilkVocab, TaskSpec}
import org.silkframework.dataset.rdf._
import org.silkframework.entity._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.util.Uri

object QuadEntitySchema extends TypedEntitySchema[Quad, TaskSpec] {

  /**
   * The fixed schema for this type.
   * Entities will be associated with this custom type based on the type URI of the schema, i.e., the type URI must be unique.
   */
  override val schema: EntitySchema = {
    EntitySchema(
      typeUri = Uri(SilkVocab.QuadSchemaType),
      typedPaths = IndexedSeq(
        TypedPath(UntypedPath(SilkVocab.tripleSubject), ValueType.URI, isAttribute = false),
        TypedPath(UntypedPath(SilkVocab.triplePredicate), ValueType.URI, isAttribute = false),
        TypedPath(UntypedPath(SilkVocab.tripleObject), ValueType.STRING, isAttribute = false),
        TypedPath(UntypedPath(SilkVocab.tripleObjectValueType), ValueType.STRING, isAttribute = false),
        TypedPath(UntypedPath(SilkVocab.quadContext), ValueType.STRING, isAttribute = false)
      )
    )
  }

  /**
   * Creates a generic entity from a typed entity.
   */
  override def toEntity(quad: Quad)(implicit pluginContext: PluginContext): Entity = {
    val (value, typ) = convertToEncodedType(quad.objectVal)
    val values = IndexedSeq(
      Seq(quad.subject.value),
      Seq(quad.predicate.value),
      Seq(value),
      Seq(typ),
      quad.context.map(_.value).toSeq
    )
    Entity(Uri.uuid(quad.subject.value + quad.predicate.value + value + quad.context.map(_.value).getOrElse("")), values, schema)
  }

  /**
   * Creates a typed entity from a generic entity.
   * Implementations may assume that the incoming schema matches the schema expected by this typed schema, i.e., schema validation is not required.
   */
  override def fromEntity(entity: Entity)(implicit pluginContext: PluginContext): Quad = {
    val Seq(Some(s), Some(p), Some(o), Some(encodedType), context) = entity.values.map(_.headOption)
    Quad(Resource(s), Resource(p), convertToRdfNode(o, encodedType), context.map(Resource))
  }

  def getValueType(node: RdfNode): ValueType = {
    node match {
      case PlainLiteral(_) =>
        ValueType.STRING
      case LanguageLiteral(_, l) =>
        LanguageValueType(l)
      case DataTypeLiteral(_, dt) =>
        CustomValueType(dt)
      case BlankNode(_) =>
        ValueType.BLANK_NODE
      case Resource(_) =>
        ValueType.URI
    }
  }

  private final val LANGUAGE_ENC_PREFIX = "lg"
  private final val DATA_TYPE_ENC_PREFIX = "dt"
  private final val URI_ENC_PREFIX = "ur"
  private final val BLANK_NODE_ENC_PREFIX = "bn"

  private def convertToEncodedType(valueType: RdfNode): (String, String) = {
    valueType match {
      case PlainLiteral(v) =>
        (v, "")
      case LanguageLiteral(v, l) =>
        (v, s"$LANGUAGE_ENC_PREFIX=$l")
      case DataTypeLiteral(v, dt) =>
        (v, s"$DATA_TYPE_ENC_PREFIX=$dt")
      case BlankNode(bn) =>
        (bn, s"$BLANK_NODE_ENC_PREFIX")
      case Resource(uri) =>
        (uri, s"$URI_ENC_PREFIX")
    }
  }

  private def convertToRdfNode(value: String, encodedType: String): RdfNode = {
    encodedType.take(2) match {
      case DATA_TYPE_ENC_PREFIX =>
        Resource(value)
      case LANGUAGE_ENC_PREFIX =>
        LanguageLiteral(value, encodedType.drop(3))
      case URI_ENC_PREFIX =>
        Resource(value)
      case BLANK_NODE_ENC_PREFIX =>
        BlankNode(value)
      case _ =>
        Resource(value)
    }
  }
}
