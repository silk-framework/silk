package org.silkframework.execution.typed

import org.silkframework.config.TaskSpec
import org.silkframework.dataset.rdf._
import org.silkframework.entity._
import org.silkframework.entity.paths.TypedPath
import org.silkframework.execution.typed.TypedEntitiesVocab.{schemaPath, schemaType}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.util.Uri

object QuadEntitySchema extends TypedEntitySchema[Quad, TaskSpec] {

  /**
   * The fixed schema for this type.
   * Entities will be associated with this custom type based on the type URI of the schema, i.e., the type URI must be unique.
   */
  override val schema: EntitySchema = {
    EntitySchema(
      typeUri = schemaType("Quad"),
      typedPaths = IndexedSeq(
        TypedPath(schemaPath("quad/subject"), ValueType.STRING, isAttribute = false),
        TypedPath(schemaPath("quad/subjectType"), ValueType.STRING, isAttribute = false),
        TypedPath(schemaPath("quad/predicate"), ValueType.URI, isAttribute = false),
        TypedPath(schemaPath("quad/object"), ValueType.STRING, isAttribute = false),
        TypedPath(schemaPath("quad/objectType"), ValueType.STRING, isAttribute = false),
        TypedPath(schemaPath("quad/objectLanguage"), ValueType.STRING, isAttribute = false),
        TypedPath(schemaPath("quad/objectDataType"), ValueType.URI, isAttribute = false),
        TypedPath(schemaPath("quad/graph"), ValueType.STRING, isAttribute = false)
      )
    )
  }

  /**
   * Creates a generic entity from a typed entity.
   */
  override def toEntity(quad: Quad)(implicit pluginContext: PluginContext): Entity = {
    val subjectType = quad.subject match {
      case _: Resource => Types.URI
      case _: BlankNode => Types.BlankNode
    }

    val objectType = quad.objectVal match {
      case _: Resource => Types.URI
      case _: BlankNode => Types.BlankNode
      case _: PlainLiteral => Types.Literal
      case _: LanguageLiteral => Types.LangLiteral
      case _: DataTypeLiteral => Types.TypedLiteral
    }

    val objectLanguage = quad.objectVal match {
      case LanguageLiteral(_, l) => Seq(l)
      case _ => Seq.empty
    }

    val objectDataType = quad.objectVal match {
      case DataTypeLiteral(_, dt) => Seq(dt)
      case _ => Seq.empty
    }

    val uri = Uri.uuid(quad.subject.value + quad.predicate.value + quad.objectVal.value + objectLanguage.mkString + objectDataType.mkString + quad.context.map(_.value).mkString)

    val values = IndexedSeq(
      Seq(quad.subject.value),
      Seq(subjectType),
      Seq(quad.predicate.value),
      Seq(quad.objectVal.value),
      Seq(objectType),
      objectLanguage,
      objectDataType,
      quad.context.map(_.value).toSeq
    )

    Entity(uri, values, schema)
  }

  /**
   * Creates a typed entity from a generic entity.
   * Implementations may assume that the incoming schema matches the schema expected by this typed schema, i.e., schema validation is not required.
   */
  override def fromEntity(entity: Entity)(implicit pluginContext: PluginContext): Quad = {
    // Indices for the values in the entity
    val subjectIndex = 0
    val subjectTypeIndex = 1
    val predicateIndex = 2
    val objectIndex = 3
    val objectTypeIndex = 4
    val objectLanguageIndex = 5
    val objectDataTypeIndex = 6
    val graphIndex = 7

    // Extracting values from the entity
    val values = entity.values

    val subject = values(subjectTypeIndex) match {
      case Seq(Types.URI) => Resource(values(subjectIndex).head)
      case Seq(Types.BlankNode) => BlankNode(values(subjectIndex).head)
      case _ => throw new IllegalArgumentException("Invalid subject type. Expected URI or BlankNode.")
    }

    val predicate = Resource(values(predicateIndex).head)

    val objectValue = values(objectTypeIndex) match {
      case Seq(Types.URI) => Resource(values(objectIndex).head)
      case Seq(Types.BlankNode) => BlankNode(values(objectIndex).head)
      case Seq(Types.Literal) => PlainLiteral(values(objectIndex).head)
      case Seq(Types.LangLiteral) => LanguageLiteral(values(objectIndex).head, values(objectLanguageIndex).head)
      case Seq(Types.TypedLiteral) => DataTypeLiteral(values(objectIndex).head, values(objectDataTypeIndex).head)
      case _ => throw new IllegalArgumentException("Invalid object type. Expected URI, BlankNode, Literal, LangLiteral or TypedLiteral.")
    }

    val context = values(graphIndex).headOption.map(Resource)

    // Build the Quad object
    Quad(subject, predicate, objectValue, context)
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

  object Types {
    val URI: String = "URI"
    val BlankNode: String = "BlankNode"
    val Literal: String = "Literal"
    val LangLiteral: String = "LangLiteral"
    val TypedLiteral: String = "TypedLiteral"
  }
}
