package org.silkframework.rule.plugins.transformer.dataset

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.runtime.resource.InMemoryResourceManager

class FileHashTransformerTest extends AnyFlatSpec with Matchers {

  behavior of "FileHashTransformer"

  it should "generate the hash sum of the specified file" in {
    val resource = InMemoryResourceManager().get("test")
    resource.writeString("ABC")
    val transformer = FileHashTransformer(resource)
    transformer(Seq.empty) shouldBe Seq("3c01bdbb26f358bab27f267924aa2c9a03fcfdb8")
  }

}
