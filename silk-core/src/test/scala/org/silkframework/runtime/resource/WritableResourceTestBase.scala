package org.silkframework.runtime.resource

import org.scalatest.{FlatSpec, MustMatchers}

/**
  * Base trait for [[Resource]] tests.
  */
trait WritableResourceTestBase extends FlatSpec with MustMatchers {
  def freshResource(): WritableResource

  it should "allow writing multiple string via append" in {
    val resource = freshResource()
    var overallString = ""
    for(i <- 1 to 100) {
      val stringToWrite = s"Some • string $i, "
      overallString += stringToWrite
      resource.writeString(stringToWrite, append = true)
    }
    val roundTrip = resource.loadAsString
    roundTrip mustBe overallString
  }

  it should "overwrite the current value if append is false" in {
    val resource = freshResource()
    resource.writeString("Test")
    resource.loadAsString mustBe "Test"
    resource.writeString("Overwrite")
    resource.loadAsString mustBe "Overwrite"
  }
}
