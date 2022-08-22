package controllers.linking.activeLearning

import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import org.silkframework.learning.active.ActiveLearning
import org.silkframework.rule.LinkSpec
import org.silkframework.serialization.json.MetaDataSerializers.UserInfo
import org.silkframework.workspace.ProjectTask
import play.api.libs.json.{Json, OFormat}

case class ActiveLearningInfo(@ArraySchema(schema = new Schema(description = "List of all users that have been involved in the current session.",
                                                               implementation = classOf[UserInfo]))
                              users: Seq[UserInfo],
                              @Schema(description = "Statistics about the changes to the reference links.")
                              referenceLinks: ReferenceLinksStatistics)

object ActiveLearningInfo {

  implicit val activeLearningInfoFormat: OFormat[ActiveLearningInfo] = Json.format[ActiveLearningInfo]

  def apply(task: ProjectTask[LinkSpec]): ActiveLearningInfo = {
    val state = task.activity[ActiveLearning].value()
    ActiveLearningInfo(
      users = state.users.toSeq.map(UserInfo.fromUser),
      referenceLinks = ReferenceLinksStatistics.compute(task.referenceLinks, state.referenceData.referenceLinks)
    )
  }

}


