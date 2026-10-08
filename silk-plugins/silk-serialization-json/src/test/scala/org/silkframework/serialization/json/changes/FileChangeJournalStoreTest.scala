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

  // The size at which a segment rolls, whatever the cap
  private val segmentSize = 1024 * 1024

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

  private def seqs(store: FileChangeJournalStore, project: Identifier): Seq[Int] = store.headers(project).map(_.seq)

  private def projectDir(storeName: String, project: String): Path = directory.resolve(storeName).resolve(project)

  private def files(storeName: String, project: String): Seq[String] = {
    val dir = projectDir(storeName, project)
    if(Files.isDirectory(dir)) Files.list(dir).iterator().asScala.map(_.getFileName.toString).toSeq.sorted else Seq.empty
  }

  it should "keep the entries and the watermark across instances on the same directory" in {
    val first = store("restart")
    append(first, "p", 1 to 3)
    first.setReviewedUpTo("p", 2)
    seqs(first, "p") shouldBe Seq(1, 2, 3)
    files("restart", "p") shouldBe Seq("000000001.jsonl", "reviewed.json")

    // A second instance answers the latest seq and the watermark without loading, then loads the headers on the first read
    val second = store("restart")
    second.latestSeq("p") shouldBe 3
    second.reviewedUpTo("p") shouldBe 2
    second.headers("p") shouldBe first.headers("p")
    // An entry is read from its segment when asked for; a seq the journal does not hold answers nothing
    second.entry("p", 2) shouldBe Some(entry(2))
    second.entry("p", 4) shouldBe None
    second.entry("p", 0) shouldBe None
    second.append("p", entry(4))
    seqs(second, "p") shouldBe Seq(1, 2, 3, 4)
    second.latestSeq("p") shouldBe 4
    second.entry("p", 4) shouldBe Some(entry(4))
    // A seq out of order would break the position-is-seq rule of the segments
    an[IllegalArgumentException] should be thrownBy second.append("p", entry(6))
  }

  it should "roll segments at a tenth of the cap, at most 1 MB, and drop the oldest whole segments beyond the cap, never the newest" in {
    val cap = 1024 * 1024
    val s = store("cap", maxSizeInMB = 1)
    s.append("p", entry(1, size = 30 * 1024))
    val lineSize = Files.size(projectDir("cap", "p").resolve("000000001.jsonl"))
    // A new segment starts once the newest has reached a tenth of this small cap, checked before the append
    val perSegment = math.ceil(cap / 10.0 / lineSize).toInt
    append(s, "p", 2 to 2 * perSegment, size = 30 * 1024)
    files("cap", "p") shouldBe Seq("000000001.jsonl", f"${perSegment + 1}%09d.jsonl")
    // An entry is read from the segment that holds it
    s.entry("p", perSegment + 1) shouldBe Some(entry(perSegment + 1, size = 30 * 1024))

    // Beyond the cap, whole segments go, oldest first, until the rest fits: the kept seqs stay contiguous and the
    // journal keeps at least nine tenths of the cap
    append(s, "p", 2 * perSegment + 1 to 60, size = 30 * 1024)
    val kept = seqs(s, "p")
    kept shouldBe (kept.head to 60)
    (kept.head - 1) % perSegment shouldBe 0
    val size = files("cap", "p").map(file => Files.size(projectDir("cap", "p").resolve(file))).sum
    size should be <= cap.toLong
    size + perSegment * lineSize should be > cap.toLong
    s.latestSeq("p") shouldBe 60
    seqs(store("cap", maxSizeInMB = 1), "p") shouldBe kept
    s.entry("p", 1) shouldBe None

    // The newest segment stays even when it alone exceeds the cap; the one before it goes
    s.append("q", entry(1, size = 2 * cap))
    seqs(s, "q") shouldBe Seq(1)
    s.append("q", entry(2))
    seqs(s, "q") shouldBe Seq(2)
    files("cap", "q") shouldBe Seq("000000002.jsonl")

    // With a large cap a segment rolls at 1 MB, so reading one entry reads about that much
    val large = store("largeCap", maxSizeInMB = 20)
    val perLargeSegment = math.ceil(segmentSize / lineSize.toDouble).toInt
    append(large, "p", 1 to 2 * perLargeSegment, size = 30 * 1024)
    files("largeCap", "p") shouldBe Seq("000000001.jsonl", f"${perLargeSegment + 1}%09d.jsonl")
  }

  it should "cut an unfinished last line, skip lines it cannot read and keep the seqs after them" in {
    val s = store("damage")
    append(s, "p", 1 to 4)
    val segment = projectDir("damage", "p").resolve("000000001.jsonl")
    val lines = Files.readAllLines(segment, UTF_8).asScala
    // Line 2 has a change that cannot be read, line 3 is not JSON, line 4 carries the seq of line 2, and a cut-short append follows
    val damaged = Seq(lines(0), lines(1).replace("\"type\":\"ResourceDeleted\"", "\"type\":\"Bogus\""), "{not json", lines(1)).mkString("\n") + "\n" + lines(0).take(20)
    Files.write(segment, damaged.getBytes(UTF_8))
    Files.write(projectDir("damage", "p").resolve("reviewed.json.tmp"), "{}".getBytes(UTF_8))
    Files.write(projectDir("damage", "p").resolve("notes.txt"), "kept".getBytes(UTF_8))

    val reloaded = store("damage")
    reloaded.latestSeq("p") shouldBe 4
    seqs(reloaded, "p") shouldBe Seq(1, 2)
    // The header of line 2 is read as stored, the load does not look at the change; the entry carries the placeholder
    reloaded.headers("p")(1).summary shouldBe entry(2).change.summary
    reloaded.entry("p", 2).map(_.change) shouldBe Some(UnreadableChange("Bogus", entry(2).change.summary))
    // The skipped lines are gaps
    reloaded.entry("p", 3) shouldBe None
    reloaded.entry("p", 4) shouldBe None
    // The fragment is gone and the next entry starts a proper line; the stray file is left alone
    files("damage", "p") shouldBe Seq("000000001.jsonl", "notes.txt")
    reloaded.append("p", entry(5))
    seqs(store("damage"), "p") shouldBe Seq(1, 2, 5)
    store("damage").entry("p", 5) shouldBe Some(entry(5))
  }

  it should "remove a project's files and keep its monitor" in {
    val s = store("remove")
    append(s, "p", 1 to 2)
    s.setReviewedUpTo("p", 1)
    val monitor = s.monitor("p")
    s.remove("p")
    Files.exists(projectDir("remove", "p")) shouldBe false
    s.headers("p") shouldBe empty
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
    s.headers("q") shouldBe s.headers("p")
    s.entry("q", 3) shouldBe s.entry("p", 3)
    s.latestSeq("q") shouldBe 3
    s.reviewedUpTo("q") shouldBe 2
    // An archive without a journal leaves the project empty, and a project without a journal exports nothing
    s.importJournal("q", InMemoryResourceManager())
    s.headers("q") shouldBe empty
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
      seqs(s, "b") shouldBe Seq(1)
    } finally {
      release.countDown()
    }
  }
}
