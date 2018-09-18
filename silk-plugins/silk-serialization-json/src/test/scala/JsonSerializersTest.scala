import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.dataset._
import org.silkframework.entity.UriValueType
import org.silkframework.rule.MappingTarget
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.serialization.{ReadContext, Serialization, WriteContext}

import scala.reflect.ClassTag

class JsonSerializersTest  extends FlatSpec with Matchers {

  "JsonDatasetSpecFormat" should "serialize JsonTaskFormats" in {
    PluginRegistry.registerPlugin(classOf[SomeDatasetPlugin])
    verify(new DatasetSpec(SomeDatasetPlugin("stringValue", 6.0)))
  }

  private def verify[T: ClassTag](value: T) = {
    val mime = "application/json"
    implicit val readContext = ReadContext()
    implicit val writeContext = WriteContext[Any](projectId = None)
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
  override def source(implicit userContext: UserContext): DataSource = ???
  override def linkSink(implicit userContext: UserContext): LinkSink = ???
  override def entitySink(implicit userContext: UserContext): EntitySink = ???
}
