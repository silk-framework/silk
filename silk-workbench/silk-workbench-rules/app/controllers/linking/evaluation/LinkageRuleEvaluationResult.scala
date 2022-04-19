package controllers.linking.evaluation

import play.api.libs.json.{Format, Json}

/** Result of evaluating a linkage rule against a set of links. */
case class LinkageRuleEvaluationResult(truePositives: Int,
                                       trueNegatives: Int,
                                       falsePositives: Int,
                                       falseNegatives: Int,
                                       fMeasure: String,
                                       precision: String,
                                       recall: String)

object LinkageRuleEvaluationResult {
  implicit val linkageRuleEvaluationResultFormat: Format[LinkageRuleEvaluationResult] = Json.format[LinkageRuleEvaluationResult]
}