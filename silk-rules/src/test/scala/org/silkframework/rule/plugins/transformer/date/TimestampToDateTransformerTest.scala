package org.silkframework.rule.plugins.transformer.date

import org.silkframework.rule.test.TransformerTest

import java.util.concurrent.{ConcurrentLinkedQueue, CyclicBarrier}
import scala.util.control.NonFatal

class TimestampToDateTransformerTest extends TransformerTest[TimestampToDateTransformer] {

  it should "format correctly when evaluated from multiple threads" in {
    val transformer = TimestampToDateTransformer(format = "yyyy-MM-dd")
    val inputs = IndexedSeq("1499040000000", "0")
    // Expected outputs are computed single-threaded, so the test does not depend on the server timezone.
    val expected = inputs.map(input => input -> transformer.evaluate(input)).toMap
    val barrier = new CyclicBarrier(8)
    val errors = new ConcurrentLinkedQueue[String]()
    val threads = for(t <- 0 until 8) yield new Thread(() => {
      val input = inputs(t % 2)
      barrier.await()
      for(_ <- 1 to 50000) {
        val output = try transformer.evaluate(input) catch { case NonFatal(ex) => ex.toString }
        if(output != expected(input)) {
          errors.add(s"$input -> $output")
        }
      }
    })
    threads.foreach(_.start())
    threads.foreach(_.join())
    errors shouldBe empty
  }
}
