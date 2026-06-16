package controllers.transform.autoCompletion

import org.silkframework.runtime.validation.ValidationException
import play.api.libs.json.{Format, Json}

/**
  * Rule auto-completion request.
  * @param valueRulesOnly If only value rules should be returned.
  * @param objectRulesOnly If only object rules should be returned.
  * @param searchQuery The optional text search query.
  * @param limit Optional limit of results returned.
  * @param format If true, the labels will be formatted in some cases, resembling the tree structure of the rules.
  */
case class RuleAutoCompletionRequest(valueRulesOnly: Option[Boolean],
                                     objectRulesOnly: Option[Boolean],
                                     searchQuery: Option[String],
                                     limit: Option[Int],
                                     format: Option[Boolean]
                                    ) {
  if(valueRulesOnly.getOrElse(false) && objectRulesOnly.getOrElse(false)) {
    throw new ValidationException("valueRulesOnly and objectRulesOnly cannot be both be true at the same time!")
  }
}

object RuleAutoCompletionRequest {
  implicit val ruleAutoCompletionRequestFormat: Format[RuleAutoCompletionRequest] = Json.format[RuleAutoCompletionRequest]
}