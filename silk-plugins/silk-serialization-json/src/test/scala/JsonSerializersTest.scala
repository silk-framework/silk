import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.dataset._
import org.silkframework.entity.ValueType
import org.silkframework.rule.MappingTarget
import org.silkframework.rule.vocab._
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.serialization.{ReadContext, Serialization, WriteContext}
import org.silkframework.serialization.json.JsonSerializers.{GenericInfoJsonFormat, VocabularyCacheValueJsonFormat, VocabularyJsonFormat}
import org.silkframework.workspace.activity.transform.VocabularyCacheValue
import play.api.libs.json.Json

import scala.reflect.ClassTag

class JsonSerializersTest  extends FlatSpec with Matchers {

  "JsonDatasetSpecFormat" should "serialize JsonTaskFormats" in {
    PluginRegistry.registerPlugin(classOf[SomeDatasetPlugin])
    verify(new DatasetSpec(SomeDatasetPlugin("stringValue", 6.0)))
  }

  val mime = "application/json"
  private implicit val readContext: ReadContext = ReadContext()
  private implicit val writeContext: WriteContext[Any] = WriteContext[Any](projectId = None)

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
    ))
    val json = Json.parse(toJsonString(vocabularyCacheValue))
    (json \ VocabularyCacheValueJsonFormat.VOCABULARIES \ VocabularyJsonFormat.CLASSES \\ GenericInfoJsonFormat.LABEL).
        headOption.map(_.as[String]) shouldBe vocClass.info.label
  }
}

case class SomeDatasetPlugin(param1: String, param2: Double) extends Dataset {
  override def source(implicit userContext: UserContext): DataSource = ???
  override def linkSink(implicit userContext: UserContext): LinkSink = ???
  override def entitySink(implicit userContext: UserContext): EntitySink = ???
  override def characteristics: DatasetCharacteristics = DatasetCharacteristics.attributesOnly
}
