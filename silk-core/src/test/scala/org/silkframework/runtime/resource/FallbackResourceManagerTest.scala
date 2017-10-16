package org.silkframework.runtime.resource

import org.scalatest.{FlatSpec, MustMatchers}

/**
  * Tests the fallback resource manager
  */
class FallbackResourceManagerTest extends FlatSpec with MustMatchers {
  behavior of "Fallback resource manager"

  private final val RESOURCE = "Resource A"
  private final val CONTENT_PRIM = "Prim"
  private final val CONTENT_FB = "FB"
  private final val GARBAGE = "abc"

  private def getResourceManagers: (ResourceManager, ResourceManager) = {
    (InMemoryResourceManager(), InMemoryResourceManager())
  }

  it should "read from the primary manager if resource exists there" in {
    val (pr, fb) = getResourceManagers
    val resourceManager = FallbackResourceManager(pr, fb, writeIntoFallbackLoader = false)
    pr.get(RESOURCE).writeString(CONTENT_PRIM)
    fb.get(RESOURCE).writeString(CONTENT_FB)
    resourceManager.get(RESOURCE, mustExist = true).loadAsString mustBe CONTENT_PRIM
    resourceManager.get(RESOURCE, mustExist = false).loadAsString mustBe CONTENT_PRIM
  }

  it should "read from the fallback manager if resource exists only there" in {
    val (pr, fb) = getResourceManagers
    val resourceManager = FallbackResourceManager(pr, fb, writeIntoFallbackLoader = false)
    fb.get(RESOURCE).writeString(CONTENT_FB)
    resourceManager.get(RESOURCE, mustExist = true).loadAsString mustBe CONTENT_FB
    resourceManager.get(RESOURCE, mustExist = false).loadAsString mustBe CONTENT_FB
  }

  it should "write to the primary manager even if resource exists in fallback manager" in {
    val (pr, fb) = getResourceManagers
    val resourceManager = FallbackResourceManager(pr, fb, writeIntoFallbackLoader = false)
    fb.get(RESOURCE).writeString(GARBAGE)
    resourceManager.get(RESOURCE, mustExist = false).writeString(CONTENT_PRIM)
    pr.get(RESOURCE).loadAsString mustBe CONTENT_PRIM
    fb.get(RESOURCE).loadAsString mustBe GARBAGE
    // Delete and write with mustExists = true
    pr.get(RESOURCE).delete()
    fb.get(RESOURCE).delete()
    intercept[ResourceNotFoundException] {
      resourceManager.get(RESOURCE, mustExist = true)
    }
  }

  it should "write to the primary manager if resource exists nowhere" in {
    for(writeIntoFallback <- Seq(true, false)) {
      val (pr, fb) = getResourceManagers
      val resourceManager = FallbackResourceManager(pr, fb, writeIntoFallbackLoader = writeIntoFallback)
      resourceManager.get(RESOURCE, mustExist = false).writeString(CONTENT_PRIM)
      pr.get(RESOURCE).loadAsString mustBe CONTENT_PRIM
      fb.get(RESOURCE).loadAsString mustBe ""
    }
  }

  it should "write to the fallback manager if resource exists only in fallback manager and writeIntoFallbackLoader is enabled" in {
    val (pr, fb) = getResourceManagers
    val resourceManager = FallbackResourceManager(pr, fb, writeIntoFallbackLoader = true)
    fb.get(RESOURCE).writeString(GARBAGE)
    resourceManager.get(RESOURCE, mustExist = false).writeString(CONTENT_PRIM)
    pr.get(RESOURCE).loadAsString mustBe ""
    fb.get(RESOURCE).loadAsString mustBe CONTENT_PRIM
  }
}
