package org.silkframework.serialization.json.metadata

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.entity.metadata.ExceptionSerializer
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}

class ExceptionSerializerTest extends FlatSpec with Matchers {

  it should "not fail when exceptions without a string constructor occur" in {
    val ser = new ExceptionSerializer()
    val exc = new NoConstructorException
    val nde = ser.write(exc)(WriteContext())

    try {
      ser.read(nde)(ReadContext())
    }
    catch {
      case _:Throwable => fail("The exception serializer failed due to an Throwable without String parameter constructor")
    }
  }
}

class NoConstructorException extends Throwable