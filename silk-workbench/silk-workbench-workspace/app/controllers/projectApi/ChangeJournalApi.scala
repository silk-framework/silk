package controllers.projectApi

import controllers.core.UserContextActions
import controllers.projectApi.ChangeJournalApi.{ChangeEntryJson, ChangeListJson, MarkReviewedJson, ReviewedJson, RevertOutcomeJson, RevertRequestJson, RevertResultsJson}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.workspace.WorkspaceFactory
import org.silkframework.workspace.changes.{ChangeEntry, RevertOutcome}
import play.api.libs.json.{Format, JsValue, Json}
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject.Inject

/** The change journal of a project: what has been changed, and reverting single changes. */
@Tag(name = "Project changes", description = "The changes made to a project, and reverting them.")
class ChangeJournalApi @Inject()() extends InjectedController with UserContextActions {

  @Operation(
    summary = "List changes",
    description = "The changes made to the project, as far as the configured store keeps them, newest first. A change can be reverted while " +
      "'revertible' is true and 'revertedBy' does not name the change that reverted it already; 'reverts' names the " +
      "change a revert undid.",
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
    val journal = WorkspaceFactory().workspace.project(projectId).changeJournal
    val entries = journal.all
    val revertedBy = entries.flatMap(entry => entry.reverts.map(_ -> entry.seq)).toMap
    val unreviewed = journal.unreviewed.map(_.seq).toSet
    Ok(Json.toJson(ChangeListJson(journal.reviewedUpTo,
      entries.reverse.map(entry => ChangeEntryJson.of(entry, revertedBy.get(entry.seq), unreviewed.contains(entry.seq))))))
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
      new ApiResponse(responseCode = "409", description = "The change has been reverted already, is not revertible, " +
        "the project has changed since so that the revert does not apply, or applying the inverse changed nothing, " +
        "e.g. because a variable template resolves the restored value again. The project is left unchanged.")
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

  @Operation(
    summary = "Mark changes as reviewed",
    description = "Advances the reviewed watermark: the changes up to the given seq no longer count as unreviewed. " +
      "Reviews only add up, so a seq at or below the watermark changes nothing.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "The reviewed watermark after the call.",
        content = Array(new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[ReviewedJson]),
          examples = Array(new ExampleObject("""{"reviewedUpTo": 3}"""))
        ))
      ),
      new ApiResponse(responseCode = "404", description = "The project does not exist."),
      new ApiResponse(responseCode = "409", description = "The seq lies beyond the latest recorded change.")
    ))
  @RequestBody(
    description = "The seq up to which the changes have been reviewed.",
    required = true,
    content = Array(new Content(
      mediaType = "application/json",
      schema = new Schema(implementation = classOf[MarkReviewedJson]),
      examples = Array(new ExampleObject("""{"upTo": 3}"""))
    )))
  def markReviewed(@Parameter(
                     name = "projectId",
                     description = "The project identifier",
                     required = true,
                     in = ParameterIn.PATH,
                     schema = new Schema(implementation = classOf[String])
                   )
                   projectId: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    val journal = WorkspaceFactory().workspace.project(projectId).changeJournal
    journal.markReviewed(request.body.as[MarkReviewedJson].upTo)
    Ok(Json.toJson(ReviewedJson(journal.reviewedUpTo)))
  }

  @Operation(
    summary = "Revert changes",
    description = "Reverts the given changes newest-first, so that no change is reverted while a later one still builds " +
      "on it: a change that cannot be reverted is skipped, one whose inverse changes nothing stays unchanged, a conflict " +
      "stops the batch and leaves the remaining changes unattempted. The outcomes report what happened to each change; " +
      "the changes reverted before a conflict stay reverted.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "One outcome per requested change, newest first. Conflicts are outcomes, not errors.",
        content = Array(new Content(
          mediaType = "application/json",
          schema = new Schema(implementation = classOf[RevertResultsJson]),
          examples = Array(new ExampleObject(ChangeJournalApi.revertAllExample))
        ))
      ),
      new ApiResponse(responseCode = "404", description = "The project does not exist.")
    ))
  @RequestBody(
    description = "The sequence numbers of the changes to revert, in any order.",
    required = true,
    content = Array(new Content(
      mediaType = "application/json",
      schema = new Schema(implementation = classOf[RevertRequestJson]),
      examples = Array(new ExampleObject("""{"seqs": [2, 3]}"""))
    )))
  def revertAll(@Parameter(
                  name = "projectId",
                  description = "The project identifier",
                  required = true,
                  in = ParameterIn.PATH,
                  schema = new Schema(implementation = classOf[String])
                )
                projectId: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request => implicit userContext =>
    val journal = WorkspaceFactory().workspace.project(projectId).changeJournal
    val outcomes = journal.revertAll(request.body.as[RevertRequestJson].seqs)
    Ok(Json.toJson(RevertResultsJson(outcomes.map(RevertOutcomeJson.of))))
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
                             @Schema(description = "Whether the change can be reverted at all. False for a workflow run, and for a file overwrite or deletion, whose previous content is not kept.")
                             revertible: Boolean,
                             @Schema(description = "The change this one reverted. Present only if the change was made by reverting one.")
                             reverts: Option[Int],
                             @Schema(description = "The change that reverted this one. Present only if the change has been reverted.")
                             revertedBy: Option[Int],
                             @Schema(description = "True for an agent change after the reviewed watermark that has not been reverted. Absent otherwise.")
                             unreviewed: Option[Boolean])

  object ChangeEntryJson {

    implicit val format: Format[ChangeEntryJson] = Json.format[ChangeEntryJson]

    def of(entry: ChangeEntry, revertedBy: Option[Int], unreviewed: Boolean = false): ChangeEntryJson = {
      ChangeEntryJson(entry.seq, entry.timestamp.toString, entry.user, entry.origin, entry.change.changeType,
        entry.change.describe, entry.change.inverse.isDefined, entry.reverts, revertedBy,
        unreviewed = if(unreviewed) Some(true) else None)
    }
  }

  @Schema(description = "The changes of a project, newest first.")
  case class ChangeListJson(@Schema(description = "The seq up to which the user has reviewed the changes; 0 if never set.")
                            reviewedUpTo: Int,
                            changes: Seq[ChangeEntryJson])

  object ChangeListJson {
    implicit val format: Format[ChangeListJson] = Json.format[ChangeListJson]
  }

  @Schema(description = "Marks the changes up to a seq as reviewed.")
  case class MarkReviewedJson(@Schema(description = "The seq up to which the changes have been reviewed.")
                              upTo: Int)

  object MarkReviewedJson {
    implicit val format: Format[MarkReviewedJson] = Json.format[MarkReviewedJson]
  }

  @Schema(description = "The reviewed watermark of a project.")
  case class ReviewedJson(@Schema(description = "The seq up to which the user has reviewed the changes.")
                          reviewedUpTo: Int)

  object ReviewedJson {
    implicit val format: Format[ReviewedJson] = Json.format[ReviewedJson]
  }

  @Schema(description = "The changes to revert.")
  case class RevertRequestJson(@Schema(description = "The sequence numbers of the changes to revert, in any order.")
                               seqs: Seq[Int])

  object RevertRequestJson {
    implicit val format: Format[RevertRequestJson] = Json.format[RevertRequestJson]
  }

  @Schema(description = "What happened to one change of a revert batch.")
  case class RevertOutcomeJson(@Schema(description = "The sequence number of the change.")
                               seq: Int,
                               @Schema(description = "'reverted', 'skipped' (cannot be reverted, the batch continues), " +
                                 "'unchanged' (the inverse changed nothing, the batch continues), 'conflict' (stops " +
                                 "the batch) or 'notAttempted' (a newer change conflicted).")
                               outcome: String,
                               @Schema(description = "Why the change was skipped, left unchanged or conflicted. Absent otherwise.")
                               message: Option[String],
                               @Schema(description = "The change that records the revert. Present only for 'reverted'.")
                               entry: Option[ChangeEntryJson])

  object RevertOutcomeJson {

    implicit val format: Format[RevertOutcomeJson] = Json.format[RevertOutcomeJson]

    def of(outcome: RevertOutcome): RevertOutcomeJson = {
      outcome match {
        case RevertOutcome.Reverted(seq, entry) =>
          RevertOutcomeJson(seq, "reverted", None, Some(ChangeEntryJson.of(entry, revertedBy = None)))
        case RevertOutcome.Skipped(seq, reason) =>
          RevertOutcomeJson(seq, "skipped", Some(reason), None)
        case RevertOutcome.Unchanged(seq, reason) =>
          RevertOutcomeJson(seq, "unchanged", Some(reason), None)
        case RevertOutcome.Conflict(seq, reason) =>
          RevertOutcomeJson(seq, "conflict", Some(reason), None)
        case RevertOutcome.NotAttempted(seq) =>
          RevertOutcomeJson(seq, "notAttempted", None, None)
      }
    }
  }

  @Schema(description = "The outcomes of a revert batch, newest first.")
  case class RevertResultsJson(results: Seq[RevertOutcomeJson])

  object RevertResultsJson {
    implicit val format: Format[RevertResultsJson] = Json.format[RevertResultsJson]
  }

  // Annotation arguments, so plain literals.
  final val listExample =
    """
      {
        "reviewedUpTo": 0,
        "changes": [
        {
          "seq": 3,
          "timestamp": "2026-08-26T09:51:02.417Z",
          "user": "urn:user:alice",
          "type": "RemoveMapping",
          "description": "Removed mapping rule 'name' from transform 'persons'",
          "revertible": true,
          "reverts": 2
        },
        {
          "seq": 2,
          "timestamp": "2026-08-26T09:50:12.345Z",
          "user": "urn:user:alice",
          "origin": "mcp:claude-code",
          "type": "AddMapping",
          "description": "Added mapping rule 'name' under 'root' in transform 'persons'",
          "revertible": true,
          "revertedBy": 3
        },
        {
          "seq": 1,
          "timestamp": "2026-08-26T09:49:58.001Z",
          "user": "urn:user:alice",
          "origin": "mcp:claude-code",
          "type": "AddTask",
          "description": "Added transform 'persons'",
          "revertible": true,
          "unreviewed": true
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
        "revertible": true,
        "reverts": 2
      }
    """

  final val revertAllExample =
    """
      {
        "results": [
        {
          "seq": 3,
          "outcome": "reverted",
          "entry": {
            "seq": 4,
            "timestamp": "2026-08-26T09:51:02.417Z",
            "user": "urn:user:alice",
            "type": "RemoveMapping",
            "description": "Removed mapping rule 'name' from transform 'persons'",
            "revertible": true,
            "reverts": 3
          }
        },
        {
          "seq": 2,
          "outcome": "conflict",
          "message": "Task 'persons' in project 'movies' has been changed since."
        },
        {
          "seq": 1,
          "outcome": "notAttempted"
        }
        ]
      }
    """
}
