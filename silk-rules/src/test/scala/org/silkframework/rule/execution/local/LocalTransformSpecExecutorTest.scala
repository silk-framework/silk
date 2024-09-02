package org.silkframework.rule.execution.local


import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.config.PlainTask
import org.silkframework.dataset.{DatasetSpec, EmptyDataset}
import org.silkframework.entity._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.execution.local.{GenericEntityTable, LocalExecution, MultiEntityTable}
import org.silkframework.execution.{ExecutionReport, ExecutorOutput, ExecutorRegistry}
import org.silkframework.rule._
import org.silkframework.rule.input.{PathInput, TransformInput, Transformer}
import org.silkframework.runtime.activity.{ActivityContext, ActivityMonitor, TestUserContextTrait}
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.util.{Identifier, MockitoSugar}

class LocalTransformSpecExecutorTest extends AnyFlatSpec with Matchers with ExecutorRegistry with MockitoSugar with TestUserContextTrait {

  private implicit val pluginContext: PluginContext = PluginContext.empty

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
    result.get.entities.map(_.values).toSeq mustBe Seq(IndexedSeq(Seq("A"), Seq("B", "D"), Seq("C", "E")))
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
    val multiEntitySchema = MultiEntityTable(CloseableIterator(rootEntities.iterator), rootEntitySchema, transformTask, Seq(GenericEntityTable(objectEntities, objectEntitySchema, transformTask)))
    val context: ActivityContext[ExecutionReport] = new ActivityMonitor(getClass.getSimpleName)
    val rootEntitiesResult = executor.execute(transformTask, Seq(multiEntitySchema), ExecutorOutput.empty, LocalExecution(true), context).get
    val subTables = rootEntitiesResult.asInstanceOf[MultiEntityTable].subTables
    subTables must have size 1
    val objectEntitiesResult = subTables.head
    // Check entities
    objectEntitiesResult.entitySchema.typeUri.toString mustBe "TypeObject"
    val objectEntitiesSeq = objectEntitiesResult.entities.toSeq
    objectEntitiesSeq.flatMap(e => e.values).flatten mustBe Seq("B")
    val objectEntityUri = objectEntitiesSeq.head.uri
    rootEntitiesResult.entitySchema.typeUri.toString mustBe "TypeRoot"
    val rootEntitiesSeq = rootEntitiesResult.entities.toSeq
    rootEntitiesSeq.flatMap(e => e.values).flatten mustBe Seq("A", objectEntityUri.toString)
    val rootEntityUri = rootEntitiesSeq.head.uri
    rootEntityUri must not be objectEntityUri
    // Check report
    context.value().entityCount mustBe 2
  }

  it should "inject the task context into the transform rules" in {
    val path = UntypedPath("inputPath")
    val target = MappingTarget("outputPath")
    def transformInput: TransformInput = TransformInput(id = Identifier.random, transformer = TestTransformer(), inputs = IndexedSeq(PathInput(path = path)))

    val transformTask = PlainTask("transform", TransformSpec(
      DatasetSelection.empty,
      RootMappingRule(MappingRules(
        propertyRules = Seq(
          ComplexMapping(id = "m1", operator = transformInput, target = Some(target)),
          ObjectMapping("object", rules = MappingRules(
            propertyRules = Seq(
              ComplexMapping(id = "m2", operator = transformInput, target = Some(target))
            )
          ))
        )
      ))
    ))

    val entitySchema = EntitySchema("es", IndexedSeq(path).map(_.asStringTypedPath))
    def entity: CloseableIterator[Entity] = CloseableIterator.single(new Entity("entity", IndexedSeq(Seq("input value")), entitySchema))
    val executor = new LocalTransformSpecExecutor()
    val inputDatasetId = "inputDataset"
    val inputTask = PlainTask(inputDatasetId, DatasetSpec(EmptyDataset))
    val inputTable = MultiEntityTable(entity, entitySchema, inputTask, Seq(GenericEntityTable(entity, entitySchema, inputTask)))
    val context: ActivityContext[ExecutionReport] = new ActivityMonitor(getClass.getSimpleName)
    val result = executor.execute(transformTask, Seq(inputTable), ExecutorOutput.empty, LocalExecution(useLocalInternalDatasets = true), context).get.asInstanceOf[MultiEntityTable]

    // Check the result of the root rule
    result.headOption.map(_.values.head) mustBe Some(Seq(inputDatasetId))
    // Check the result of the object rule
    result.subTables.head.headOption.map(_.values.head) mustBe Some(Seq(inputDatasetId))
  }
}

case class TestTransformer() extends Transformer {

  override def withContext(taskContext: TaskContext): Transformer = {
    TransformerWithContext(taskContext)
  }

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    Seq.empty
  }

}

case class TransformerWithContext(taskContext: TaskContext) extends Transformer {

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    Seq(taskContext.inputTasks.head.id.toString)
  }

}