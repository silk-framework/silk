import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.dataset._
import org.silkframework.entity.UriValueType
import org.silkframework.rule.MappingTarget
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.serialization.{ReadContext, Serialization, WriteContext}

import scala.reflect.ClassTag

class JsonSerializersTest  extends FlatSpec with Matchers {

  "JsonDatasetTaskFormat" should "serialize JsonTaskFormats" in {
    PluginRegistry.registerPlugin(classOf[SomeDatasetPlugin])
    verify(new DatasetTask("taskId", SomeDatasetPlugin("stringValue", 6.0)))
  }

  private def verify[T: ClassTag](value: T) = {
    val mime = "application/json"
    implicit val readContext = ReadContext()
    implicit val writeContext = WriteContext[Any]()
    val format = Serialization.formatForMime[T](mime)
    val serialized = format.toString(value, mime)
    val deserialized = format.fromString(serialized, mime)
    value should be (deserialized)
  }

  "MappingTargetJsonFormat" should "serialize MappingTarget" in {
    val mappingTarget = MappingTarget("http://dot.com/prop", UriValueType, isBackwardProperty = true)
    verify(mappingTarget)
  }
}

case class SomeDatasetPlugin(param1: String, param2: Double) extends Dataset {
  override def source: DataSource = ???
  override def clear(): Unit = ???
  override def linkSink: LinkSink = ???
  override def entitySink: EntitySink = ???
}
