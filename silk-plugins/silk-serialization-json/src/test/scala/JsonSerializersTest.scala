import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.dataset._
import org.silkframework.plugins.dataset.InternalDataset
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.serialization.Serialization

import scala.reflect.ClassTag

class JsonSerializersTest  extends FlatSpec with Matchers {

  "JsonDatasetTaskFormat" should "serialize JsonTaskFormats" in {
    PluginRegistry.registerPlugin(classOf[SomeDatasetPlugin])
    verify(new DatasetTask("taskId", SomeDatasetPlugin("stringValue", 6.0)))
  }

  private def verify[T: ClassTag](value: T) = {
    val mime = "application/json"
    val serialized = Serialization.serialize(value, mime)
    val deserialized = Serialization.deserialize[T](serialized, mime)
    value should be (deserialized)
  }
}

case class SomeDatasetPlugin(param1: String, param2: Double) extends Dataset {
  override def source: DataSource = ???
  override def clear(): Unit = ???
  override def linkSink: LinkSink = ???
  override def entitySink: EntitySink = ???
}
