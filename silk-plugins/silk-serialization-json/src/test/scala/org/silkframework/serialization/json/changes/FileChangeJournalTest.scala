package org.silkframework.serialization.json.changes

import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.validation.NotFoundException
import org.silkframework.util.FileUtils._
import org.silkframework.workspace.changes.{AddMapping, ChangeConflictException, ChangeJournalTestTrait, RevertOutcome, UnreadableChange}

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import scala.jdk.CollectionConverters._

/** The journal against the file store on a temporary directory. */
class FileChangeJournalTest extends ChangeJournalTestTrait {

  private val directory = Files.createTempDirectory("fileChangeJournal")

  override protected def storeProperties: Map[String, Option[String]] = Map(
    "workspace.changes.plugin" -> Some("fileChangeJournal"),
    "workspace.changes.fileChangeJournal.dir" -> Some(directory.toString))

  override protected def afterAll(): Unit = {
    try {
      super.afterAll()
    } finally {
      directory.toFile.deleteRecursive()
    }
  }

  it should "report an entry whose stored change cannot be read as a conflict and refuse to revert it" in {
    val project = retrieveOrCreateProject("journalUnreadableChange")
    val journal = project.changeJournal
    val task = project.addTask[TransformSpec]("transform", transform(name))
    task.applyChange(AddMapping("transform", "root", age))
    val added = journal.all.last
    added.revertible shouldBe true
    // The rule inside the stored change is damaged; the header, which the store keeps, says nothing of it
    val segment = directory.resolve(project.id.toString).resolve("000000001.jsonl")
    val lines = Files.readAllLines(segment, UTF_8).asScala.toSeq
    Files.write(segment, (lines.init :+ lines.last.replace("\"type\":\"direct\"", "\"type\":\"bogus\"")).mkString("", "\n", "\n").getBytes(UTF_8))

    journal.all.last shouldBe added
    journal.entry(added.seq).map(_.change) shouldBe Some(UnreadableChange("AddMapping", added.summary))
    val refusal = s"Change ${added.seq} in project 'journalUnreadableChange' cannot be reverted: its stored change cannot be read."
    journal.revertConflicts(Seq(added)) shouldBe Map(added.seq -> refusal)
    (the[ChangeConflictException] thrownBy journal.revert(added.seq)).getMessage should include("cannot be reverted")
    // A batch skips it, as it skips a change without inverse
    journal.revertAll(Seq(added.seq)) shouldBe
      Seq(RevertOutcome.Skipped(added.seq, s"Change ${added.seq} (${added.describe}) cannot be reverted: its stored change cannot be read."))

    // A line whose change is not even JSON still has its header; the conflict is the same, and there is no entry to revert
    val damaged = Files.readAllLines(segment, UTF_8).asScala.toSeq
    Files.write(segment, (damaged.head.replace(",\"change\":{", ",\"change\":{{") +: damaged.tail).mkString("", "\n", "\n").getBytes(UTF_8))
    val taskAdded = journal.all.head
    journal.entry(taskAdded.seq) shouldBe None
    journal.revertConflicts(Seq(taskAdded)) shouldBe Map(taskAdded.seq -> refusal.replace(s"Change ${added.seq}", s"Change ${taskAdded.seq}"))
    a[NotFoundException] should be thrownBy journal.revert(taskAdded.seq)
    // A batch goes on past both: neither stops it
    journal.revertAll(Seq(added.seq, taskAdded.seq)).map(_.getClass) shouldBe Seq(classOf[RevertOutcome.Skipped], classOf[RevertOutcome.Skipped])
  }
}
