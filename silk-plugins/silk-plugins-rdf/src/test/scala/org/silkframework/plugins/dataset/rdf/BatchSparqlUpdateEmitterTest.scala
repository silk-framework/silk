package org.silkframework.plugins.dataset.rdf

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.config.PlainTask
import org.silkframework.plugins.dataset.rdf.executors.{BatchSparqlUpdateEmitter, SparqlUpdateExecutionReportUpdater}
import org.silkframework.util.TestMocks

import scala.collection.mutable.ArrayBuffer

class BatchSparqlUpdateEmitterTest extends AnyFlatSpec with Matchers {
  behavior of "BatchSparqlUpdateEmitter"

  private def newEmitter(batchSize: Int, onEmit: String => Unit): BatchSparqlUpdateEmitter[Unit] = {
    val reportUpdater = SparqlUpdateExecutionReportUpdater(PlainTask("t", new DummyTaskSpec(Map.empty)), TestMocks.activityContextMock())
    BatchSparqlUpdateEmitter(onEmit, batchSize, reportUpdater)
  }

  it should "leave an already-terminated query unchanged when batching two entries" in {
    val emitted = ArrayBuffer[String]()
    val emitter = newEmitter(batchSize = 2, emitted.append(_))
    emitter.update("INSERT DATA { <a> <b> <c> } ;")
    emitter.update("INSERT DATA { <d> <e> <f> } ;")
    emitted.head mustBe "INSERT DATA { <a> <b> <c> } ;\nINSERT DATA { <d> <e> <f> } ;"
  }

  it should "strip trailing whitespace after an existing semicolon rather than appending a second one" in {
    val emitted = ArrayBuffer[String]()
    val emitter = newEmitter(batchSize = 1, emitted.append(_))
    emitter.update("INSERT DATA { <a> <b> <c> } ;   \n")
    emitted.head mustBe "INSERT DATA { <a> <b> <c> } ;"
  }

  it should "append a semicolon when none is present, after trimming trailing whitespace" in {
    val emitted = ArrayBuffer[String]()
    val emitter = newEmitter(batchSize = 1, emitted.append(_))
    emitter.update("INSERT DATA { <a> <b> <c> }   ")
    emitted.head mustBe "INSERT DATA { <a> <b> <c> };"
  }

  it should "leave an empty query empty rather than turning it into a bare semicolon" in {
    val emitted = ArrayBuffer[String]()
    val emitter = newEmitter(batchSize = 1, emitted.append(_))
    emitter.update("")
    emitted.head mustBe ""
  }

  it should "leave a whitespace-only query empty rather than turning it into a bare semicolon" in {
    val emitted = ArrayBuffer[String]()
    val emitter = newEmitter(batchSize = 1, emitted.append(_))
    emitter.update("   \n")
    emitted.head mustBe ""
  }
}
