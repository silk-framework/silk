
import org.silkframework.config.PlainTask
import org.silkframework.dataset._
import org.silkframework.config.TaskSpec
import org.silkframework.entity.ValueType
import org.silkframework.rule.vocab._
import org.silkframework.rule.input.TransformInput
import org.silkframework.rule.plugins.transformer.value.ConstantTransformer
import org.silkframework.rule.{MappingTarget, NodePosition, RuleBlockInputExample, RuleBlockModel, RuleBlockPort, RuleBlockSpec, RuleLayout}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.serialization.{ReadContext, Serialization, TestReadContext, TestWriteContext, WriteContext}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.serialization.json.JsonSerializers._
import org.silkframework.serialization.json.{JsonFormat, JsonSerialization}
import org.silkframework.serialization.json.ExecutionReportSerializers.WorkflowExecutionReportJsonFormat
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.transform.VocabularyCacheValue
import org.silkframework.serialization.json.WorkflowSerializers._
import org.silkframework.workspace.activity.workflow.{WorkflowExecutionReport, WorkflowTest}
import org.silkframework.workspace.activity.workflow.WorkflowTest.{DS_A1, OUTPUT, testWorkflow}
import org.silkframework.workspace.annotation.{StickyNote, UiAnnotations}
import play.api.libs.json.{JsObject, Json}

import scala.reflect.ClassTag
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class JsonSerializersTest  extends AnyFlatSpec with Matchers {

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
          "ports" -> Json.arr(
            Json.obj(
              ID -> "missingOrderPort",
              "label" -> "Missing order"
            )
          )
        )
      )

    val ex = the[ValidationException] thrownBy {
      JsonSerialization.fromJson[TaskSpec](json)
    }
    ex.getMessage should include("missing required field 'displayOrder'")
    ex.getMessage should include("missingOrderPort")
  }

  it should "reject rule block ports without label in JSON" in {
    val json =
      Json.obj(
        TASKTYPE -> TASK_TYPE_RULE_BLOCK,
        PARAMETERS -> Json.obj(
          "ports" -> Json.arr(
            Json.obj(
              ID -> "missingLabelPort",
              "displayOrder" -> 1
            )
          )
        )
      )

    val ex = the[ValidationException] thrownBy {
      JsonSerialization.fromJson[TaskSpec](json)
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
