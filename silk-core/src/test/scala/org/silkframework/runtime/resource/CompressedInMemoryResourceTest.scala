package org.silkframework.runtime.resource

class CompressedInMemoryResourceTest extends WritableResourceTestBase {
  behavior of "Compressed in-memory resource"

  val resourceName = "resourceName"

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

  override def freshResource(): WritableResource = CompressedInMemoryResource(resourceName, resourceName, IndexedSeq.empty)
}
