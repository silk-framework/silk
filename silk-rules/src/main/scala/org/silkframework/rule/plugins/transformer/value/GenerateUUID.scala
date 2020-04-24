package org.silkframework.rule.plugins.transformer.value

import java.nio.charset.Charset
import java.util.UUID

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.{Plugin, TransformExample, TransformExamples}
import GenerateUUID._

@Plugin(
  id = "uuid",
  categories = Array("Value"),
  label = "UUID",
  description =
""" Generates UUIDs.
If no input value is provided, a random UUID (type 4) is generated using a cryptographically strong pseudo random number generator.
If input values are provided, a name-based UUID (type 3) is generated for each input value.
Each input value will generate a separate UUID. For building a UUID from multiple inputs, the Concatenate operator can be used.
"""
)
@TransformExamples(Array(
  new TransformExample(
    input1 = Array("input value"),
    output = Array("cee963a2-8f70-3e97-b51a-85ef732e66dd")
  ),
  new TransformExample(
    input1 = Array("üöä!", "êéè"),
    output = Array("690802dd-a317-335f-807c-e4e1e32b7b5b", "925cbd7f-377b-3fbd-8f4c-ca41529b74ad")
  )
))
case class GenerateUUID() extends Transformer {

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    if(values.isEmpty) {
      Seq(UUID.randomUUID.toString)
    } else {
      values.flatten.map(generateUUIDfromName)
    }
  }

  @inline
  private def generateUUIDfromName(str: String): String = {
    UUID.nameUUIDFromBytes(str.getBytes(charset)).toString
  }

}

object GenerateUUID {

  private val charset: Charset = Charset.forName("UTF8")

}
