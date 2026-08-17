package org.silkframework.execution

/**
  * The semantic type of the operation an execution report describes.
  * Complements the free-text display label [[ExecutionReport.operation]], which stays unconstrained.
  */
sealed abstract class OperationType(val id: String)

object OperationType {

  /** Reads entities from a dataset. Output samples of read reports echo the read source entities. */
  case object Read extends OperationType("read")

  /** Writes entities to a dataset. */
  case object Write extends OperationType("write")

  /** Any other operation (the default), e.g. transforming entities or generating queries. */
  case object Process extends OperationType("process")

  val values: Seq[OperationType] = Seq(Read, Write, Process)

  /** Resolves an id back to the operation type; unknown ids fall back to [[Process]]. */
  def fromId(id: String): OperationType = values.find(_.id == id).getOrElse(Process)
}
