package org.silkframework.serialization.json.metadata


import org.silkframework.entity.metadata.{ExceptionSerializer, GenericExecutionFailure}
import org.silkframework.runtime.serialization.{ReadContext, TestReadContext, TestWriteContext, WriteContext}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ExceptionSerializerTest extends AnyFlatSpec with Matchers {

  "Exception Serializer" should "not fail when exceptions miss a string constructor or have an empty cause" in {
    val cau = new Throwable("cause")
    val ex1 = NoStringConstructorThrowable(cau) // see CMEM-1472, lead to trouble with some of Sparks exceptions
    val ex2 = UnknownCauseException("no known cause") // has been observed to lead to to NPE in situations will lead to emulated exception
    val throwable1 = serializeThrowable(ex1)
    val throwable2 = serializeThrowable(ex2)
    throwable1.getMessage shouldBe "NoStringConstructor Test Message"
    throwable1.cause.flatMap(_.message) shouldBe Some("cause")
    throwable2.cause.map(_.className) shouldBe Some("org.silkframework.serialization.json.metadata.UnknownCause")
  }

  "Exception Serializer" should "not fail when an exception contains null values as messages" in {
    val exceptionWithNulls: Throwable = NullMessageException("With String constructor, but no 'message'")
    val throwable = serializeThrowable(exceptionWithNulls)
    throwable.message shouldBe empty
  }

  "Exception Serializer" should "handle exceptions with edge case constructors" in {
    val edgeCase = new EdgeCaseException1("msg", "no comment", null)
    val throwable = serializeThrowable(edgeCase)
    throwable.getMessage shouldBe "msg"
  }

  // TODO Is this even possible, remove if not
  "Exception Serializer" should "deserialize an exception without a class name" in {
    val exceptionSerializer = new ExceptionSerializer
    val message = "Some message"
    val throwable = exceptionSerializer.read(<Exception>
      <Class></Class>
      <Message>{message}</Message>
      <Cause>
      </Cause>
      <StackTrace>
      </StackTrace>
    </Exception>)(TestReadContext())
    throwable.getMessage shouldBe message
  }

  "ExceptionSerializerJson" should "not fail when exceptions miss a string constructor or have an empty cause" in {
    val cau = new Throwable("cause")
    val ex1 = NoStringConstructorThrowable(cau) // see CMEM-1472, lead to trouble with some of Sparks exceptions
    val ex2 = UnknownCauseException("no known cause") // has been observed to lead to to NPE in situations
    val throwable1 = serializeThrowableJson(ex1)
    val throwable2 = serializeThrowableJson(ex2)
    throwable1.getMessage shouldBe "NoStringConstructor Test Message"
    throwable2.getMessage shouldBe "no known cause"
    throwable2.cause.map(_.getMessage) shouldBe Some("UnknownCause Test Message")
  }

  "ExceptionSerializerJson" should "not fail when an contain null values as messages" in {
    // Specifically test for Json, XML handles that differently
    val exceptionWithNulls: Throwable = NullMessageException("With String constructor, but no 'message'")
    val throwable = serializeThrowableJson(exceptionWithNulls)
    throwable.message shouldBe empty
  }

  "Exception SerializerJson" should "handle exceptions with edge case constructors" in {
    val edgeCase = new EdgeCaseException1("msg", "no comment", null)
    val throwable = serializeThrowableJson(edgeCase)
    throwable.getMessage shouldBe "msg"
  }

  /* Helper methods that serialize and deserialize a Throwable and return it. */
  def serializeThrowable(exception: Throwable): GenericExecutionFailure = {
    val serializer = new ExceptionSerializer
    try {
      val nde = serializer.write(GenericExecutionFailure(exception))(TestWriteContext())
      val res = serializer.read(nde)(TestReadContext())
      res
    }
    catch {
      case _: NoSuchMethodException =>
        fail(new Exception("Exception Serializer failed to de-/serialize an Exception without a String Constructor"))
      case _: NullPointerException =>
        fail(new Exception("Exception Serializer failed to de-/serialize an Exception with missing feature"))
      case t: Throwable =>
        fail(t)
    }
  }

  def serializeThrowableJson(exception: Throwable): GenericExecutionFailure = {
    val serializer = new ExceptionSerializerJson
    try {
      val nde = serializer.write(GenericExecutionFailure(exception))(TestWriteContext())
      val res = serializer.read(nde)(TestReadContext())
      res
    }
    catch {
      case _: NoSuchMethodException =>
        fail(new Exception("Exception Serializer failed to de-/serialize an Exception without a String Constructor"))
      case _: NullPointerException =>
        fail(new Exception("Exception Serializer failed to de-/serialize an Exception with missing feature"))
      case t: Throwable =>
        fail(t)
    }
  }

}


//* test objects //*
// TODO Is this something that can happen? I had more test exceptiojs here earlier but removed speculative cases
class EdgeCaseException1(msg: String, comment: String, cause: Throwable) extends RuntimeException(msg, cause) {
  def this(msg: String, cause: Throwable) = this(msg, "no comment", cause)
}

case class NoStringConstructorThrowable(ex: Throwable) extends Throwable(null, ex) {
  override def getMessage: String = "NoStringConstructor Test Message"
}

case class NullMessageException(s: String) extends Throwable {
  override def getMessage: String = null
}

case class UnknownCauseException(message: String) extends Throwable(message) {
  override def getCause: Throwable = UnknownCause(this.message)
}

case class UnknownCause(message: String) extends Throwable {
  override def getMessage: String = "UnknownCause Test Message"
  override def getCause: UnknownCause = null
}
