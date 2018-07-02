package org.silkframework.serialization.json.metadata

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.entity.metadata.EntityMetadata
import org.silkframework.entity.{Entity, EntitySchema, Path, Restriction}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.JsonFormat
import org.silkframework.util.{DPair, Uri}
import org.silkframework.serialization.json.JsonHelpers._
import play.api.libs.json.{JsObject, JsString, JsValue}

class JsonMetadataTest extends FlatSpec with Matchers {
  val schema = EntitySchema(typeUri = Uri(""), typedPaths = IndexedSeq(Path("path1").asStringTypedPath, Path("path2").asStringTypedPath), filter = Restriction.empty)

  implicit val throwableTag = classOf[Throwable]
  implicit val dpairTag = classOf[DPair[String]]
  implicit val Tag = classOf[LazyMetadataJson[DPair[String]]]

  val testException = new Exception("test", new IllegalArgumentException("some cause"))
  val metadata = new EntityMetadataJson(
    Map(EntityMetadata.FAILURE_KEY -> LazyMetadataJson(testException, ExceptionSerializerJson()))
  )

  val TestSerializerCategoryName = "pair_test_metadata"

  val entity1 = Entity("http://silk-framework.com/example", IndexedSeq(Seq("value1", "value2"), Seq("value3")), schema, IndexedSeq.empty, metadata)
  val entity2 = Entity("http://silk-framework.com/example", IndexedSeq(Seq("dÃ©clarÃ©s", "v2"), Seq("v3")), schema)

  val serializer = new JsonMetadataSerializer[DPair[String]] {
    /**
      * The identifier used to define metadata objects in the map of [[EntityMetadata]]
      */
    override def metadataId: String = TestSerializerCategoryName

    override def read(value: JsValue)(implicit readContext: ReadContext): DPair[String] = {
      val source = stringValue(value, "Source")
      val target = stringValue(value, "Target")
      new DPair[String](source, target)
    }

    override def write(value: DPair[String])(implicit writeContext: WriteContext[JsValue]): JsValue = {
      JsObject(Seq(
        "Source" -> JsString(value.source),
        "Target" -> JsString(value.target)
      ))
    }
  }

  it should "accept exception object as metadata and recognize it as a failed entity" in{
    val lazyMetadata = metadata.getLazyMetadata[Throwable](EntityMetadata.FAILURE_KEY)
    // test if metadata is serialized as single string (no pretty printing is important for the internal metadata representation)
    lazyMetadata.toString.contains(System.getProperty("line.separator")) shouldBe false
    //create a copy deleting the initial exception (keeping only its serialized version, so that the call for lm.metadata triggers the parsing of the serialized data)
    val serialized = lazyMetadata.serialized
    val copy = LazyMetadataJson(serialized, ExceptionSerializerJson())
    //now compare any aspect of the origin exception and the parsed one
    copy.metadata.isDefined shouldBe true
    copy.metadata.get.getClass.getCanonicalName shouldBe testException.getClass.getCanonicalName
    copy.metadata.get.getMessage shouldBe testException.getMessage
    copy.metadata.get.getStackTrace.length shouldBe testException.getStackTrace.length
    copy.metadata.get.getCause should not be  null.asInstanceOf[Throwable]
    copy.metadata.get.getCause.getClass shouldBe testException.getCause.getClass
    copy.metadata.get.getCause.getMessage shouldBe testException.getCause.getMessage
    //finally, since this is an exception provided with the FAILURE_KEY, the containing entity should recognize the failure and signal its failed state
    entity1.hasFailed shouldBe true
  }

  it should "be able to create an arbitrary metadata object and handle it normally" in{
    val pair =  new DPair("s", "t")
    val metadata = new EntityMetadataJson(Map(serializer.metadataId -> LazyMetadataJson(pair, serializer)))
    val entity = entity2.copy(metadata = metadata)

    val lazyMetadata = entity.metadata.getLazyMetadata[DPair[String]](serializer.metadataId)
    lazyMetadata.serialized.toString.replaceAll("\\s+", "").contains("\"Target\":\"t\"") shouldBe true
    lazyMetadata.metadata.get shouldEqual pair
  }

  it should "serialize the container EntityMetadata and parse it lazily" in{
    val newMetadata = new EntityMetadataJson(
      Map(
        EntityMetadata.FAILURE_KEY -> LazyMetadataJson(testException, ExceptionSerializerJson()),
        TestSerializerCategoryName -> LazyMetadataJson(new DPair("s", "t"), serializer)
      )
    )
    val stringVers = newMetadata.serializer.toString(newMetadata, JsonFormat.MIME_TYPE_APPLICATION)(WriteContext[JsValue]())
    val readMetadata = newMetadata.serializer.fromString(stringVers, JsonFormat.MIME_TYPE_APPLICATION)(ReadContext())
    val ex = readMetadata.getLazyMetadata[Throwable](EntityMetadata.FAILURE_KEY).metadata.get
    val pair = readMetadata.getLazyMetadata[DPair[String]](TestSerializerCategoryName).metadata.get
    ex shouldBe testException
  }
}
