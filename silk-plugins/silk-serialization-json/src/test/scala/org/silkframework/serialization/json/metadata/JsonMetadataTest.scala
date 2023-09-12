package org.silkframework.serialization.json.metadata


import org.silkframework.{config, dataset}
import org.silkframework.dataset.EmptyDataset
import org.silkframework.entity.metadata.{EntityMetadata, GenericExecutionFailure}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.{Entity, EntitySchema, Restriction}
import org.silkframework.failures.{EntityException, FailureClass}
import org.silkframework.runtime.serialization.{ReadContext, TestReadContext, TestWriteContext, WriteContext}
import org.silkframework.serialization.json.JsonFormat
import org.silkframework.util.{DPair, Uri}
import org.silkframework.serialization.json.JsonHelpers._
import play.api.libs.json.{JsObject, JsString, JsValue}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class JsonMetadataTest extends AnyFlatSpec with Matchers {
  val schema = EntitySchema(typeUri = Uri(""), typedPaths = IndexedSeq(UntypedPath("path1").asStringTypedPath, UntypedPath("path2").asStringTypedPath), filter = Restriction.empty)

  implicit val throwableTag: Class[FailureClass] = classOf[FailureClass]
  implicit val dpairTag: Class[DPair[String]] = classOf[DPair[String]]
  implicit val Tag: Class[LazyMetadataJson[DPair[String]]] = classOf[LazyMetadataJson[DPair[String]]]

  val alibiTask = config.PlainTask("alibi", dataset.DatasetSpec(EmptyDataset))
  val testException = new EntityException("", new Exception("test", new IllegalArgumentException("some cause")), alibiTask.id)
  val metadata = new EntityMetadataJson(
    Map(EntityMetadata.FAILURE_KEY -> LazyMetadataJson(testException.failureClass, FailureClassSerializerJson()))
  )

  val TestSerializerCategoryName = "pair_test_metadata"

  val entity1 = Entity("http://silk-framework.com/example", IndexedSeq(Seq("value1", "value2"), Seq("value3")), schema, metadata)
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

    override def replaceableMetadata: Boolean = true
  }

  def compareExceptions(ex1: GenericExecutionFailure, ex2: GenericExecutionFailure): Unit = {
    ex1.getMessage shouldBe ex2.getMessage
    ex1.getStackTrace.length shouldBe ex2.getStackTrace.length
    (ex1.cause, ex2.cause) match {
      case (Some(cause1), Some(cause2)) =>
        compareExceptions(cause1, cause2)
      case (None, None) =>
      case _ =>
        fail("Exceptions have different causes.")
    }
  }

  it should "accept exception object as metadata and recognize it as a failed entity" in{
    val lazyMetadata = metadata.getLazyMetadata[FailureClass](EntityMetadata.FAILURE_KEY)
    // test if metadata is serialized as single string (no pretty printing is important for the internal metadata representation)
    lazyMetadata.toString.contains(System.getProperty("line.separator")) shouldBe false
    //create a copy deleting the initial exception (keeping only its serialized version, so that the call for lm.metadata triggers the parsing of the serialized data)
    val serialized = lazyMetadata.serialized
    val copy = LazyMetadataJson(serialized, FailureClassSerializerJson())
    //now compare any aspect of the origin exception and the parsed one
    copy.metadata.isDefined shouldBe true
    compareExceptions(copy.metadata.get.rootCause, testException.rootCause)
    //finally, since this is an exception provided with the FAILURE_KEY, the containing entity should recognize the failure and signal its failed state
    entity1.hasFailed shouldBe true
  }

  it should "be able to create an arbitrary metadata object and handle it normally" in{
    val pair =  new DPair("s", "t")
    val metadata = new EntityMetadataJson(Map(serializer.metadataId -> LazyMetadataJson(pair, serializer)))
    val entity = entity2.adapt(metadata = metadata)

    val lazyMetadata = entity.metadata.getLazyMetadata[DPair[String]](serializer.metadataId)
    lazyMetadata.serialized.toString.replaceAll("\\s+", "").contains("\"Target\":\"t\"") shouldBe true
    lazyMetadata.metadata.get shouldEqual pair
  }

  it should "serialize the container EntityMetadata and parse it lazily" in{
    val newMetadata = new EntityMetadataJson(
      Map(
        EntityMetadata.FAILURE_KEY -> LazyMetadataJson(FailureClass(GenericExecutionFailure(testException), alibiTask.id), FailureClassSerializerJson()),
        TestSerializerCategoryName -> LazyMetadataJson(new DPair("s", "t"), serializer)
      )
    )
    val stringVers = newMetadata.serializer.toString(newMetadata, JsonFormat.MIME_TYPE_APPLICATION)(TestWriteContext[JsValue]())
    val readMetadata = newMetadata.serializer.fromString(stringVers, JsonFormat.MIME_TYPE_APPLICATION)(TestReadContext())
    val ex = readMetadata.getLazyMetadata[FailureClass](EntityMetadata.FAILURE_KEY).metadata.get
    val pair = readMetadata.getLazyMetadata[DPair[String]](TestSerializerCategoryName).metadata.get
    pair.source + pair.target shouldEqual "st"    //test the DPair
    compareExceptions(ex.rootCause, testException.rootCause)          //compare both exceptions
  }

  it should "deal correctly with empty metadata objects" in{
    val emptyMap = EntityMetadataJson()
    val stringVal = EntityMetadataJson.JsonSerializer.toString(emptyMap, "")(TestWriteContext[JsValue]())
    stringVal shouldBe ""
    val parsed = EntityMetadataJson.JsonSerializer.fromString(stringVal, "")(TestReadContext())
    parsed shouldBe EntityMetadataJson()
  }
}
