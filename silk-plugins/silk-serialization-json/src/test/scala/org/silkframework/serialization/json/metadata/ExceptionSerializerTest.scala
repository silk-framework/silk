package org.silkframework.serialization.json.metadata

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.entity.metadata.{ExceptionSerializer, ExecutionFailure}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}

class ExceptionSerializerTest extends FlatSpec with Matchers {

  "Exception Serializer" should "not fail when exceptions miss a string constructor or have an empty cause" in {
    val cau = new Throwable("cause")
    val ex1 = NoStringConstructorThrowable(cau) // see CMEM-1472, lead to trouble with some of Sparks exceptions
    val ex2 = UnknownCauseException("no known cause") // has been observed to lead to to NPE in situations will lead to emulated exception
    val throwable1 = serializeThrowable(ex1)
    val throwable2 = serializeThrowable(ex2)
    throwable1.getMessage shouldBe "NoStringConstructor Test Message"
    throwable2.getMessage shouldBe "The exception message was empty or incorrectly de/serialized. The origin class was: UnknownCauseException"
  }

  "Exception Serializer" should "not fail when an exception contains null values as messages" in {
    val exceptionWithNulls: Throwable = NullMessageException("With String constructor, but no 'message'")
    val throwable = serializeThrowable(exceptionWithNulls)
    throwable.getMessage shouldBe "The exception message was empty or incorrectly de/serialized. The origin class was: NullMessageException"
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
    </Exception>)(ReadContext())
    throwable.getMessage shouldBe message
  }

  "ExceptionSerializerJson" should "not fail when exceptions miss a string constructor or have an empty cause" in {
    val cau = new Throwable("cause")
    val ex1 = NoStringConstructorThrowable(cau) // see CMEM-1472, lead to trouble with some of Sparks exceptions
    val ex2 = UnknownCauseException("no known cause") // has been observed to lead to to NPE in situations
    val throwable1 = serializeThrowableJson(ex1)
    val throwable2 = serializeThrowableJson(ex2)
    throwable1.getMessage shouldBe "NoStringConstructor Test Message"
    throwable2.getMessage shouldBe "The exception message was empty or incorrectly de/serialized. The origin class was: UnknownCauseException"
  }

  "ExceptionSerializerJson" should "not fail when an contain null values as messages" in {
    // Specifically test for Json, XML handles that differently
    val exceptionWithNulls: Throwable = NullMessageException("With String constructor, but no 'message'")
    val throwable = serializeThrowableJson(exceptionWithNulls)
    throwable.getMessage shouldBe "The exception message was empty or incorrectly de/serialized. The origin class was: NullMessageException"
  }

  "Exception SerializerJson" should "handle exceptions with edge case constructors" in {
    val edgeCase = new EdgeCaseException1("msg", "no comment", null)
    val throwable = serializeThrowableJson(edgeCase)
    throwable.getMessage shouldBe "msg"
  }

  /* Helper methods that serialize and deserialize a Throwable and return it. */
  def serializeThrowable(exception: Throwable): ExecutionFailure = {
    val serializer = new ExceptionSerializer
    try {
      val nde = serializer.write(ExecutionFailure.fromThrowable(exception))(WriteContext())
      val res = serializer.read(nde)(ReadContext())
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

  def serializeThrowableJson(exception: Throwable): ExecutionFailure = {
    val serializer = new ExceptionSerializerJson
    try {
      val nde = serializer.write(ExecutionFailure.fromThrowable(exception))(WriteContext())
      val res = serializer.read(nde)(ReadContext())
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
class EdgeCaseException1(msg: String, comment: String, cause: Throwable) extends RuntimeException(msg, cause) {
  def this(msg: String, cause: Throwable) = this(msg, "no comment", cause)
}

case class NoStringConstructorThrowable(ex: Throwable) extends Throwable {
  override def getMessage: String = "NoStringConstructor Test Message"
}

case class NullMessageException(s: String) extends Throwable {
  override def getMessage: String = null
}

case class UnknownCauseException(message: String) extends Throwable {
  override def getCause: Throwable = UnknownCause(this.message)
}

case class UnknownCause(message: String) extends Throwable {
  override def getMessage: String = "UnknownCause Test Message"
  override def getCause: UnknownCause = null
}
