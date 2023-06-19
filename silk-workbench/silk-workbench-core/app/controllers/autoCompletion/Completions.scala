package controllers.autoCompletion

import io.swagger.v3.oas.annotations.media.Schema.RequiredMode
import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import org.silkframework.util.StringUtils
import play.api.libs.json._

import java.net.URLDecoder
import scala.util.Try

/** The base properties of an auto-completion result. */
case class CompletionsBase(@ArraySchema(schema = new Schema(implementation = classOf[CompletionBase]))
                           completions: Seq[CompletionBase])

object CompletionsBase {
  implicit val completionsBaseFormat: Format[CompletionsBase] = Json.format[CompletionsBase]
}

/**
  * A list of auto completions.
  */
@Schema(description = "List of auto completions")
case class Completions(@ArraySchema(schema = new Schema(implementation = classOf[Completion]))
                       values: Seq[Completion] = Seq.empty) {

  def toCompletionsBase: CompletionsBase = CompletionsBase(values.map(_.toCompletionBase))

  /**
    * Adds another list of completions to this one and returns the result.
    */
  def +(completions: Completions): Completions = {
    Completions(values ++ completions.values)
  }

  /**
    * Filters and ranks all completions using a search term.
    */
  def filterAndSort(term: String,
                    maxResults: Int,
                    sortEmptyTermResult: Boolean = true,
                    multiWordFilter: Boolean = false): Completions = {
    if (term.trim.isEmpty) {
      // If the term is empty, return some completions anyway
      val sortedValues = if(sortEmptyTermResult) values.sortBy(_.labelOrGenerated.length) else values
      Completions(sortedValues.take(maxResults))
    } else {
      // Filter all completions that match the search term and sort them by score
      val fm = filterMethod(term, multiWordFilter)
      val scoredValues = for(value <- values; score <- fm(value)) yield (value, score)
      val sortedValues = scoredValues.sortBy(-_._2).map(_._1)
      Completions(sortedValues.take(maxResults))
    }
  }

  // Choose the filter / ranking method
  private def filterMethod(term: String,
                           multiWordFilter: Boolean): (Completion => Option[Double]) = {
    if(multiWordFilter) {
      val searchWords = StringUtils.extractSearchTerms(term.stripPrefix("/").stripPrefix("\\"))
      val termMinLength = if(searchWords.length > 0) searchWords.map(_.length).min.toDouble else 1.0
      completion: Completion => completion.matchesMultiWordQuery(searchWords, termMinLength)
    } else {
      val normalizedTerm = Completions.normalizeTerm(term)
      completion: Completion => completion.matches(normalizedTerm)
    }
  }

  def toJson: JsValue = {
    JsArray(values.map(_.toJson))
  }
}

/** The base properties for auto-completion results. */
case class CompletionBase(value: String,
                          label: Option[String] = None,
                          description: Option[String] = None)

object CompletionBase {
  implicit val completionBaseFormat: Format[CompletionBase] = Json.format[CompletionBase]
}

/**
  * A single completion.
  *
  * @param value        The value to be filled if the user selects this completion.
  * @param confidence   The confidence of this completion.
  * @param label        A user readable label if available
  * @param description  A user readable description if available
  * @param category     The category to be shown in the autocompletion
  * @param isCompletion True, if this is a valid completion. False, if this is a (error) message.
  * @param extra        Some extra values depending on the category
  */
case class Completion(@Schema(description = "The value to be filled if the user selects this completion.", requiredMode = RequiredMode.REQUIRED)
                      value: String,
                      @Schema(description = "The confidence of this completion.", requiredMode = RequiredMode.REQUIRED)
                      confidence: Double = Double.MinValue,
                      @Schema(description = "A user readable label if available.")
                      label: Option[String],
                      @Schema(description = "A user readable description if available.")
                      description: Option[String],
                      @Schema(description = "The category to be shown in the autocompletion.", requiredMode = RequiredMode.REQUIRED)
                      category: String,
                      @Schema(description = "True, if this is a valid completion. False, if this is a (error) message.", requiredMode = RequiredMode.REQUIRED)
                      isCompletion: Boolean,
                      @Schema(
                        description = "Some extra values depending on the category (arbitrary JSON)",
                        implementation = classOf[Object], // Dummy type, because JsValue is not recognized as JSON by Swagger
                      )
                      extra: Option[JsValue] = None) {

  /**
    * Returns the label if present or generates a label from the value if no label is set.
    */
  @Schema(hidden = true)
  lazy val labelOrGenerated: String = label match {
    case Some(existingLabel) =>
      existingLabel
    case None =>
      val lastPart = value.substring(value.lastIndexWhere(c => c == '#' || c == '/' || c == ':') + 1).filterNot(_ == '>')
      Try(URLDecoder.decode(lastPart, "UTF8")).getOrElse(lastPart)
  }

  /**
    * Checks if a term matches this completion.
    *
    * @param normalizedTerm the term normalized using normalizeTerm(term)
    * @return None, if the term does not match at all.
    *         Some(matchScore), if the terms match.
    */
  def matches(normalizedTerm: String): Option[Double] = {
    val values = Set(value, labelOrGenerated) ++ description
    val scores = values.flatMap(rank(normalizedTerm))
    if(scores.isEmpty)
      None
    else
      Some(scores.max)
  }

  /** Match against a multi word query, rank matches higher that have more matches in the label, then value and then description. */
  def matchesMultiWordQuery(lowerCaseTerms: Array[String],
                            termMinLength: Double): Option[Double] = {
    val lowerCaseValue = value.toLowerCase
    val lowerCaseLabel = label.getOrElse("").toLowerCase
    val lowerCaseDescription = description.getOrElse("").toLowerCase
    val searchIn = s"$lowerCaseValue $lowerCaseLabel $lowerCaseDescription"
    val matches = StringUtils.matchesSearchTerm(lowerCaseTerms, searchIn)
    if(matches) {
      var score = 0.0
      val labelMatchCount = StringUtils.matchCount(lowerCaseTerms, lowerCaseLabel)
      val labelLengthBonus = termMinLength / lowerCaseLabel.size
      score += (0.5 + labelLengthBonus) * labelMatchCount
      score += 0.2 * StringUtils.matchCount(lowerCaseTerms, lowerCaseValue)
      score += 0.1 * StringUtils.matchCount(lowerCaseTerms, lowerCaseDescription)
      Some(score)
    } else {
      None
    }
  }

  /**
    * Ranks a term, the higher the result the higher the ranking.
    */
  private def rank(normalizedTerm: String)(value: String): Option[Double] = {
    val normalizedValue = Completions.normalizeTerm(value)
    if(normalizedValue.contains(normalizedTerm)) {
      Some(normalizedTerm.length.toDouble / normalizedValue.length)
    } else {
      None
    }
  }

  def toJson: JsValue = {
    val genericObject = Json.obj(
      "value" -> value,
      "label" -> labelOrGenerated,
      "description" -> description,
      "category" -> category,
      "isCompletion" -> isCompletion
    )
    extra match {
      case Some(ex) => genericObject ++ JsObject(Seq("extra" -> ex))
      case None => genericObject
    }
  }

  def toCompletionBase: CompletionBase = CompletionBase(value, label, description)
}

object Completions {
  // Characters that are removed before comparing (in addition to whitespaces)
  private val ignoredCharacters = Set('/', '\\')

  /**
    * Normalizes a term.
    */
  def normalizeTerm(term: String): String = {
    term.toLowerCase.filterNot(c => c.isWhitespace || ignoredCharacters.contains(c))
  }
}