
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.{PlainTask, Task, TaskSpec}
import org.silkframework.dataset._
import org.silkframework.entity.ValueType
import org.silkframework.rule.input.TransformInput
import org.silkframework.rule.plugins.transformer.value.ConstantTransformer
import org.silkframework.rule.vocab._
import org.silkframework.rule._
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{ClassPluginDescription, PluginRegistry}
import org.silkframework.runtime.serialization._
import org.silkframework.runtime.templating.{SimpleSubstitutionTemplateEngine, TemplateVariable, VariableScope, TemplateVariables}
import org.silkframework.runtime.validation.TaskValidationException
import org.silkframework.util.ConfigTestTrait
import org.silkframework.execution.report.{EntitySample, SampleEntities, SampleEntitiesSchema}
import org.silkframework.rule.execution.TransformReport
import org.silkframework.rule.execution.TransformReport.{RuleError, RuleResult}
import org.silkframework.serialization.json.ExecutionReportSerializers.{ExecutionReportJsonFormat, TransformReportJsonFormat, WorkflowExecutionReportJsonFormat}
import org.silkframework.serialization.json.ReportDetail
import org.silkframework.serialization.json.JsonSerializers._
import org.silkframework.serialization.json.WorkflowSerializers._
import org.silkframework.serialization.json.{JsonFormat, JsonSerialization}
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.transform.VocabularyCacheValue
import org.silkframework.serialization.json.WorkflowSerializers._
import org.silkframework.execution.{OperationType, SimpleExecutionReport}
import org.silkframework.workspace.activity.workflow.{WorkflowExecutionReport, WorkflowTaskReport, WorkflowTest}
import org.silkframework.workspace.activity.workflow.WorkflowTest.{DS_A1, OUTPUT, testWorkflow}
import org.silkframework.workspace.activity.workflow.WorkflowTest.{DS_A1, OUTPUT, testWorkflow}
import org.silkframework.workspace.activity.workflow.{WorkflowExecutionReport, WorkflowTest}
import org.silkframework.workspace.annotation.{StickyNote, UiAnnotations}
import play.api.libs.json.{JsObject, Json}

import scala.reflect.ClassTag

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

  it should "round-trip workflow-level warnings" in {
    val report = WorkflowExecutionReport(
      task = PlainTask("workflowReport", WorkflowTest.testWorkflow),
      workflowWarnings = Seq("Dataset 'output' is cleared without a defined execution order.")
    )

    val reportJson = JsonSerialization.toJson(report)
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

    val slimJson = WorkflowExecutionReportJsonFormat.write(parentReport, ReportDetail.Compact)

    val childNode = (slimJson \ "taskReports")(0)
    (childNode \ "nodeId").as[String] shouldBe "childWf"
    // The nested workflow's own per-node reports must survive the compact form.
    val leafNode = (childNode \ "taskReports")(0)
    (leafNode \ "nodeId").as[String] shouldBe "leaf"
    (leafNode \ "entityCount").as[Int] shouldBe 5
    // Compact form must not embed the full task definition at any level.
    Json.stringify(slimJson) should not include "\"parameters\""
  }

  it should "omit auth diagnostics in the slim form unless the run failed" in {
    implicit val jsonWriteContext: WriteContext[play.api.libs.json.JsValue] =
      TestWriteContext[play.api.libs.json.JsValue]()
    val report = WorkflowExecutionReport(
      task = PlainTask("workflowReport", WorkflowTest.testWorkflow),
      authDiagnostics = Some("""{"scope":"read"}"""))

    (WorkflowExecutionReportJsonFormat.write(report, ReportDetail.Compact) \ "authDiagnostics").toOption shouldBe empty
    (WorkflowExecutionReportJsonFormat.write(report.copy(error = Some("auth failed")), ReportDetail.Compact) \ "authDiagnostics").isDefined shouldBe true
    (WorkflowExecutionReportJsonFormat.write(report, ReportDetail.Full) \ "authDiagnostics").isDefined shouldBe true
  }

  it should "keep only node reports with errors or warnings at the IssueNodesOnly detail level" in {
    implicit val jsonWriteContext: WriteContext[play.api.libs.json.JsValue] =
      TestWriteContext[play.api.libs.json.JsValue]()
    val task = PlainTask("wf", WorkflowTest.testWorkflow)
    def node(nodeId: String, report: org.silkframework.execution.ExecutionReport) = WorkflowTaskReport(nodeId, report)
    // A nested workflow whose only child failed with an error (no warnings) must survive the filter.
    val nestedWithFailingChild = WorkflowExecutionReport(task,
      taskReports = IndexedSeq(node("innerFailing", SimpleExecutionReport(task, error = Some("inner boom"), isDone = true))), isDone = true)
    val nestedAllClean = WorkflowExecutionReport(task,
      taskReports = IndexedSeq(node("innerClean", SimpleExecutionReport(task, isDone = true))), isDone = true)
    val report = WorkflowExecutionReport(task, taskReports = IndexedSeq(
      node("clean", SimpleExecutionReport(task, isDone = true)),
      node("failing", SimpleExecutionReport(task, error = Some("boom"), isDone = true)),
      node("warning", SimpleExecutionReport(task, warnings = Seq("something happened"), isDone = true)),
      node("nestedWithFailingChild", nestedWithFailingChild),
      node("nestedAllClean", nestedAllClean)), isDone = true)

    val json = WorkflowExecutionReportJsonFormat.write(report, ReportDetail.IssueNodesOnly)
    val keptNodes = (json \ "taskReports").as[play.api.libs.json.JsArray].value.map(n => (n \ "nodeId").as[String])
    keptNodes shouldBe Seq("failing", "warning", "nestedWithFailingChild")
    // The surviving nested node recursively keeps only its failing child, and no samples anywhere.
    (((json \ "taskReports")(2) \ "taskReports")(0) \ "nodeId").as[String] shouldBe "innerFailing"
    Json.stringify(json) should not include "outputEntitiesSample"
  }

  "TransformReport (slim)" should "keep rule error messages but drop their stacktraces" in {
    implicit val jsonWriteContext: WriteContext[play.api.libs.json.JsValue] =
      TestWriteContext[play.api.libs.json.JsValue]()
    val report = TransformReport(
      task = PlainTask("transform", TransformSpec.empty),
      entityCount = 5,
      entityErrorCount = 1,
      ruleResults = Map(Identifier("rule1") -> RuleResult(
        errorCount = 1,
        sampleErrors = IndexedSeq(RuleError("urn:entity:1", Seq(Seq("some value")), "not a valid Integer", None, Some(new RuntimeException("boom")))))),
      isDone = true)

    val slimJson = Json.stringify(TransformReportJsonFormat.write(report, ReportDetail.Compact))
    slimJson should include ("not a valid Integer")
    slimJson should not include "stacktrace"
    Json.stringify(TransformReportJsonFormat.write(report, ReportDetail.Full)) should include ("stacktrace")
  }

  "ExecutionReport (slim)" should "drop output samples of dataset read reports and truncate long sample values" in {
    implicit val jsonWriteContext: WriteContext[play.api.libs.json.JsValue] =
      TestWriteContext[play.api.libs.json.JsValue]()
    val longValue = "x" * 500
    val samples = Seq(SampleEntities(Seq(EntitySample("urn:entity:1", IndexedSeq(Seq(longValue)))), SampleEntitiesSchema.empty))
    def report(operationType: OperationType) = SimpleExecutionReport(
      task = PlainTask("someTask", WorkflowTest.testWorkflow),
      isDone = true, entityCount = 5, operation = Some(operationType.id),
      sampleOutputEntities = samples, operationType = operationType)

    // Samples of read reports echo the read source entities -> dropped in the slim form.
    val readJson = ExecutionReportJsonFormat.serializeBasicValues(report(OperationType.Read), ReportDetail.Compact)
    (readJson \ "outputEntitiesSample").toOption shouldBe empty
    // Other reports keep their samples, but values are truncated to 200 chars + ellipsis.
    val writeJson = ExecutionReportJsonFormat.serializeBasicValues(report(OperationType.Write), ReportDetail.Compact)
    val slimValue = (((writeJson \ "outputEntitiesSample")(0) \ "entities")(0) \ "values")(0)(0).as[String]
    slimValue shouldBe ("x" * 200 + "…")
    // The sample-free level drops them entirely.
    val noSamplesJson = ExecutionReportJsonFormat.serializeBasicValues(report(OperationType.Write), ReportDetail.CompactWithoutSamples)
    (noSamplesJson \ "outputEntitiesSample").toOption shouldBe empty
    // The verbose report keeps the full value.
    val fullJson = ExecutionReportJsonFormat.serializeBasicValues(report(OperationType.Write))
    ((((fullJson \ "outputEntitiesSample")(0) \ "entities")(0) \ "values")(0)(0)).as[String] shouldBe longValue
  }

  it should "round-trip the operation type through the verbose format" in {
    implicit val jsonWriteContext: WriteContext[play.api.libs.json.JsValue] =
      TestWriteContext[play.api.libs.json.JsValue]()
    def report(operationType: OperationType) = SimpleExecutionReport(
      task = PlainTask("someTask", WorkflowTest.testWorkflow),
      isDone = true, entityCount = 5, operation = Some(operationType.id), operationType = operationType)

    for(operationType <- OperationType.values) {
      val fullJson = ExecutionReportJsonFormat.write(report(operationType))
      (fullJson \ "operationType").as[String] shouldBe operationType.id
      ExecutionReportJsonFormat.read(fullJson).operationType shouldBe operationType
    }
    // Reports without the field (e.g. persisted before its introduction) default to Process.
    val legacyJson = ExecutionReportJsonFormat.write(report(OperationType.Read)) - "operationType"
    ExecutionReportJsonFormat.read(legacyJson).operationType shouldBe OperationType.Process
  }

  "TaskJsonFormat" should "resolve parameter templates against the task's own execution variables" in {
    PluginRegistry.unregisterPlugin(classOf[SomeDatasetPlugin])
    PluginRegistry.registerPlugin(classOf[SomeDatasetPlugin])
    val pluginId = ClassPluginDescription(classOf[SomeDatasetPlugin]).id.toString
    val executionVariables = TemplateVariables(Seq(
      TemplateVariable("param1Value", "valueFromVariable", None, None, isSensitive = false, VariableScope.execution)))
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
      TemplateVariable("myVar", "some value", None, None, isSensitive = false, VariableScope.execution)))

    val datasetTask = DatasetTask("datasetTask", new DatasetSpec(SomeDatasetPlugin("stringValue", 6.0)), executionVariables = executionVariables)
    JsonSerialization.fromJson[DatasetTask](JsonSerialization.toJson(datasetTask)).executionVariables shouldBe executionVariables

    val transformTask = TransformTask("transformTask", TransformSpec.empty, executionVariables = executionVariables)
    JsonSerialization.fromJson[TransformTask](JsonSerialization.toJson(transformTask)).executionVariables shouldBe executionVariables
  }

  "RuleBlockSpec" should "be serializable to and from JSON via TaskSpec dispatch" in {
    val ruleBlockSpec: TaskSpec = RuleBlockSpec(
      RuleBlockModel(
        ports = IndexedSeq(
          RuleBlockPort(
            id = "firstInput",
            label = "First input",
            description = "Used in the main branch.",
            displayOrder = 0
          ),
          RuleBlockPort(
            id = "secondInput",
            label = "Second input",
            description = "Deprecated fallback.",
            displayOrder = 1,
            deprecated = true
          )
        ),
        inputExamples = IndexedSeq(
          RuleBlockInputExample(
            id = "example-1",
            label = Some("Primary example"),
            inputs = Map(
              Identifier("firstInput") -> Seq("value 1", "value 2"),
              Identifier("secondInput") -> Seq("fallback")
            )
          )
        ),
        operator = Some(TransformInput(id = "rootTransform", transformer = ConstantTransformer("constant result"))),
        layout = RuleLayout(Map("rootTransform" -> NodePosition(10, 20, Some(120), Some(60)))),
        uiAnnotations = UiAnnotations(Seq(stickyNote))
      )
    )

    testSerialization(ruleBlockSpec)
  }

  it should "reject rule block ports without displayOrder in JSON" in {
    val json =
      Json.obj(
        TASKTYPE -> TASK_TYPE_RULE_BLOCK,
        PARAMETERS -> Json.obj(
          "ruleBlockModel" -> Json.obj(
            "ports" -> Json.arr(
              Json.obj(
                ID -> "missingOrderPort",
                "label" -> "Missing order"
              )
            )
          )
        )
      )

    val ex = the[TaskValidationException] thrownBy {
      JsonSerialization.fromJson[RuleBlockSpec](json)
    }
    ex.getMessage should include("missing required field 'displayOrder'")
    ex.getMessage should include("missingOrderPort")
  }

  it should "reject rule block ports without label in JSON" in {
    val json =
      Json.obj(
        TASKTYPE -> TASK_TYPE_RULE_BLOCK,
        PARAMETERS -> Json.obj(
          "ruleBlockModel" -> Json.obj(
            "ports" -> Json.arr(
              Json.obj(
                ID -> "missingLabelPort",
                "displayOrder" -> 1
              )
            )
          )
        )
      )

    val ex = the[TaskValidationException] thrownBy {
      JsonSerialization.fromJson[RuleBlockSpec](json)
    }
    ex.getMessage should include("missing required field 'label'")
    ex.getMessage should include("missingLabelPort")
  }

  def testSerialization[T](obj: T)(implicit format: JsonFormat[T]): Unit = {
    val objJson = JsonSerialization.toJson(obj)
    val objRoundTrip = JsonSerialization.fromJson[T](objJson)
    obj shouldBe objRoundTrip
  }
}

case class SomeDatasetPlugin(param1: String, param2: Double) extends Dataset {
  override def characteristics: DatasetCharacteristics = DatasetCharacteristics.attributesOnly()
}
