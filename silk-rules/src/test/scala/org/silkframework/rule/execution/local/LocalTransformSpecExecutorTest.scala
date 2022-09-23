package org.silkframework.rule.execution.local

import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.config.{PlainTask, Prefixes}
import org.silkframework.entity._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.execution.{ExecutorOutput, ExecutorRegistry}
import org.silkframework.execution.local.{GenericEntityTable, LocalExecution, MultiEntityTable}
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
    val plainMappingTarget = MappingTarget("urn:prop:label", ValueType.STRING)
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
      TypedPath(UntypedPath("urn:prop:label"), ValueType.STRING, isAttribute = false)
    )
    result.get.entities.map(_.values) mustBe Seq(IndexedSeq(Seq("A"), Seq("B", "D"), Seq("C", "E")))
  }

  it should "output the correct entity schemas and entities" in {
    val executor = new LocalTransformSpecExecutor()
    val transformTask = PlainTask("transform", TransformSpec(
      DatasetSelection.empty,
      RootMappingRule(MappingRules(
        typeRules = Seq(TypeMapping("type", "TypeRoot")),
        propertyRules = Seq(
          DirectMapping(id = "sp1", sourcePath = UntypedPath("pathA"), mappingTarget = MappingTarget("propA")),
          ObjectMapping("object", rules = MappingRules(
            typeRules = Seq(TypeMapping("type2", "TypeObject")),
            propertyRules = Seq(
              DirectMapping(id = "sp2", sourcePath = UntypedPath("pathB"), mappingTarget = MappingTarget("propB"))
            )
          ))
        )
      ))
    ))
    val rootEntitySchema = EntitySchema("es", IndexedSeq(UntypedPath("pathA")).map(_.asStringTypedPath))
    val objectEntitySchema = EntitySchema("es", IndexedSeq(UntypedPath("pathB")).map(_.asStringTypedPath))
    val rootEntities = Seq(Entity("uri1", IndexedSeq(Seq("A")), rootEntitySchema))
    val objectEntities = Seq(Entity("uri1", IndexedSeq(Seq("B")), objectEntitySchema))
    val multiEntitySchema = MultiEntityTable(rootEntities, rootEntitySchema, transformTask, Seq(GenericEntityTable(objectEntities, objectEntitySchema, transformTask)))
    val rootEntitiesResult = executor.execute(transformTask, Seq(multiEntitySchema), ExecutorOutput.empty, LocalExecution(true)).get
    val subTables = rootEntitiesResult.asInstanceOf[MultiEntityTable].subTables
    subTables must have size 1
    val objectEntitiesResult = subTables.head
    // Check entities
    objectEntitiesResult.entitySchema.typeUri.toString mustBe "TypeObject"
    objectEntitiesResult.entities.flatMap(e => e.values).flatten mustBe Seq("B")
    val objectEntityUri = objectEntitiesResult.entities.head.uri
    rootEntitiesResult.entitySchema.typeUri.toString mustBe "TypeRoot"
    rootEntitiesResult.entities.flatMap(e => e.values).flatten mustBe Seq("A", objectEntityUri.toString)
    val rootEntityUri = rootEntitiesResult.entities.head.uri
    rootEntityUri must not be objectEntityUri

  }
}