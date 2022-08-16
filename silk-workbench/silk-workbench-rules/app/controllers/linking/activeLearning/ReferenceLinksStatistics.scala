package controllers.linking.activeLearning

import io.swagger.v3.oas.annotations.media.Schema
import org.silkframework.entity.{Link, ReferenceLink}
import org.silkframework.rule.evaluation.ReferenceLinks
import play.api.libs.json.{Json, OFormat}

@Schema(description = "Statistics about the reference links of the current active learning session.")
case class ReferenceLinksStatistics(@Schema(description = "The number of existing reference links of the linking task.")
                                    existingLinks: Int,
                                    @Schema(description = "The number of new reference links that are not yet part of the linking task.")
                                    addedLinks: Int,
                                    @Schema(description = "The number of reference links that are part of the linking task, but have been removed.")
                                    removedLinks: Int)

object ReferenceLinksStatistics {

  implicit val referenceLinksStatisticsFormat: OFormat[ReferenceLinksStatistics] = Json.format[ReferenceLinksStatistics]

  def compute(existingReferenceLinks: ReferenceLinks, newReferenceLinks: Seq[ReferenceLink]): ReferenceLinksStatistics = {
    val existingReferenceLinksSet = existingReferenceLinks.positive ++ existingReferenceLinks.negative
    val newReferenceLinksSet = newReferenceLinks.map(_.asInstanceOf[Link]).toSet

    ReferenceLinksStatistics(
      existingLinks = existingReferenceLinksSet.size,
      addedLinks = newReferenceLinksSet.count(l => !existingReferenceLinksSet.contains(l)),
      removedLinks = existingReferenceLinksSet.count(l => !newReferenceLinksSet.contains(l)),
    )
  }

}