package org.silkframework.serialization.json.changes

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.JsonParseException
import org.silkframework.serialization.json.changes.ChangeJsonFormats.{ChangeEntryJsonFormat, ChangeHeaderJsonFormat, ChangeJsonFormat}
import org.silkframework.util.Identifier
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

  private val run = ChangeEntry(2, now, None, None, WorkflowExecuted("wf", Some("exec-1"), failed = true, Some("WF")), fulfils = Some(1))

  private val overwritten = ChangeEntry(3, now, None, None, ResourceOverwritten("data/a.csv", FileState(Some(1), Some(now)), FileState(None, None)), reverts = Some(2))

  private val recorded = Seq(proposal, run, overwritten,
    ChangeEntry(4, now, None, None, ResourceDeleted("a.csv", FileState(Some(12), Some(now)))),
    ChangeEntry(5, now, None, None, DiscardedWorkflowRun("wf")))

  it should "round-trip the recorded changes, which need no project to read" in {
    for(entry <- recorded) {
      val line = Json.stringify(ChangeEntryJsonFormat.write(entry))
      withClue(line) { ChangeEntryJsonFormat.read(Json.parse(line)) shouldBe entry }
    }
  }

  it should "read the header of a line without its change" in {
    for(entry <- recorded) {
      val line = Json.stringify(ChangeEntryJsonFormat.write(entry))
      // The change comes last, so the header is what precedes it
      line should endWith(",\"change\":" + Json.stringify(ChangeJsonFormat.write(entry.change)) + "}")
      withClue(line) { ChangeHeaderJsonFormat.readLine(line) shouldBe entry.header }
    }
    // What the header carries beyond the entry's own fields
    proposal.header.proposal shouldBe true
    proposal.header.revertible shouldBe true
    proposal.header.taskId shouldBe Some(Identifier("wf"))
    run.header.revertible shouldBe false
    run.header.executionId shouldBe Some("exec-1")
    overwritten.header.path shouldBe Some("data/a.csv")
    // A quote inside a value is escaped, so a label that holds the start of the change field does not cut the header short
    val quoted = ChangeEntry(6, now, None, None, ProposedWorkflowRun("wf", Some("x\",\"change\":{\"y")))
    ChangeHeaderJsonFormat.readLine(Json.stringify(ChangeEntryJsonFormat.write(quoted))) shouldBe quoted.header
  }

  it should "list an entry whose change it cannot read as a placeholder with the stored type and summary" in {
    val json = ChangeEntryJsonFormat.write(proposal).as[JsObject]
    def readWith(patch: JsObject): ChangeEntry = ChangeEntryJsonFormat.read(json ++ patch)
    val placeholder = UnreadableChange("ProposedWorkflowRun", "Proposed to run workflow 'WF'")

    // The change is the placeholder; of the header, the fields every version carries are read, the rest are the placeholder's
    val unknownVersion = readWith(Json.obj("version" -> 2))
    unknownVersion.change shouldBe placeholder
    unknownVersion.header shouldBe proposal.header.copy(revertible = false, proposal = false, taskId = None)
    // A later version may not write the summary; the type stands in. The seq must be a whole number in any version
    ChangeEntryJsonFormat.read(json - "summary" ++ Json.obj("version" -> 2)).change shouldBe placeholder.copy(summary = "ProposedWorkflowRun")
    a[JsonParseException] should be thrownBy readWith(Json.obj("version" -> 2, "seq" -> 1.5))
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
