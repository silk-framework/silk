package org.silkframework.execution

import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.{Instant, OffsetDateTime, ZoneOffset}

import org.silkframework.runtime.activity.ActivityContext

/**
  * Base trait for simple execution report updates.
  */
trait ExecutionReportUpdater {

  final val DEFAULT_DELAY_BETWEEN_UPDATES = 1000
  /** The label of the task as it will be displayed in the execution report. */
  def taskLabel: String
  /** The activity context of the task that is executed */
  def context: ActivityContext[ExecutionReport]
  /** What does the task emit, e.g. "entity", "query" etc. */
  def entityLabelSingle: String = "Entity"
  /** The plural of the entity label, e.g. "entities", "queries" */
  def entityLabelPlural: String = "Entities"
  /** The verb that applies to the entity, depending on what task is executed. */
  def entityProcessVerb: String = "processed"
  /** The minimum delay between report updates */
  def delayBetweenUpdates: Int = DEFAULT_DELAY_BETWEEN_UPDATES

  private val start = System.currentTimeMillis()
  private var startFirstEntity: Option[Long] = None
  private var lastUpdate = 0L
  private var entitiesEmitted = 0

  def additionalFields(): Seq[(String, String)] = Seq.empty

  /** Increases the number of entities that were processed/generated. */
  def increaseEntityCounter(): Unit = {
    if(entitiesEmitted == 0) {
      startFirstEntity = Some(System.currentTimeMillis())
    }
    entitiesEmitted += 1
  }

  private val dateTimeFormatter: DateTimeFormatter = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd'T'HH:mm:ssZ").toFormatter

  protected def formatDateTime(timestamp: Long): String = {
    val instant = Instant.ofEpochMilli(timestamp)
    dateTimeFormatter.format(OffsetDateTime.ofInstant(instant, ZoneOffset.UTC))
  }

  // Runtime in ms
  final protected def runtime: Long = System.currentTimeMillis() - start

  // Throughput per second
  final protected def throughput: Double = 1000 * entitiesEmitted.toDouble / runtime

  /**
    * Updates the execution report. Does not update the report if the last update was not longer ago than updateDelay.
    *
    * @param force      Force the execution report update. This should only be done rarely.
    * @param addEndTime Adds an end time to the execution report. This should be done with the last update.
    */
  def update(force: Boolean = false, addEndTime: Boolean = false): Unit = {
    if (force || System.currentTimeMillis() - lastUpdate > delayBetweenUpdates) {
      val runtime = System.currentTimeMillis() - start
      val stats = Seq(
        "Started" -> formatDateTime(start),
        "Runtime" -> s"${runtime.toDouble / 1000} seconds",
        s"$entityLabelPlural / second" -> (if (runtime <= 0) "-" else throughput.formatted("%.3f")),
        s"Nr. of ${entityLabelPlural.toLowerCase} $entityProcessVerb" -> entitiesEmitted.toString
      ) ++
          Seq("Finished" -> formatDateTime(System.currentTimeMillis())).filter(_ => addEndTime) ++
          startFirstEntity.toSeq.map(firstEntityStart =>
            s"First ${entityLabelSingle.toLowerCase} $entityProcessVerb at" -> formatDateTime(firstEntityStart)) ++
          startFirstEntity.toSeq.map(firstEntityStart =>
            s"Runtime since first ${entityLabelSingle.toLowerCase} $entityProcessVerb" -> s"${(firstEntityStart - start).toDouble / 1000} seconds") ++
          additionalFields()
      context.value.update(SimpleExecutionReport(taskLabel, stats, None))
      lastUpdate = System.currentTimeMillis()
    }
  }
}