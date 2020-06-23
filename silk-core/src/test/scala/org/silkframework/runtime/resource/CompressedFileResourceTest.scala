package org.silkframework.runtime.resource

import java.io.File

class CompressedFileResourceTest extends WritableResourceTestBase {
  behavior of "Compressed file resource"
  private val resourceName = "resourceName"

  it should "return meaningful values for all other API methods except modified data" in {
    val inputString = "test" * 100
    val resource = freshResource()
    resource.writeString(inputString)
    resource.size mustBe defined
    assert(resource.size.get > 0 && resource.size.get < inputString.length)
    resource.name mustBe resourceName
    resource.exists mustBe true
    resource.path mustBe resourceName
    resource.delete()
    resource.size mustBe Some(0)
    resource.exists mustBe false
    resource.loadAsString mustBe ""
  }

  override def freshResource(): WritableResource = {
    val file = File.createTempFile("CompressedFileResourceTest", "lz4")
    file.deleteOnExit()
    CompressedFileResource(file, resourceName, resourceName, IndexedSeq.empty, deleteOnGC = false)
  }
}