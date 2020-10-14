package org.silkframework.util

import java.time.temporal.ChronoUnit
import java.time.{Duration => JDuration}
import java.util.concurrent.TimeUnit

import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * This object contains methods that convert between Scala and Java duration types.
  *
  * This has been adapted from the Scala 2.13 code.
  * Can be removed as soon as we switch to Scala 2.13
  */
object DurationConverters {

  implicit class JavaDurationOps(private val duration: JDuration) extends AnyVal {

    /** Convert a Java duration to a Scala duration. If the nanosecond part of the Java duration is
      * zero, the returned duration will have a time unit of seconds. If there is a nanoseconds part,
      * the Scala duration will have a time unit of nanoseconds.
      *
      * @throws IllegalArgumentException If the given Java Duration is out of bounds of what can be
      *                                  expressed by [[scala.concurrent.duration.FiniteDuration]].
      */
    def toScala: Duration = {
      val originalSeconds = duration.getSeconds
      val originalNanos = duration.getNano
      if (originalNanos == 0) {
        if (originalSeconds == 0) Duration.Zero
        else FiniteDuration(originalSeconds, TimeUnit.SECONDS)
      } else if (originalSeconds == 0) {
        FiniteDuration(originalNanos, TimeUnit.NANOSECONDS)
      } else {
        try {
          val secondsAsNanos = Math.multiplyExact(originalSeconds, 1000000000)
          val totalNanos = secondsAsNanos + originalNanos
          if ((totalNanos < 0 && secondsAsNanos < 0) || (totalNanos > 0 && secondsAsNanos > 0))
            FiniteDuration(totalNanos, TimeUnit.NANOSECONDS)
          else
            throw new ArithmeticException()
        } catch {
          case _: ArithmeticException =>
            throw new IllegalArgumentException(s"Java duration $duration cannot be expressed as a Scala duration")
        }
      }
    }
  }


  implicit final class ScalaDurationOps(private val duration: FiniteDuration) extends AnyVal {

    /** Convert a Scala `FiniteDuration` to a Java duration. Note that the Scala duration keeps the
      * time unit it was created with, while a Java duration always is a pair of seconds and nanos,
      * so the unit it lost.
      */
    def toJava: JDuration = {
      if (duration.length == 0) JDuration.ZERO
      else duration.unit match {
        case TimeUnit.NANOSECONDS => JDuration.ofNanos(duration.length)
        case TimeUnit.MICROSECONDS => JDuration.of(duration.length, ChronoUnit.MICROS)
        case TimeUnit.MILLISECONDS => JDuration.ofMillis(duration.length)
        case TimeUnit.SECONDS => JDuration.ofSeconds(duration.length)
        case TimeUnit.MINUTES => JDuration.ofMinutes(duration.length)
        case TimeUnit.HOURS => JDuration.ofHours(duration.length)
        case TimeUnit.DAYS => JDuration.ofDays(duration.length)
      }
    }
  }
}