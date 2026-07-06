
import org.silkframework.config.{PlainTask, Task, TaskSpec}
import org.silkframework.dataset._
import org.silkframework.entity.ValueType
import org.silkframework.rule.vocab._
import org.silkframework.rule.{MappingTarget, NodePosition, RuleLayout, TransformSpec, TransformTask}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{ClassPluginDescription, PluginRegistry}
import org.silkframework.runtime.templating.{SimpleSubstitutionTemplateEngine, TemplateVariable, TemplateVariableScopes, TemplateVariables}
import org.silkframework.util.ConfigTestTrait
import org.silkframework.runtime.serialization.{ReadContext, Serialization, TestReadContext, TestWriteContext, WriteContext}
import org.silkframework.serialization.json.JsonSerializers._
import org.silkframework.serialization.json.{JsonFormat, JsonSerialization}
import org.silkframework.serialization.json.ExecutionReportSerializers.WorkflowExecutionReportJsonFormat
import org.silkframework.workspace.activity.transform.VocabularyCacheValue
import org.silkframework.serialization.json.WorkflowSerializers._
import org.silkframework.execution.SimpleExecutionReport
import org.silkframework.workspace.activity.workflow.{WorkflowExecutionReport, WorkflowTaskReport, WorkflowTest}
import org.silkframework.workspace.activity.workflow.WorkflowTest.{DS_A1, OUTPUT, testWorkflow}
import org.silkframework.workspace.annotation.{StickyNote, UiAnnotations}
import play.api.libs.json.{JsObject, Json}

import scala.reflect.ClassTag
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class JsonSerializersTest  extends AnyFlatSpec with Matchers with ConfigTestTrait {

  // Use the dependency-free substitution engine, so that parameter templates can be evaluated in this module's tests.
  override def propertyMap: Map[String, Option[String]] = Map(
    "config.variables.engine" -> Some(SimpleSubstitutionTemplateEngine.id)
  )

  "JsonDatasetSpecFormat" should "serialize JsonTaskFormats" in {
    PluginRegistry.registerPlugin(classOf[SomeDatasetPlugin])
    verify(new DatasetSpec(SomeDatasetPlugin("stringValue", 6.0)))
  }

  val mime = "application/json"
  private implicit val readContext: ReadContext = TestReadContext()
  private implicit val writeContext: WriteContext[Any] = TestWriteContext[Any]()

  private def verify[T: ClassTag](value: T) = {
    val format = Serialization.formatForMime[T](mime)
    val serialized = format.toString(value, mime)
    val deserialized = format.fromString(serialized, mime)
    value should be (deserialized)
  }

  private def toJsonString[T: ClassTag](value: T): String = {
    val format = Serialization.formatForMime[T](mime)
    format.toString(value, mime)
  }

  "MappingTargetJsonFormat" should "serialize MappingTarget" in {
    val mappingTarget = MappingTarget("http://dot.com/prop", ValueType.URI, isBackwardProperty = true)
    verify(mappingTarget)
  }

  "VocabularyCacheValue" should "be serializable to JSON" in {
    val vocClass = VocabularyClass(GenericInfo("http://class"), Seq("http://parentClass"))
    val vocabularyCacheValue = new VocabularyCacheValue(Seq(
      Vocabulary(
        GenericInfo("http://vocUri", Some("Voc label"), Some("Voc description"), Seq("voc alt label")),
        Seq(vocClass),
        Seq(VocabularyProperty(GenericInfo("http://property"), DatatypePropertyType, Some(vocClass), Some(vocClass)))
      )
    ), Some(System.currentTimeMillis()))
    val json = Json.parse(toJsonString(vocabularyCacheValue))
    (json \ VocabularyCacheValueJsonFormat.VOCABULARIES \ VocabularyJsonFormat.CLASSES \\ GenericInfoJsonFormat.LABEL).
        headOption.map(_.as[String]) shouldBe vocClass.info.label
  }

  "RuleLayout" should "be serializable to and from JSON" in {
    val layout = RuleLayout(
      Map(
        "nodeA" -> NodePosition(1, 2),
        "nodeB" -> NodePosition(3, 4, Some(10), None),
        "nodeC" -> NodePosition(5, 6, None, Some(10)),
        "nodeD" -> NodePosition(7, 8, Some(100), Some(200))
      )
    )
    testSerialization(layout)
  }

  private val stickyNote = StickyNote(
    "sticky ID",
    "content with\nnew\n\nlines",
    "#fff",
    NodePosition(3, 6, 20, 24)
  )

  "StickyNote" should "serialize to and from JSON" in {
    testSerialization(stickyNote)
  }

  "UiAnnotations" should "serialize to and from JSON" in {
    testSerialization(UiAnnotations(Seq(stickyNote, stickyNote.copy(id = "other Id"))))
  }

  "Workflows" should "serialize to and from JSON" in {
    val workflow = testWorkflow.copy(
      replaceableInputs = Seq(DS_A1),
      replaceableOutputs = Seq(OUTPUT)
    )
    testSerialization(workflow)
  }

  "WorkflowExecutionReport" should "serialize auth diagnostics as a JSON object" in {
    val authDiagnostics = Json.obj(
      "scope" -> "read",
      "refreshTokenPresent" -> true
    )
    val report = WorkflowExecutionReport(
      task = PlainTask("workflowReport", WorkflowTest.testWorkflow),
      authDiagnostics = Some(Json.stringify(authDiagnostics))
    )

    val reportJson = JsonSerialization.toJson(report)
    (reportJson \ "authDiagnostics").as[JsObject] shouldBe authDiagnostics

    val roundTrip = JsonSerialization.fromJson[WorkflowExecutionReport](reportJson)
    roundTrip shouldBe report
  }

  "WorkflowExecutionReport (slim)" should "recurse into nested-workflow sub-reports without embedding task definitions" in {
    implicit val jsonWriteContext: WriteContext[play.api.libs.json.JsValue] =
      TestWriteContext[play.api.libs.json.JsValue]()

    // Parent workflow -> nested child workflow -> one leaf node.
    val leafReport = SimpleExecutionReport(
      task = PlainTask("leaf", WorkflowTest.testWorkflow), entityCount = 5, isDone = true,
      operationDesc = "entities written")
    val nestedWorkflowReport = WorkflowExecutionReport(
      task = PlainTask("childWf", WorkflowTest.testWorkflow),
      taskReports = IndexedSeq(WorkflowTaskReport(nodeId = "leaf", report = leafReport)),
      isDone = true)
    val parentReport = WorkflowExecutionReport(
      task = PlainTask("parentWf", WorkflowTest.testWorkflow),
      taskReports = IndexedSeq(WorkflowTaskReport(nodeId = "childWf", report = nestedWorkflowReport)),
      isDone = true)

    val slimJson = WorkflowExecutionReportJsonFormat.write(parentReport, slim = true)

    val childNode = (slimJson \ "taskReports")(0)
    (childNode \ "nodeId").as[String] shouldBe "childWf"
    // The nested workflow's own per-node reports must survive the compact form.
    val leafNode = (childNode \ "taskReports")(0)
    (leafNode \ "nodeId").as[String] shouldBe "leaf"
    (leafNode \ "entityCount").as[Int] shouldBe 5
    // Compact form must not embed the full task definition at any level.
    Json.stringify(slimJson) should not include "\"parameters\""
  }

  "TaskJsonFormat" should "resolve parameter templates against the task's own execution variables" in {
    PluginRegistry.unregisterPlugin(classOf[SomeDatasetPlugin])
    PluginRegistry.registerPlugin(classOf[SomeDatasetPlugin])
    val pluginId = ClassPluginDescription(classOf[SomeDatasetPlugin]).id.toString
    val executionVariables = TemplateVariables(Seq(
      TemplateVariable("param1Value", "valueFromVariable", None, None, isSensitive = false, TemplateVariableScopes.execution)))
    val taskJson = Json.obj(
      "id" -> "taskWithExecutionVariables",
      "executionVariables" -> JsonSerialization.toJson(executionVariables),
      "data" -> Json.obj(
        "taskType" -> "Dataset",
        "type" -> pluginId,
        "parameters" -> Json.obj("param2" -> "6.0"),
        "templates" -> Json.obj("param1" -> "{{execution.param1Value}}")
      )
    )
    // The read context does not provide any execution variables — they must be seeded from the task payload itself.
    val task = JsonSerialization.fromJson[Task[TaskSpec]](taskJson)
    task.data.asInstanceOf[DatasetSpec[Dataset]].plugin.asInstanceOf[SomeDatasetPlugin].param1 shouldBe "valueFromVariable"
    task.executionVariables.variables.map(_.name) shouldBe Seq("param1Value")
  }

  "DatasetTaskJsonFormat and TransformTaskJsonFormat" should "preserve execution variables" in {
    PluginRegistry.unregisterPlugin(classOf[SomeDatasetPlugin])
    PluginRegistry.registerPlugin(classOf[SomeDatasetPlugin])
    val executionVariables = TemplateVariables(Seq(
      TemplateVariable("myVar", "some value", None, None, isSensitive = false, TemplateVariableScopes.execution)))

    val datasetTask = DatasetTask("datasetTask", new DatasetSpec(SomeDatasetPlugin("stringValue", 6.0)), executionVariables = executionVariables)
    JsonSerialization.fromJson[DatasetTask](JsonSerialization.toJson(datasetTask)).executionVariables shouldBe executionVariables

    val transformTask = TransformTask("transformTask", TransformSpec.empty, executionVariables = executionVariables)
    JsonSerialization.fromJson[TransformTask](JsonSerialization.toJson(transformTask)).executionVariables shouldBe executionVariables
  }

  def testSerialization[T](obj: T)(implicit format: JsonFormat[T]): Unit = {
    val objJson = JsonSerialization.toJson(obj)
    val objRoundTrip = JsonSerialization.fromJson[T](objJson)
    obj shouldBe objRoundTrip
  }
}

case class SomeDatasetPlugin(param1: String, param2: Double) extends Dataset {
  override def source(implicit userContext: UserContext): DataSource = ???
  override def linkSink(implicit userContext: UserContext): LinkSink = ???
  override def entitySink(implicit userContext: UserContext): EntitySink = ???
  override def characteristics: DatasetCharacteristics = DatasetCharacteristics.attributesOnly()
}
