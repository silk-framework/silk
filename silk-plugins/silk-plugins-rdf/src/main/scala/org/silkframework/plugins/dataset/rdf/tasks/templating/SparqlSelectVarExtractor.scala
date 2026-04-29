package org.silkframework.plugins.dataset.rdf.tasks.templating

/**
 * Best-effort heuristic that extracts the projected result variables from a SPARQL SELECT query.
 *
 * Unlike a full SPARQL parser, this works on raw template text that may still contain unevaluated
 * placeholders (Jinja, Velocity, ...). It is used where rendering-with-defaults would fail because
 * placeholders sit in contexts that do not accept a seed URI (string literals, numeric limits, etc.).
 *
 * Strategy:
 *
 *   1. Locate the first `SELECT` keyword (word-bounded, case-insensitive).
 *   2. Find the end of the projection clause: the first `WHERE` / `FROM` keyword or `{`.
 *   3. Strip a leading `DISTINCT` / `REDUCED`.
 *   4. If the projection is `*`, fall back to collecting every distinct `?var` token in the full query.
 *   5. Otherwise, walk the projection tracking parenthesis depth. At depth 0 collect `?var` directly.
 *      For each `( ... )` group, extract the alias of the last `AS ?alias` inside it.
 *
 * Returns an empty sequence when no match can be found (non-SELECT templates, or templates whose
 * projection itself is produced by a placeholder).
 */
object SparqlSelectVarExtractor {

  private val selectKeywordPattern = """(?i)\bSELECT\b""".r
  private val whereKeywordPattern = """(?i)\bWHERE\b""".r
  private val fromKeywordPattern = """(?i)\bFROM\b""".r
  private val distinctReducedPattern = """(?i)^(?:DISTINCT|REDUCED)\s+""".r
  private val anyVarPattern = """\?([A-Za-z_][A-Za-z0-9_]*)""".r
  private val asAliasPattern = """(?i)\bAS\s+\?([A-Za-z_][A-Za-z0-9_]*)""".r

  def extractSelectVars(query: String): Seq[String] = {
    selectKeywordPattern.findFirstMatchIn(query) match {
      case None => Seq.empty
      case Some(m) =>
        val afterSelect = query.substring(m.end)
        val boundary = projectionBoundary(afterSelect)
        val projection = distinctReducedPattern.replaceFirstIn(afterSelect.substring(0, boundary).trim, "")
        if (projection.trim == "*") {
          anyVarPattern.findAllMatchIn(query).map(_.group(1)).toSeq.distinct
        } else {
          extractProjectedVars(projection)
        }
    }
  }

  private def projectionBoundary(afterSelect: String): Int = {
    val candidates = Seq(
      whereKeywordPattern.findFirstMatchIn(afterSelect).map(_.start),
      fromKeywordPattern.findFirstMatchIn(afterSelect).map(_.start),
      Some(afterSelect.indexOf('{')).filter(_ >= 0)
    ).flatten
    if (candidates.isEmpty) afterSelect.length else candidates.min
  }

  private def extractProjectedVars(projection: String): Seq[String] = {
    val vars = scala.collection.mutable.ArrayBuffer.empty[String]
    var depth = 0
    var parenStart = 0
    var i = 0
    while (i < projection.length) {
      projection.charAt(i) match {
        case '(' =>
          if (depth == 0) parenStart = i
          depth += 1
          i += 1
        case ')' =>
          depth -= 1
          if (depth == 0) {
            val content = projection.substring(parenStart + 1, i)
            asAliasPattern.findAllMatchIn(content).toSeq.lastOption.foreach(m => vars += m.group(1))
          }
          i += 1
        case '?' if depth == 0 =>
          val start = i + 1
          var j = start
          while (j < projection.length && isVarChar(projection.charAt(j))) j += 1
          if (j > start) vars += projection.substring(start, j)
          i = j
        case _ =>
          i += 1
      }
    }
    vars.toSeq.distinct
  }

  private def isVarChar(c: Char): Boolean = c.isLetterOrDigit || c == '_'
}
