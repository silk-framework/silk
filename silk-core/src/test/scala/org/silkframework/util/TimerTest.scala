package org.silkframework.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TimerTest extends AnyFlatSpec with Matchers {

  behavior of "Timer.formatDuration"

  it should "use the unit that fits the duration" in {
    Timer.formatDuration(46) shouldBe "46ms"
    Timer.formatDuration(4567) shouldBe "4.567s"
    Timer.formatDuration(1760329) shouldBe "29min 20s"
    Timer.formatDuration(7234100) shouldBe "2h 0min"
  }
}
