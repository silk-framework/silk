package org.silkframework.runtime.metrics

import io.micrometer.core.instrument.Meter.Type
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.micrometer.core.instrument.distribution.pause.PauseDetector
import io.micrometer.core.instrument.noop._
import io.micrometer.core.instrument._

import java.lang
import java.util.concurrent.TimeUnit
import java.util.function.{ToDoubleFunction, ToLongFunction}
import scala.util.Random

class NoopMeterRegistry(clock: Clock = new MockClock()) extends MeterRegistry(clock) {
  private def meterId(typ: Type): Meter.Id = {
    def randomString: String = Random.alphanumeric.filter(_.isDigit).take(5).mkString
    new Meter.Id(randomString, Tags.empty(), randomString, randomString, typ)
  }

  override def newGauge[T](id: Meter.Id, obj: T, valueFunction: ToDoubleFunction[T]): Gauge =
    new NoopGauge(meterId(Type.GAUGE))

  override def newCounter(id: Meter.Id): Counter = new NoopCounter(meterId(Type.COUNTER))

  override def newTimer(id: Meter.Id,
                        distributionStatisticConfig: DistributionStatisticConfig,
                        pauseDetector: PauseDetector): Timer = new NoopTimer(meterId(Type.TIMER))

  override def newDistributionSummary(id: Meter.Id,
                                      distributionStatisticConfig: DistributionStatisticConfig,
                                      scale: Double): DistributionSummary =
    new NoopDistributionSummary(meterId(Type.DISTRIBUTION_SUMMARY))

  override def newMeter(id: Meter.Id,
                        `type`: Meter.Type,
                        measurements: lang.Iterable[Measurement]): Meter = new NoopMeter(meterId(`type`))

  override def newFunctionTimer[T](id: Meter.Id,
                                   obj: T,
                                   countFunction: ToLongFunction[T],
                                   totalTimeFunction: ToDoubleFunction[T],
                                   totalTimeFunctionUnit: TimeUnit): FunctionTimer =
    new NoopFunctionTimer(meterId(Type.TIMER))

  override def newFunctionCounter[T](id: Meter.Id,
                                     obj: T,
                                     countFunction: ToDoubleFunction[T]): FunctionCounter =
    new NoopFunctionCounter(meterId(Type.COUNTER))

  override def getBaseTimeUnit: TimeUnit = TimeUnit.SECONDS

  override def defaultHistogramConfig(): DistributionStatisticConfig = DistributionStatisticConfig.NONE
}
