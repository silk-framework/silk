package controllers.util

import controllers.util.SerializationUtilsTest._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.rule.input.PathInput
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.serialization.json.JsonFormat
import org.silkframework.workspace.{Project, ProjectConfig}
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import play.api.mvc.{AnyContent, AnyContentAsJson, Request}

/**
  *
  */
class SerializationUtilsTest extends FlatSpec with MustMatchers with MockitoSugar {
  behavior of "Serialization Utils"

  private val APPLICATION_JSON = "application/json"
  private val TEXT_PLAIN = "text/plain"
  private val WILDCARD = "*/*"

  val testObject = new TestSubSubClass()
  implicit val project: Project = mock[Project]
  when(project.config).thenReturn(ProjectConfig())
  val request: Request[AnyContent] = mock[Request[AnyContent]]
  when(request.acceptedTypes).thenReturn(List())
  when(request.mediaType).thenReturn(None)
  implicit private val readContext = ReadContext()

  it must "serialize the object with the compile type of the object" in {
    implicit val r = request
    // No formatter available
    typ(SerializationUtils.serializeToStringCompileType(testObject, Seq(APPLICATION_JSON))) mustBe None
    // Set class tag explicitly
    typ(SerializationUtils.serializeToStringCompileType[TestTrait](testObject, Seq(APPLICATION_JSON))) mustBe Some(TEST_TRAIT)
    // Change compile time type of object
    typ(SerializationUtils.serializeToStringCompileType(testObject.asInstanceOf[TestTrait], Seq(APPLICATION_JSON))) mustBe Some(TEST_TRAIT)
    // Set class tag explicitly
    typ(SerializationUtils.serializeToStringCompileType[TestSubClass](testObject, Seq(APPLICATION_JSON))) mustBe Some(SUB_TEST_CLASS)
    // Change compile time type of object
    typ(SerializationUtils.serializeToStringCompileType(testObject.asInstanceOf[TestSubClass], Seq(APPLICATION_JSON))) mustBe Some(SUB_TEST_CLASS)
  }

  it must "serialize the object with the runtime type of the object" in {
    implicit val r = request
    typ(SerializationUtils.serializeToStringRuntimeType(testObject, Seq(APPLICATION_JSON))) mustBe None
    val testTraitImpl = new TestTrait {}
    // Only exact runtime type works, so subclasses of TestTrait cannot be serialized
    typ(SerializationUtils.serializeToStringRuntimeType(testTraitImpl, Seq(APPLICATION_JSON))) mustBe None
    val testSubClass = new TestSubClass()
    typ(SerializationUtils.serializeToStringRuntimeType(testSubClass, Seq(APPLICATION_JSON))) mustBe Some(SUB_TEST_CLASS)
  }

  it must "serialize to plain text, if no more specific serialization format is available and the MIME type includes text/plain" in {
    implicit val r = request
    val objectWithoutFormatter = new ClassWithoutFormatter
    SerializationUtils.serializeToStringCompileType(objectWithoutFormatter, Seq(APPLICATION_JSON)) mustBe None
    SerializationUtils.serializeToStringCompileType(objectWithoutFormatter, Seq(TEXT_PLAIN)) mustBe Some("ClassWithoutFormatter")
    SerializationUtils.serializeToStringCompileType[ClassWithoutFormatter](objectWithoutFormatter, Seq(WILDCARD)) mustBe Some("ClassWithoutFormatter")
    SerializationUtils.serializeToStringRuntimeType(objectWithoutFormatter, Seq(TEXT_PLAIN, WILDCARD)) mustBe Some("ClassWithoutFormatter")
  }

  private def typ(jsString: Option[String]): Option[String] = {
    jsString map {str => (Json.parse(str) \ "type").as[String]}
  }

  it must "deserialize the object with the given compile type" in {
    implicit val r = mock[Request[AnyContent]]
    when(r.mediaType).thenReturn(None)
    val json = SerializationUtils.serializeToStringCompileType[TestTrait](testObject, Seq(APPLICATION_JSON))
    when(r.body).thenReturn(AnyContentAsJson(Json.parse(json.get)))
    val result = SerializationUtils.deserializeCompileTime[TestTrait]("application/json") { requestBodyObj =>
      requestBodyObj.m mustBe "SubSubTestClass"
      null
    }
    result mustBe null
  }

  it must "return a BadRequestException if there are validation errors during the parsing" in {
    implicit val r = mock[Request[AnyContent]]
    when(r.mediaType).thenReturn(None)
    val json = """{"type":"pathInput","id":"id","path":"/unknown:path"}"""
    when(r.body).thenReturn(AnyContentAsJson(Json.parse(json)))
    intercept[BadUserInputException] {
      SerializationUtils.deserializeCompileTime[PathInput]("application/json") { requestBodyObj =>
        null
      }
    }
  }
}

object SerializationUtilsTest {
  final val TEST_TRAIT = "TestTrait"
  final val SUB_TEST_CLASS = "SubTestClass"
}

trait TestTrait {
  def m: String = TEST_TRAIT
}

class TestSubClass() extends TestTrait {
  override def m: String = SUB_TEST_CLASS
}

class TestSubSubClass() extends TestSubClass {
  override def m: String = "SubSubTestClass"
}

class ClassWithoutFormatter extends TestTrait {
  override def m: String = "ClassWithoutFormatter"
  override def toString = "ClassWithoutFormatter"
}

class TestTraitFormatter extends JsonFormat[TestTrait] {
  override def read(value: JsValue)(implicit readContext: ReadContext): TestTrait = {
    new TestSubSubClass
  }

  override def write(value: TestTrait)(implicit writeContext: WriteContext[JsValue]): JsValue = {
    JsObject(
      Seq(
        "type" -> JsString(TEST_TRAIT),
        "name" -> JsString(value.m)
      )
    )
  }
}

class TestSubClassFormatter extends JsonFormat[TestSubClass] {
  override def read(value: JsValue)(implicit readContext: ReadContext): TestSubClass = ???
  override def write(value: TestSubClass)(implicit writeContext: WriteContext[JsValue]): JsValue = {
    JsObject(
      Seq(
        "type" -> JsString(SUB_TEST_CLASS),
        "name" -> JsString(value.m)
      )
    )
  }
}
