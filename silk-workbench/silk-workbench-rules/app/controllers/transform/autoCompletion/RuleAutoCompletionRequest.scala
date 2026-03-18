package controllers.transform.autoCompletion

import org.silkframework.runtime.validation.ValidationException
import play.api.libs.json.{Format, Json}

case class RuleAutoCompletionRequest(valueRulesOnly: Option[Boolean],
                                     objectRulesOnly: Option[Boolean],
                                     searchQuery: Option[String],
                                     limit: Option[Int]) {
  if(valueRulesOnly.getOrElse(false) && objectRulesOnly.getOrElse(false)) {
    throw new ValidationException("valueRulesOnly and objectRulesOnly cannot be both be true at the same time!")
  }
}

object RuleAutoCompletionRequest {
  implicit val ruleAutoCompletionRequestFormat: Format[RuleAutoCompletionRequest] = Json.format[RuleAutoCompletionRequest]
}