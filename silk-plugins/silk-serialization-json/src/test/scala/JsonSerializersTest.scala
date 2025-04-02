
import org.silkframework.dataset._
import org.silkframework.entity.ValueType
import org.silkframework.rule.vocab._
import org.silkframework.rule.{MappingTarget, NodePosition, RuleLayout}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.serialization.{ReadContext, Serialization, TestReadContext, TestWriteContext, WriteContext}
import org.silkframework.serialization.json.JsonSerializers._
import org.silkframework.serialization.json.{JsonFormat, JsonSerialization}
import org.silkframework.workspace.activity.transform.VocabularyCacheValue
import org.silkframework.serialization.json.WorkflowSerializers._
import org.silkframework.workspace.activity.workflow.WorkflowTest.{DS_A1, OUTPUT, testWorkflow}
import org.silkframework.workspace.annotation.{StickyNote, UiAnnotations}
import play.api.libs.json.Json

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
