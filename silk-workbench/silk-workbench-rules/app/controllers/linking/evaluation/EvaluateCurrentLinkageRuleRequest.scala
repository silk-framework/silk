package controllers.linking.evaluation

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import play.api.libs.json.{Format, Json}

case class EvaluateCurrentLinkageRuleRequest(@Parameter(
                                                name = "includeReferenceLinks",
                                                description = "When true, this will return an evaluation of the reference links in addition to freshly matched links.",
                                                required = false,
                                                in = ParameterIn.QUERY,
                                                schema = new Schema(implementation = classOf[Boolean], defaultValue = "false")
                                              )
                                              includeReferenceLinks: Option[Boolean] = None,
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

    implicit val evaluationLinkFilterEnumFormat = Json.formatEnum(this)
  }

  object EvaluationLinkSortEnum extends Enumeration {
    val scoreAsc, scoreDesc, sourceEntityAsc, sourceEntityDesc, targetEntityAsc, targetEntityDesc = Value

    implicit val sortOrderFormat = Json.formatEnum(this)
  }

  implicit val evaluateCurrentLinkageRuleRequestFormat: Format[EvaluateCurrentLinkageRuleRequest] = Json.format[EvaluateCurrentLinkageRuleRequest]
}