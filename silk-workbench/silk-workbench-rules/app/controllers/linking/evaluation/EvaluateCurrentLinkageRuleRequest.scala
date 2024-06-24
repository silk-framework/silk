package controllers.linking.evaluation

import controllers.linking.evaluation.EvaluateCurrentLinkageRuleRequest.EvaluationLinkSortEnum
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import play.api.libs.json.{Format, Json}

case class EvaluateCurrentLinkageRuleRequest(@Parameter(
                                                name = "includeReferenceLinks",
                                                description = "When true, this will return the reference links in the evaluation result. These will come before the links of the evaluation activity.",
                                                required = false,
                                                in = ParameterIn.QUERY,
                                                schema = new Schema(implementation = classOf[Boolean], defaultValue = "false")
                                              )
                                              includeReferenceLinks: Option[Boolean] = None,
                                             @Parameter(
                                               name = "includeEvaluationLinks",
                                               description = "When true, this will return links from the evaluation activity, i.e. positive links that were matched with the then existing linking rule.",
                                               required = false,
                                               in = ParameterIn.QUERY,
                                               schema = new Schema(implementation = classOf[Boolean], defaultValue = "true")
                                             )
                                             includeEvaluationLinks: Option[Boolean] = Some(true),
                                            /** Paging parameters */
                                              offset: Option[Int] = None,
                                              limit: Option[Int] = None,
                                            /** The query by which to filter the results. */
                                              query: Option[String] = None,
                                            /** Filters */
                                             filters: Option[Seq[EvaluateCurrentLinkageRuleRequest.EvaluationLinkFilterEnum.Value]] = None,
                                            /** Sorting */
                                             sortBy: Option[List[EvaluateCurrentLinkageRuleRequest.EvaluationLinkSortEnum.Value]] = None
                                            )

object EvaluateCurrentLinkageRuleRequest {
  object EvaluationLinkFilterEnum extends Enumeration {

    type EvaluationLinkFilterEnum = Value

    val positiveLinks, negativeLinks, undecidedLinks = Value

    implicit val evaluationLinkFilterEnumFormat: Format[EvaluateCurrentLinkageRuleRequest.EvaluationLinkFilterEnum.Value] = Json.formatEnum(this)
  }

  object EvaluationLinkSortEnum extends Enumeration {
    val scoreAsc, scoreDesc, sourceEntityAsc, sourceEntityDesc, targetEntityAsc, targetEntityDesc = Value

    implicit val sortOrderFormat: Format[EvaluationLinkSortEnum.Value] = Json.formatEnum(this)
  }

  implicit val evaluateCurrentLinkageRuleRequestFormat: Format[EvaluateCurrentLinkageRuleRequest] = Json.format[EvaluateCurrentLinkageRuleRequest]
}