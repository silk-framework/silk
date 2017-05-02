package org.silkframework.rule.execution.local

import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.config.PlainTask
import org.silkframework.entity._
import org.silkframework.execution.local.{LocalExecution, NestedEntityTable}
import org.silkframework.execution.{ExecutionReport, ExecutorRegistry}
import org.silkframework.rule.TransformSpecHelper._
import org.silkframework.rule.{DatasetSelection, TransformSpec}
import org.silkframework.runtime.activity.ActivityContext

/**
  * Tests for the local transform specification executor.
  */
class LocalTransformSpecificationExecutorTest extends FlatSpec with MustMatchers with ExecutorRegistry with MockitoSugar {
  behavior of "Local Transform Specification Executor"
  private final val SOURCE_LEVEL1 = "sourceLevel1"
  private final val SOURCE_LEVEL2 = "sourceLevel2"
  final private val SOURCE_ENTITY = "sourceEntity"

  it should "load from the registry" in {
    executor(TransformSpec(null, null), LocalExecution(false)).getClass mustBe classOf[LocalTransformSpecificationExecutor]
  }
  val transformer = new LocalTransformSpecificationExecutor()
  val nestedTransformSpec = TransformSpec(
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

  /** Compares two nested entities recursively to better highlight differences */
  private def validateNestedEntities(expectedEntity: NestedEntity, entity: NestedEntity): Unit = {
    entity.uri mustBe expectedEntity.uri
    entity.values mustBe expectedEntity.values
    entity.nestedEntities.size mustBe expectedEntity.nestedEntities.size
    for((children, expectedChildren) <- entity.nestedEntities.zip(expectedEntity.nestedEntities)) {
      children.size mustBe expectedChildren.size
      for((child, expectedChild) <- children.zip(expectedChildren)) {
        validateNestedEntities(expectedChild, child)
      }
    }
  }

  private def nestedTargetEntity(nr: Int) = {
    nestedTargetEntityDeeper(SOURCE_ENTITY + nr,
      valNrs = Seq(1, "2", 3, (2, "val8")),
      nestedEntities = IndexedSeq(
        Seq(
          nestedTargetEntityDeeper(SOURCE_ENTITY + nr,
            valNrs = Seq(4, 1, 5),
            nestedEntities = IndexedSeq(
              Seq(
                nestedTargetEntityDeeper(SOURCE_ENTITY + nr,
                  valNrs = Seq("6"),
                  nestedEntities = IndexedSeq())
              )
            )
          )
        ),
        Seq(
          nestedTargetEntityDeeper("entity2a",
            valNrs = Seq(7),
            nestedEntities = IndexedSeq()
          ),
          nestedTargetEntityDeeper("entity2b",
            valNrs = Seq(7),
            nestedEntities = IndexedSeq()
          )
        )
      )
    )
  }

  private val expectedTargetEntities = Seq(nestedTargetEntity(1), nestedTargetEntity(2))

  private def nestedTargetEntityDeeper(entityUri: String, valNrs: Seq[Any], nestedEntities: IndexedSeq[Seq[NestedEntity]]) = {
    val propValues = valNrs.map{
      case nr: Int => Seq(s"val$nr")
      case str: String => Seq(str)
      case (times: Int, str: String) =>
        for(_ <- 1 to times) yield {
          str
        }
      case a: Any =>
        throw new RuntimeException("Cannot handle type: " + a.getClass.getSimpleName)
    }.toIndexedSeq
    NestedEntity(entityUri,
      values = propValues,
      nestedEntities = nestedEntities
    )
  }

  private def nestedSourceEntity(nr: Int) = NestedEntity(SOURCE_ENTITY + nr,
    values = IndexedSeq(
      Seq("val1"),
      Seq("2"),
      Seq("val3"),
      Seq("val4"),
      Seq("val5"),
      Seq("6")
    ),
    nestedEntities = IndexedSeq(
      Seq(nestedSourceEntityLevel2("a", valNr = 7), nestedSourceEntityLevel2("b", valNr = 7)),
      Seq(nestedSourceEntityLevel2("c", valNr = 8), nestedSourceEntityLevel2("d", valNr = 8))
    )
  )

  private def nestedSourceEntityLevel2(id: String, valNr: Int) = NestedEntity("entity2" + id,
    values = IndexedSeq(
      Seq(s"val$valNr")
    ),
    nestedEntities = IndexedSeq()
  )

  val transformTask = PlainTask("nestedTransformTask", nestedTransformSpec)

  val nestedEntityTable = NestedEntityTable(
    entities = Seq(
      nestedSourceEntity(1),
      nestedSourceEntity(2)
    ),
    entitySchema = SchemaTrait.toNestedSchema(nestedTransformSpec.inputSchema),
    task = transformTask
  )

  it should "transform nested entities with a nested transformation into another nested entity" in {
    val result = transformer.execute(
      task = transformTask,
      inputs = Seq(nestedEntityTable),
      outputSchema = Some(nestedTransformSpec.outputSchemaOpt.get),
      execution = LocalExecution(useLocalInternalDatasets = false),
      context = mock[ActivityContext[ExecutionReport]]
    )
    result mustBe defined
    val entityTable = result.get
    val entities = entityTable.entities.toSeq
    entities.size mustBe 2
    entities.head mustBe a[NestedEntity]
    for((entity, expectedEntity) <- entities.zip(expectedTargetEntities)) {
      validateNestedEntities(expectedEntity, entity.asInstanceOf[NestedEntity])
    }
  }
}