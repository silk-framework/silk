package controllers.projectApi

import controllers.core.UserContextActions
import controllers.projectApi.ChangeJournalApi.{ChangeEntryJson, ChangeListJson}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.workspace.WorkspaceFactory
import org.silkframework.workspace.changes.ChangeEntry
import play.api.libs.json.{Format, Json}
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject.Inject

/** The change journal of a project: what has been changed, and reverting single changes. */
@Tag(name = "Project changes", description = "The changes made to a project, and reverting them.")
class ChangeJournalApi @Inject()() extends InjectedController with UserContextActions {

  @Operation(
    summary = "List changes",
    description = "The changes made to the project since it was loaded, newest first. A change can be reverted unless " +
      "'revertedBy' names the change that reverted it already; 'reverts' names the change a revert undid.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[ChangeListJson]),
          examples = Array(new ExampleObject(ChangeJournalApi.listExample))
        ))
      ),
      new ApiResponse(responseCode = "404", description = "The project does not exist.")
    ))
  def changes(@Parameter(
                name = "projectId",
                description = "The project identifier",
                required = true,
                in = ParameterIn.PATH,
                schema = new Schema(implementation = classOf[String])
              )
              projectId: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val entries = WorkspaceFactory().workspace.project(projectId).changeJournal.all
    val revertedBy = entries.flatMap(entry => entry.reverts.map(_ -> entry.seq)).toMap
    Ok(Json.toJson(ChangeListJson(entries.reverse.map(entry => ChangeEntryJson.of(entry, revertedBy.get(entry.seq))))))
  }

  @Operation(
    summary = "Revert a change",
    description = "Applies the inverse of the change, which is recorded as a new change; reverting that one redoes the change.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The change that records the revert.",
        content = Array(new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[ChangeEntryJson]),
          examples = Array(new ExampleObject(ChangeJournalApi.revertExample))
        ))
      ),
      new ApiResponse(responseCode = "404", description = "The project or the change does not exist."),
      new ApiResponse(responseCode = "409", description = "The change has been reverted already, or the project has " +
        "changed since so that the revert does not apply. The project is left unchanged.")
    ))
  def revert(@Parameter(
               name = "projectId",
               description = "The project identifier",
               required = true,
               in = ParameterIn.PATH,
               schema = new Schema(implementation = classOf[String])
             )
             projectId: String,
             @Parameter(
               name = "seq",
               description = "The sequence number of the change",
               required = true,
               in = ParameterIn.PATH,
               schema = new Schema(implementation = classOf[Int])
             )
             seq: Int): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    val journal = WorkspaceFactory().workspace.project(projectId).changeJournal
    Ok(Json.toJson(ChangeEntryJson.of(journal.revert(seq), revertedBy = None)))
  }
}

object ChangeJournalApi {

  @Schema(description = "A recorded change of a project.")
  case class ChangeEntryJson(@Schema(description = "Sequence number of the change, ascending in the order the changes were made.")
                             seq: Int,
                             @Schema(description = "When the change was made, as ISO-8601 timestamp.")
                             timestamp: String,
                             @Schema(description = "URI of the user who made the change. Absent if unknown.")
                             user: Option[String],
                             @Schema(description = "The client the change came from, e.g. 'mcp:<user agent>'. Absent if unknown.")
                             origin: Option[String],
                             @Schema(description = "The kind of change, e.g. 'AddMapping', 'ReplaceTask' or 'SetVariable'.")
                             `type`: String,
                             @Schema(description = "What has been changed, for display.")
                             description: String,
                             @Schema(description = "The change this one reverted. Present only if the change was made by reverting one.")
                             reverts: Option[Int],
                             @Schema(description = "The change that reverted this one. Present only if the change has been reverted.")
                             revertedBy: Option[Int])

  object ChangeEntryJson {

    implicit val format: Format[ChangeEntryJson] = Json.format[ChangeEntryJson]

    def of(entry: ChangeEntry, revertedBy: Option[Int]): ChangeEntryJson = {
      ChangeEntryJson(entry.seq, entry.timestamp.toString, entry.user, entry.origin, entry.change.changeType,
        entry.change.describe, entry.reverts, revertedBy)
    }
  }

  @Schema(description = "The changes of a project, newest first.")
  case class ChangeListJson(changes: Seq[ChangeEntryJson])

  object ChangeListJson {
    implicit val format: Format[ChangeListJson] = Json.format[ChangeListJson]
  }

  // Annotation arguments, so plain literals.
  final val listExample =
    """
      {
        "changes": [
        {
          "seq": 3,
          "timestamp": "2026-08-26T09:51:02.417Z",
          "user": "urn:user:alice",
          "type": "RemoveMapping",
          "description": "Removed mapping rule 'name' from transform 'persons'",
          "reverts": 2
        },
        {
          "seq": 2,
          "timestamp": "2026-08-26T09:50:12.345Z",
          "user": "urn:user:alice",
          "origin": "mcp:claude-code",
          "type": "AddMapping",
          "description": "Added mapping rule 'name' under 'root' in transform 'persons'",
          "revertedBy": 3
        },
        {
          "seq": 1,
          "timestamp": "2026-08-26T09:49:58.001Z",
          "user": "urn:user:alice",
          "origin": "mcp:claude-code",
          "type": "AddTask",
          "description": "Added task 'persons'"
        }
        ]
      }
    """

  final val revertExample =
    """
      {
        "seq": 3,
        "timestamp": "2026-08-26T09:51:02.417Z",
        "user": "urn:user:alice",
        "type": "RemoveMapping",
        "description": "Removed mapping rule 'name' from transform 'persons'",
        "reverts": 2
      }
    """
}
