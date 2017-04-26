package org.silkframework.rule

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.entity._
import org.silkframework.util.{Identifier, Uri}

/**
  * Specification of transform specification task
  */
class TransformSpecTest extends FlatSpec with MustMatchers {
  behavior of "transform specification"
  private final val SOURCE_LEVEL1 = "sourceLevel1"
  private final val SOURCE_LEVEL2 = "sourceLevel2"

  val nestedTransformation = TransformSpec(
    DatasetSelection.empty,
    rules = Seq(
      directMapping("target1", "sourceValue1"),
      typedDirectMapping("target2", IntValueType, SOURCE_LEVEL1, "sourceValue2"),
      directMapping("target3", SOURCE_LEVEL1, SOURCE_LEVEL2, "sourceValue3"),
      // Nested mapping staying on the same source level, this created new entities in the target
      nestedMapping(Path(Nil), targetProperty = Some("propNestedResource1"), childMappings = Seq(
        directMapping("target4", "sourceValue4"),
        directMapping("target5", "sourceValue1"),
        directMapping("target6", SOURCE_LEVEL1, "sourceValue5"),
        // Transitive 2-hop mapping still staying on the same level
        nestedMapping(Path(Nil), targetProperty = Some("propNestedResource2"), childMappings = Seq(
          typedDirectMapping("target7", DoubleValueType, "sourceValue6")
        ))
      )),
      // Nested mapping that is on a different source level AND on a different target level
      nestedMapping(path(s"$SOURCE_LEVEL1/$SOURCE_LEVEL2"), targetProperty = Some("propDeepNestedResource1"), childMappings = Seq(
        directMapping("target8", "sourceValue7")
      )),
      // Nested mapping that stays on the same property level, this means it is flattening the source schema
      nestedMapping(path(SOURCE_LEVEL1), targetProperty = None, childMappings = Seq(
        directMapping("target9", "sourceValue8")
      ))
    )
  )

  private var mappingCounter = 0

  private def mappingId(): Identifier = {
    mappingCounter += 1
    "mapping" + mappingCounter
  }

  private def nestedMapping(sourcePath: Path, targetProperty: Option[String], childMappings: Seq[TransformRule]): TransformRule = {
    HierarchicalMapping(mappingId(), sourcePath, targetProperty.map(Uri(_)), childMappings)
  }

  private def directMapping(targetProperty: String, sourcePaths: String*): TransformRule = {
    typedDirectMapping(targetProperty, AutoDetectValueType, sourcePaths :_*)
  }

  private def typedDirectMapping(targetProperty: String, targetType: ValueType, sourcePaths: String*): TransformRule = {
    val ops = sourcePaths.map(ForwardOperator(_)).toList
    val mappingTarget = MappingTarget(targetProperty, targetType)
    DirectMapping(mappingId(), sourcePath = Path(ops), mappingTarget = mappingTarget)
  }

  private def uriMapping(uriTemplate: String): TransformRule = {
    UriMapping(mappingId(), uriTemplate)
  }

  private def path(pathStr: String): Path = Path.parse(pathStr)

  private def typedPath(valueType: ValueType)(pathStr: String): TypedPath = TypedPath(path(pathStr), valueType)
  private def stringTypedPath = typedPath(StringValueType) _
  private def autoDetectTypedPath = typedPath(AutoDetectValueType) _
  private def intTypedPath = typedPath(IntValueType) _
  private def doubleTypedPath = typedPath(DoubleValueType) _


  it should "generate correct nested input schema" in {
    nestedTransformation.inputSchemataOpt mustBe Some(Seq(
      NestedEntitySchema(
        NestedSchemaNode(
          entitySchema = EntitySchema("", IndexedSeq(
            stringTypedPath("sourceValue1"),
            stringTypedPath("sourceLevel1/sourceValue2"),
            stringTypedPath("sourceLevel1/sourceLevel2/sourceValue3"),
            stringTypedPath("sourceValue4"),
            stringTypedPath("sourceLevel1/sourceValue5"),
            stringTypedPath("sourceValue6")
          )),
          nestedEntities = IndexedSeq(
            (EntitySchemaConnection(path("sourceLevel1/sourceLevel2")),
                NestedSchemaNode(EntitySchema(
                  "",
                  IndexedSeq(stringTypedPath("sourceValue7"))),
                  IndexedSeq())),
            (EntitySchemaConnection(path(SOURCE_LEVEL1)),
                NestedSchemaNode(EntitySchema(
                  "",
                  IndexedSeq(stringTypedPath("sourceValue8"))),
                  IndexedSeq()))
          ))))
    )
  }

  it should "generate correct nested output schema" in {
    nestedTransformation.outputSchemaOpt mustBe Some(
      NestedEntitySchema(
        NestedSchemaNode(
          entitySchema = EntitySchema(
            typeUri = "",
            typedPaths = IndexedSeq(
              autoDetectTypedPath("target1"),
              intTypedPath("target2"),
              autoDetectTypedPath("target3"),
              autoDetectTypedPath("target9")
            )
          ),
          nestedEntities = IndexedSeq(
            ( EntitySchemaConnection(path("propNestedResource1")), propNestedResource1),
            ( EntitySchemaConnection(path("propDeepNestedResource1")), propDeepNestedResource1)
          )
        )
      )
    )
  }

  private def propNestedResource1 = {
    NestedSchemaNode(
      EntitySchema(
        "",
        typedPaths = IndexedSeq(
          autoDetectTypedPath("target4"),
          autoDetectTypedPath("target5"),
          autoDetectTypedPath("target6")
        )
      ),
      nestedEntities = IndexedSeq(
        ( EntitySchemaConnection(path("propNestedResource2")), propNestedResource2)
      )
    )
  }

  private def propNestedResource2 = {
    NestedSchemaNode(
      EntitySchema(
        "",
        typedPaths = IndexedSeq(
          doubleTypedPath("target7")
        )
      ),
      nestedEntities = IndexedSeq()
    )
  }

  private def propDeepNestedResource1 = {
    NestedSchemaNode(
      EntitySchema(
        "",
        typedPaths = IndexedSeq(
          autoDetectTypedPath("target8")
        )
      ),
      nestedEntities = IndexedSeq()
    )
  }
}