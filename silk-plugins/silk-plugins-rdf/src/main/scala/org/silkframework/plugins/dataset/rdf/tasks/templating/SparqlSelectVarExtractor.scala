package org.silkframework.plugins.dataset.rdf.tasks.templating

import scala.collection.mutable.ArrayBuffer
import scala.util.matching.Regex

/**
 * Best-effort heuristic that extracts the projected result variables from a SPARQL SELECT query.
 *
 * Unlike a full SPARQL parser, this works on raw template text that may still contain unevaluated
 * Jinja placeholders. It is used where rendering-with-defaults would fail because
 * placeholders sit in contexts that do not accept a seed URI (string literals, numeric limits, etc.).
 *
 * Strategy:
 *
 *   1. Blank out everything that must not be mistaken for query structure: line comments and Jinja comments
 *      are dropped, the contents of string literals, IRIs and Jinja tags are removed (their delimiters are kept).
 *   2. Locate the first `SELECT` keyword that is not part of a name (case-insensitive). Give up if it is nested in
 *      a group (a sub-select of another query form) or preceded by a Jinja block tag, which may switch queries.
 *   3. Find the end of the projection clause: the first `WHERE` / `FROM` keyword or `{` outside of parentheses.
 *      A keyword that is part of a variable or prefixed name (`?from`, `ex:where`) is not a boundary.
 *   4. If the projection contains a Jinja tag, its variables cannot be known statically: return nothing.
 *   5. Strip a leading `DISTINCT` / `REDUCED`.
 *   6. If the projection is `*`, fall back to collecting every distinct `?var` token after the projection. Give up
 *      if a Jinja tag outside of an IRI or string literal, a sub-select, `EXISTS` or `MINUS` could bind variables
 *      that are not part of the result.
 *   7. Otherwise, walk the projection tracking parenthesis depth. At depth 0 collect `?var` directly.
 *      For each `( ... )` group, extract the alias of the last `AS ?alias` inside it. Give up on unbalanced parentheses.
 *
 * The heuristic need not be complete, but it must not be wrong: whenever it is in doubt it returns an empty sequence
 * (as it does for non-SELECT templates) and callers derive the schema from the query results at run time.
 */
object SparqlSelectVarExtractor {

  // Characters a variable or a prefixed name may contain. A keyword enclosed in them is part of a name
  // (?from, ex:select), not the keyword itself.
  private val nameChars = """\p{L}\p{Nd}_:.\-\x{B7}\x{300}-\x{36F}\x{203F}\x{2040}"""
  private def keyword(alternatives: String): String = raw"""(?i)(?<![$nameChars?$$])(?:$alternatives)(?![$nameChars])"""

  private val selectKeywordPattern = keyword("SELECT").r
  private val whereKeywordPattern = keyword("WHERE").r
  private val fromKeywordPattern = keyword("FROM").r
  // SPARQL does not require whitespace after a keyword, e.g. SELECT DISTINCT* or (COUNT(?x) AS?n)
  private val distinctReducedPattern = raw"""^${keyword("DISTINCT|REDUCED")}\s*""".r
  // Keywords that open a scope which may bind variables that are not part of a SELECT * result
  private val nestedScopePattern = keyword("SELECT|EXISTS|MINUS").r
  // SPARQL VARNAME: letters, digits and '_' plus a few combining characters
  private val varName = """[\p{L}\p{Nd}_][\p{L}\p{Nd}_\x{B7}\x{300}-\x{36F}\x{203F}\x{2040}]*"""
  private val anyVarPattern = raw"""[?$$]($varName)""".r
  private val asAliasPattern = raw"""${keyword("AS")}\s*[?$$]($varName)""".r

  def extractSelectVars(query: String): Seq[String] = {
    val cleaned = blankOpaqueRegions(query)
    selectKeywordPattern.findFirstMatchIn(cleaned) match {
      case Some(m) if isQueryForm(cleaned.substring(0, m.start)) =>
        val afterSelect = cleaned.substring(m.end)
        val boundary = projectionBoundary(afterSelect)
        // The boundary itself may be the start of a Jinja tag, which is not a projection we can read.
        if (isTagStart(afterSelect, boundary) || containsTag(afterSelect.substring(0, boundary))) {
          Seq.empty
        } else {
          val projection = distinctReducedPattern.replaceFirstIn(afterSelect.substring(0, boundary).trim, "")
          if (projection == "*") {
            allVars(afterSelect)
          } else {
            extractProjectedVars(projection)
          }
        }
      case _ => Seq.empty
    }
  }

  private def containsTag(text: String): Boolean = text.contains("{{") || text.contains("{%")

  /**
   * Whether the SELECT keyword preceded by `before` is the query form itself: it must not sit inside a group
   * (a sub-select of a CONSTRUCT, DESCRIBE or update query), and no Jinja block tag may precede it, since such a
   * tag may switch between alternative queries. Blanked tags are brace-balanced and do not affect the depth.
   */
  private def isQueryForm(before: String): Boolean = {
    !before.contains("{%") && before.count(_ == '{') == before.count(_ == '}')
  }

  /** All variables of a `SELECT *` query, unless a tag or a nested scope may bind variables that are not projected. */
  private def allVars(afterSelect: String): Seq[String] = {
    if (containsTag(afterSelect) || nestedScopePattern.findFirstIn(afterSelect).isDefined) {
      Seq.empty
    } else {
      anyVarPattern.findAllMatchIn(afterSelect).map(_.group(1)).toSeq.distinct
    }
  }

  private def projectionBoundary(afterSelect: String): Int = {
    val depth = parenDepths(afterSelect)
    def firstAtDepth0(pattern: Regex): Option[Int] = pattern.findAllMatchIn(afterSelect).map(_.start).find(depth(_) == 0)
    val brace = afterSelect.indices.find(i => afterSelect.charAt(i) == '{' && depth(i) == 0)
    (firstAtDepth0(whereKeywordPattern) ++ firstAtDepth0(fromKeywordPattern) ++ brace).minOption.getOrElse(afterSelect.length)
  }

  /** The parenthesis depth in front of each character. */
  private def parenDepths(text: String): Array[Int] = {
    val depths = new Array[Int](text.length)
    var depth = 0
    for (i <- text.indices) {
      depths(i) = depth
      text.charAt(i) match {
        case '(' => depth += 1
        case ')' => depth = math.max(0, depth - 1)
        case _ =>
      }
    }
    depths
  }

  private def extractProjectedVars(projection: String): Seq[String] = {
    val vars = ArrayBuffer.empty[String]
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
          if (depth == 0) return Seq.empty
          depth -= 1
          if (depth == 0) {
            val content = projection.substring(parenStart + 1, i)
            asAliasPattern.findAllMatchIn(content).toSeq.lastOption.foreach(m => vars += m.group(1))
          }
          i += 1
        case '?' | '$' if depth == 0 =>
          anyVarPattern.findPrefixMatchOf(projection.substring(i)) match {
            case Some(m) =>
              vars += m.group(1)
              i += m.end
            case None =>
              i += 1
          }
        case _ =>
          i += 1
      }
    }
    if (depth == 0) vars.toSeq.distinct else Seq.empty
  }

  /**
   * Drops line comments and Jinja comments and removes the contents of string literals, IRIs and Jinja tags,
   * keeping their delimiters, so that nothing inside them is mistaken for query structure.
   */
  private def blankOpaqueRegions(query: String): String = {
    val out = new StringBuilder(query.length)
    var i = 0
    while (i < query.length) {
      val c = query.charAt(i)
      if (query.startsWith("{#", i)) {
        i = skipPast(query, "#}", i + 2)
      } else if (isTagStart(query, i)) {
        out.append(query.substring(i, i + 2)).append(if (query.charAt(i + 1) == '{') "}}" else "%}")
        i = tagEnd(query, i)
      } else if (c == '\\') {
        // Escaped character of a prefixed name (ex:a\#b, dbr:Baldwin\'s_Peak)
        i += 2
      } else if (c == '#') {
        // Line comment. Jinja still evaluates tags inside it, so keep a marker of the same kind for them.
        val end = endOfLine(query, i)
        val comment = query.substring(i, end)
        if (comment.contains("{%")) out.append("{%%}")
        else if (comment.contains("{{")) out.append("{{}}")
        i = end
      } else if (c == '"' || c == '\'') {
        val quote = if (query.startsWith(c.toString * 3, i)) c.toString * 3 else c.toString
        out.append(quote)
        val end = stringEnd(query, i + quote.length, quote)
        if (end >= 0) {
          out.append(quote)
          i = end + quote.length
        } else {
          // Unterminated literal: skip the line for a single-quoted one, the rest of the query for a long one.
          i = if (quote.length == 1) endOfLine(query, i) else query.length
        }
      } else if (c == '<') {
        val end = iriEnd(query, i)
        if (end >= 0) {
          out.append("<>")
          i = end + 1
        } else {
          out.append(c)
          i += 1
        }
      } else {
        out.append(c)
        i += 1
      }
    }
    out.toString
  }

  /** Index after the next occurrence of `close`, or the end of the text. */
  private def skipPast(text: String, close: String, from: Int): Int = {
    val idx = text.indexOf(close, from)
    if (idx < 0) text.length else idx + close.length
  }

  private def isTagStart(text: String, i: Int): Boolean = text.startsWith("{{", i) || text.startsWith("{%", i)

  /** Index after the Jinja tag starting at `from`, or the end of the text if it is unterminated. */
  private def tagEnd(text: String, from: Int): Int = {
    skipPast(text, if (text.charAt(from + 1) == '{') "}}" else "%}", from + 2)
  }

  /** Index of the next line break (which is kept), or the end of the text. */
  private def endOfLine(text: String, from: Int): Int = {
    val idx = text.indexOf('\n', from)
    if (idx < 0) text.length else idx
  }

  /**
   * Index of the closing quote of a string literal whose content starts at `from`, or -1 if it is unterminated.
   * Jinja tags inside the literal are skipped as a whole, since they may contain quotes themselves.
   */
  private def stringEnd(text: String, from: Int, quote: String): Int = {
    var j = from
    while (j < text.length) {
      val c = text.charAt(j)
      if (c == '\\') j += 2
      else if (isTagStart(text, j)) j = tagEnd(text, j)
      else if (text.startsWith(quote, j)) return j
      else if (c == '\n' && quote.length == 1) return -1
      else j += 1
    }
    -1
  }

  /** Index of the closing `>` if the `<` at `from` starts an IRI (possibly containing Jinja tags), else -1. */
  private def iriEnd(text: String, from: Int): Int = {
    var j = from + 1
    while (j < text.length) {
      val c = text.charAt(j)
      if (c == '>') {
        return j
      } else if (isTagStart(text, j)) {
        j = tagEnd(text, j)
      } else if (c.isWhitespace || "<\"{}|^`\\".indexOf(c) >= 0) {
        return -1
      } else {
        j += 1
      }
    }
    -1
  }
}
