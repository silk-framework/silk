package org.silkframework.rule.plugins.transformer.value

import java.util.UUID

import org.silkframework.rule.test.TransformerTest

class GenerateUUIDTest extends TransformerTest[GenerateUUID] {

  it should "generate random UUIDs if no input is provided" in {
    val generateUUID = GenerateUUID()
    val result = generateUUID(Seq.empty)

    result should have size 1
    noException should be thrownBy UUID.fromString(result.head) // will fail if UUID is not valid
  }

}
