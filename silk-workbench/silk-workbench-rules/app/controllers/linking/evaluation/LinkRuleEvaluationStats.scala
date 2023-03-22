package controllers.linking.evaluation

import play.api.libs.json.{Format, Json}

/** Statistics of a linking evaluation activity. */
case class LinkRuleEvaluationStats(nrSourceEntities: Int,
                                   nrTargetEntities: Int,
                                   nrLinks: Int)

object LinkRuleEvaluationStats {
  implicit val linkRuleEvaluationStatsFormat: Format[LinkRuleEvaluationStats] = Json.format[LinkRuleEvaluationStats]
}
