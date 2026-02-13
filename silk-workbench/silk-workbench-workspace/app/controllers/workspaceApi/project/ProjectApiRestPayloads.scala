package controllers.workspaceApi.project

import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import org.silkframework.config.MetaData
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.util.{Identifier, IdentifierUtils, Uri}
import org.silkframework.workspace.{Project, ProjectConfig, WorkspaceFactory}
import play.api.libs.json.{Format, Json}

/**
  * REST request and response classes.
  */
object ProjectApiRestPayloads {

  /**
    * Meta data used for projects and tasks. Does not include fields that should not be modified by a user request.
    *
    * @param label       label of the item
    * @param description optional description of the item
    */
  case class ItemMetaData(label: String, description: Option[String] = None,
                          @ArraySchema(schema = new Schema(implementation = classOf[String], required = false, nullable = true))
                          tags: Option[Set[String]] = None) {
    def asMetaData: MetaData = MetaData(Some(label), description, tags = tags.getOrElse(Set.empty).map(new Uri(_)))
  }

  object ItemMetaData {
    implicit val itemMetaDataFormat: Format[ItemMetaData] = Json.format[ItemMetaData]
  }

  /** Request to create a new project. */
  case class CreateProjectRequest(metaData: ItemMetaData, id: Option[String], groups: Option[Set[String]] = None) {

    /**
     * Executes the request.
     *
     * @return The created project.
     */
    def apply()(implicit user: UserContext): Project = {
      val parsedMetaData = metaData.asMetaData
      val generatedId = parsedMetaData.label match {
        case Some(label) if label.trim.nonEmpty =>
          IdentifierUtils.generateProjectId(label)
        case _ =>
          throw BadUserInputException("The label must not be empty!")
      }
      val projectId = id match {
        case Some(v) => Identifier(v)
        case None => generatedId
      }
      val project = WorkspaceFactory().workspace.createProject(ProjectConfig(projectId, metaData = cleanUpMetaData(parsedMetaData).asNewMetaData))
      // TODO For testing, discuss if this is a good default
      project.accessControl.setGroups(groups.getOrElse(user.user.map(_.groups).getOrElse(Set.empty)))
      project
    }

    private def cleanUpMetaData(metaData: MetaData) = {
      MetaData(metaData.label.map(_.trim).filter(_.nonEmpty), metaData.description.filter(_.trim.nonEmpty))
    }
  }

  object CreateProjectRequest {
    implicit val projectCreationData: Format[CreateProjectRequest] = Json.format[CreateProjectRequest]
  }
}