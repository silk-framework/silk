package org.silkframework.serialization.json

/**
  * The detail level of a serialized execution report — a linear ladder from the verbose form down to
  * the most compact one, so size-capped consumers can degrade a too-large report step by step.
  *
  * @param slim           Compact form: no embedded task definitions, timing or empty fields; rule errors
  *                       without stacktraces; auth diagnostics only for failed runs; sample values truncated.
  * @param includeSamples Whether output entity samples are serialized at all.
  * @param onlyIssueNodes Whether only node reports with an error or warnings are serialized.
  */
sealed abstract class ReportDetail(val slim: Boolean, val includeSamples: Boolean, val onlyIssueNodes: Boolean)

object ReportDetail {

  /** The verbose report: every node's full task definition, timing, stacktraces and samples. */
  case object Full extends ReportDetail(slim = false, includeSamples = true, onlyIssueNodes = false)

  /** The compact report: all nodes with counts, warnings/errors and truncated output samples. */
  case object Compact extends ReportDetail(slim = true, includeSamples = true, onlyIssueNodes = false)

  /** The compact report without any output entity samples. */
  case object CompactWithoutSamples extends ReportDetail(slim = true, includeSamples = false, onlyIssueNodes = false)

  /** Only the node reports that carry an error or warnings, without samples. */
  case object IssueNodesOnly extends ReportDetail(slim = true, includeSamples = false, onlyIssueNodes = true)
}
