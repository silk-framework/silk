package org.silkframework.serialization.json.changes

import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.util.FileUtils._
import org.silkframework.util.Identifier
import org.silkframework.workspace.changes.{ChangeEntry, FileState, ResourceDeleted, UnreadableChange}

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path}
import java.time.Instant
import java.util.concurrent.CountDownLatch
import scala.jdk.CollectionConverters._

/**
  * The file store on its own: files, cap, damage and transfer. Payloads without project content are enough for
  * that; the journal's behaviour against the store is [[FileChangeJournalTest]].
  */
class FileChangeJournalStoreTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  behavior of "FileChangeJournalStore"

  private implicit val context: PluginContext = PluginContext.empty

  private val directory = Files.createTempDirectory("fileChangeJournalStore")

  private val now = Instant.parse("2026-10-07T10:12:03Z")

  override protected def afterAll(): Unit = {
    directory.toFile.deleteRecursive()
  }

  private def store(name: String, maxSizeInMB: Int = 10): FileChangeJournalStore = {
    FileChangeJournalStore(directory.resolve(name).toString, maxSizeInMB)
  }

  /** An entry whose payload needs no project to read; `size` pads the path for the size tests. */
  private def entry(seq: Int, size: Int = 1): ChangeEntry = {
    ChangeEntry(seq, now, Some("urn:elds-backend-users:alice"), None, ResourceDeleted("f" * size, FileState(Some(seq.toLong), None)))
  }

  private def append(store: FileChangeJournalStore, project: Identifier, seqs: Range, size: Int = 1): Unit = {
    for(seq <- seqs) store.append(project, entry(seq, size))
  }

  private def projectDir(storeName: String, project: String): Path = directory.resolve(storeName).resolve(project)

  private def files(storeName: String, project: String): Seq[String] = {
    val dir = projectDir(storeName, project)
    if(Files.isDirectory(dir)) Files.list(dir).iterator().asScala.map(_.getFileName.toString).toSeq.sorted else Seq.empty
  }

  it should "keep the entries and the watermark across instances on the same directory" in {
    val first = store("restart")
    append(first, "p", 1 to 3)
    first.setReviewedUpTo("p", 2)
    first.entries("p").map(_.seq) shouldBe Seq(1, 2, 3)
    files("restart", "p") shouldBe Seq("000000001.jsonl", "reviewed.json")

    // A second instance answers the latest seq and the watermark without loading, then loads on the first read
    val second = store("restart")
    second.latestSeq("p") shouldBe 3
    second.reviewedUpTo("p") shouldBe 2
    second.entries("p") shouldBe first.entries("p")
    second.append("p", entry(4))
    second.entries("p").map(_.seq) shouldBe Seq(1, 2, 3, 4)
    second.latestSeq("p") shouldBe 4
    // A seq out of order would break the position-is-seq rule of the segments
    an[IllegalArgumentException] should be thrownBy second.append("p", entry(6))
  }

  it should "roll segments at a tenth of the cap and drop the oldest whole segments beyond it, never the newest" in {
    val cap = 1024 * 1024
    val s = store("cap", maxSizeInMB = 1)
    s.append("p", entry(1, size = 30 * 1024))
    val lineSize = Files.size(projectDir("cap", "p").resolve("000000001.jsonl"))
    // A new segment starts once the newest has reached the tenth, checked before the append
    val perSegment = math.ceil(cap / 10.0 / lineSize).toInt
    append(s, "p", 2 to 2 * perSegment, size = 30 * 1024)
    files("cap", "p") shouldBe Seq("000000001.jsonl", f"${perSegment + 1}%09d.jsonl")

    // Beyond the cap, whole segments go, oldest first, until the rest fits; the kept seqs stay contiguous
    append(s, "p", 2 * perSegment + 1 to 60, size = 30 * 1024)
    val kept = s.entries("p").map(_.seq)
    kept shouldBe (kept.head to 60)
    (kept.head - 1) % perSegment shouldBe 0
    val size = files("cap", "p").map(file => Files.size(projectDir("cap", "p").resolve(file))).sum
    size should be <= cap.toLong
    size + perSegment * lineSize should be > cap.toLong
    s.latestSeq("p") shouldBe 60
    store("cap", maxSizeInMB = 1).entries("p").map(_.seq) shouldBe kept

    // The newest segment stays even when it alone exceeds the cap; the one before it goes
    s.append("q", entry(1, size = 2 * cap))
    s.entries("q").map(_.seq) shouldBe Seq(1)
    s.append("q", entry(2))
    s.entries("q").map(_.seq) shouldBe Seq(2)
    files("cap", "q") shouldBe Seq("000000002.jsonl")
  }

  it should "cut an unfinished last line, skip lines it cannot read and keep the seqs after them" in {
    val s = store("damage")
    append(s, "p", 1 to 4)
    val segment = projectDir("damage", "p").resolve("000000001.jsonl")
    val lines = Files.readAllLines(segment, UTF_8).asScala
    // Line 2 has a payload that cannot be read, line 3 is not JSON, line 4 carries the seq of line 2, and a cut-short append follows
    val damaged = Seq(lines(0), lines(1).replace("\"type\":\"ResourceDeleted\"", "\"type\":\"Bogus\""), "{not json", lines(1)).mkString("\n") + "\n" + lines(0).take(20)
    Files.write(segment, damaged.getBytes(UTF_8))
    Files.write(projectDir("damage", "p").resolve("reviewed.json.tmp"), "{}".getBytes(UTF_8))
    Files.write(projectDir("damage", "p").resolve("notes.txt"), "kept".getBytes(UTF_8))

    val reloaded = store("damage")
    reloaded.latestSeq("p") shouldBe 4
    reloaded.entries("p").map(_.seq) shouldBe Seq(1, 2)
    reloaded.entries("p").map(_.change)(1) shouldBe UnreadableChange("Bogus", entry(2).change.summary)
    // The fragment is gone and the next entry starts a proper line; the stray file is left alone
    files("damage", "p") shouldBe Seq("000000001.jsonl", "notes.txt")
    reloaded.append("p", entry(5))
    store("damage").entries("p").map(_.seq) shouldBe Seq(1, 2, 5)
  }

  it should "remove a project's files and keep its monitor" in {
    val s = store("remove")
    append(s, "p", 1 to 2)
    s.setReviewedUpTo("p", 1)
    val monitor = s.monitor("p")
    s.remove("p")
    Files.exists(projectDir("remove", "p")) shouldBe false
    s.entries("p") shouldBe empty
    s.latestSeq("p") shouldBe 0
    s.reviewedUpTo("p") shouldBe 0
    s.monitor("p") should be theSameInstanceAs monitor
    s.monitor("q") should not be theSameInstanceAs(monitor)
  }

  it should "export its files as they are and replace a project's journal on import" in {
    val s = store("transfer")
    append(s, "p", 1 to 3)
    s.setReviewedUpTo("p", 2)
    val archive = InMemoryResourceManager()
    s.exportJournal("p", archive)
    archive.list.sorted shouldBe Seq("000000001.jsonl", "reviewed.json")
    archive.get("000000001.jsonl").loadAsBytes shouldBe Files.readAllBytes(projectDir("transfer", "p").resolve("000000001.jsonl"))

    append(s, "q", 1 to 5)
    s.importJournal("q", archive)
    s.entries("q") shouldBe s.entries("p")
    s.latestSeq("q") shouldBe 3
    s.reviewedUpTo("q") shouldBe 2
    // An archive without a journal leaves the project empty, and a project without a journal exports nothing
    s.importJournal("q", InMemoryResourceManager())
    s.entries("q") shouldBe empty
    s.latestSeq("q") shouldBe 0
    val target = InMemoryResourceManager()
    s.exportJournal("none", target)
    target.list shouldBe empty
  }

  it should "let an append to one project complete while a thread holds the monitor of another" in {
    val s = store("locks")
    val held = new CountDownLatch(1)
    val release = new CountDownLatch(1)
    val holder = new Thread(() => s.monitor("a").synchronized { held.countDown(); release.await() })
    holder.setDaemon(true)
    holder.start()
    held.await()
    try {
      val appender = new Thread(() => s.append("b", entry(1)))
      appender.start()
      appender.join(5000)
      appender.isAlive shouldBe false
      s.entries("b").map(_.seq) shouldBe Seq(1)
    } finally {
      release.countDown()
    }
  }
}
