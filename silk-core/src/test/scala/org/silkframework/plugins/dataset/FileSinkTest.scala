package org.silkframework.plugins.dataset

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.InMemoryResourceManager

class FileSinkTest extends AnyFlatSpec with Matchers {

  behavior of "FileSink"

  implicit val userContext: UserContext = UserContext.Empty

  it should "only delete the file on clear if forced" in {
    val resource = InMemoryResourceManager().get("file")
    resource.writeString("existing")
    val sink = new FileSink(resource)

    sink.clear()
    resource.exists shouldBe true

    sink.clear(force = true)
    resource.exists shouldBe false
  }

}
