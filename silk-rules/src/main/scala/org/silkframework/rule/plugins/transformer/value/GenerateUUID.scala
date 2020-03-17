package org.silkframework.rule.plugins.transformer.value

import java.nio.charset.Charset
import java.util.UUID

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.{Plugin, TransformExample, TransformExamples}
import GenerateUUID._

@Plugin(
  id = "generateUUID",
  categories = Array("Value"),
  label = "Generate UUID",
  description = "Generates UUIDs from input values."
)
@TransformExamples(Array(
  new TransformExample(
    input1 = Array("input value"),
    output = Array("cee963a2-8f70-3e97-b51a-85ef732e66dd")
  )
))
case class GenerateUUID() extends Transformer {

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    values.flatten.map(generateUUID)
  }

  @inline
  private def generateUUID(str: String): String = {
    UUID.nameUUIDFromBytes(str.getBytes(charset)).toString
  }

}

object GenerateUUID {

  private val charset: Charset = Charset.forName("UTF8")

}
