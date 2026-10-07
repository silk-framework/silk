package org.silkframework.serialization.json.changes

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.changes.ChangeJsonFormats.ChangeEntryJsonFormat
import org.silkframework.workspace.changes._
import play.api.libs.json.{JsObject, JsValue, Json}

import java.time.Instant

/**
  * The envelope without a project: the recorded changes, which hold no project content, and the placeholder for
  * an entry whose change cannot be read. The payloads with project content are covered by
  * [[ChangePayloadRoundTripTrait]] against a live project.
  */
class ChangeJsonFormatsTest extends AnyFlatSpec with Matchers {

  behavior of "ChangeEntryJsonFormat"

  private implicit val readContext: ReadContext = ReadContext.empty
  private implicit val writeContext: WriteContext[JsValue] = WriteContext.empty[JsValue]

  private val now = Instant.parse("2026-10-07T10:12:03.123456Z")

  private val proposal = ChangeEntry(1, now, Some("urn:elds-backend-users:alice"), Some("mcp:claude-code"), ProposedWorkflowRun("wf", Some("WF")))

  it should "round-trip the recorded changes, which need no project to read" in {
    val entries = Seq(
      proposal,
      ChangeEntry(2, now, None, None, WorkflowExecuted("wf", Some("exec-1"), failed = true, Some("WF")), fulfils = Some(1)),
      ChangeEntry(3, now, None, None, ResourceOverwritten("data/a.csv", FileState(Some(1), Some(now)), FileState(None, None)), reverts = Some(2)),
      ChangeEntry(4, now, None, None, ResourceDeleted("a.csv", FileState(Some(12), Some(now)))),
      ChangeEntry(5, now, None, None, DiscardedWorkflowRun("wf")))
    for(entry <- entries) {
      val line = Json.stringify(ChangeEntryJsonFormat.write(entry))
      withClue(line) { ChangeEntryJsonFormat.read(Json.parse(line)) shouldBe entry }
    }
  }

  it should "list an entry whose change it cannot read as a placeholder with the stored type and summary" in {
    val json = ChangeEntryJsonFormat.write(proposal).as[JsObject]
    def readWith(patch: JsObject): ChangeEntry = ChangeEntryJsonFormat.read(json ++ patch)
    val placeholder = UnreadableChange("ProposedWorkflowRun", "Proposed to run workflow 'WF'")

    // The header survives, only the change is replaced
    val unknownVersion = readWith(Json.obj("version" -> 2))
    unknownVersion shouldBe proposal.copy(change = placeholder)
    readWith(Json.obj("type" -> "Bogus")).change shouldBe placeholder.copy(changeType = "Bogus")
    val brokenRule = Json.obj("taskId" -> "t", "parentId" -> "root", "rule" -> Json.obj("type" -> "bogus"))
    readWith(Json.obj("type" -> "AddMapping", "change" -> brokenRule)).change shouldBe placeholder.copy(changeType = "AddMapping")
    val taskThatDoesNotLoad = Json.obj("task" -> Json.obj("id" -> "ds", "taskType" -> "Dataset",
      "data" -> Json.obj("taskType" -> "Dataset", "type" -> "noSuchDataset", "parameters" -> Json.obj())))
    readWith(Json.obj("type" -> "AddTask", "change" -> taskThatDoesNotLoad)).change shouldBe placeholder.copy(changeType = "AddTask")

    // Listed, but not revertible
    placeholder.inverse shouldBe None
    placeholder.describe shouldBe "Proposed to run workflow 'WF'"
    // Written again, it stays the placeholder it is
    ChangeEntryJsonFormat.read(ChangeEntryJsonFormat.write(unknownVersion)) shouldBe unknownVersion
  }
}
