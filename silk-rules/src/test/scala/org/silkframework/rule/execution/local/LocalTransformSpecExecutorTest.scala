package org.silkframework.rule.execution.local

import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.config.{PlainTask, Prefixes}
import org.silkframework.entity._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.execution.{ExecutorOutput, ExecutorRegistry}
import org.silkframework.execution.local.{GenericEntityTable, LocalExecution}
import org.silkframework.rule._
import org.silkframework.runtime.activity.TestUserContextTrait

class LocalTransformSpecExecutorTest extends FlatSpec with MustMatchers with ExecutorRegistry with MockitoSugar with TestUserContextTrait {

  private implicit val prefixes: Prefixes = Prefixes.empty

  behavior of "Local Transform Specification Executor"

  it should "load from the registry" in {
    executor(TransformSpec(null, null), LocalExecution(false)).getClass mustBe classOf[LocalTransformSpecExecutor]
  }

  private val enVT = LanguageValueType("en")
  private val deVT = LanguageValueType("de")

  it should "group transformed results correctly" in {
    val executor = new LocalTransformSpecExecutor()
    val enMappingTarget = MappingTarget("urn:prop:label", enVT)
    val deMappingTarget = MappingTarget("urn:prop:label", deVT)
    val plainMappingTarget = MappingTarget("urn:prop:label", StringValueType)
    val transformTask = PlainTask("transform", TransformSpec(
      DatasetSelection.empty,
      RootMappingRule(MappingRules(
        propertyRules = Seq(
          DirectMapping(id= "sp1", sourcePath = UntypedPath("pathA"), mappingTarget = enMappingTarget),
          DirectMapping(id= "sp2", sourcePath = UntypedPath("pathB"), mappingTarget = deMappingTarget),
          DirectMapping(id= "sp3", sourcePath = UntypedPath("pathD"), mappingTarget = deMappingTarget),
          DirectMapping(id= "sp4", sourcePath = UntypedPath("pathC"), mappingTarget = plainMappingTarget),
          DirectMapping(id= "sp5", sourcePath = UntypedPath("pathE"), mappingTarget = plainMappingTarget)
        )
      ))
    ))
    val es = EntitySchema("es", IndexedSeq(UntypedPath("pathA"), UntypedPath("pathB"), UntypedPath("pathC"), UntypedPath("pathD"), UntypedPath("pathE")).map(_.asStringTypedPath))
    val entities = Seq(Entity("uri1", IndexedSeq(Seq("A"), Seq("B"), Seq("C"), Seq("D"), Seq("E")), es))
    val result = executor.execute(transformTask, Seq(GenericEntityTable(entities, es, transformTask)), ExecutorOutput.empty, LocalExecution(true))
    result.get.entitySchema.typedPaths mustBe IndexedSeq(
      TypedPath(UntypedPath("urn:prop:label"), enVT, isAttribute = false),
      TypedPath(UntypedPath("urn:prop:label"), deVT, isAttribute = false),
      TypedPath(UntypedPath("urn:prop:label"), StringValueType, isAttribute = false)
    )
    result.get.entities.map(_.values) mustBe Seq(IndexedSeq(Seq("A"), Seq("B", "D"), Seq("C", "E")))
  }
}