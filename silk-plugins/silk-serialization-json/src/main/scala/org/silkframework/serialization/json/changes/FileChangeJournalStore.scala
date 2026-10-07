package org.silkframework.serialization.json.changes

import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.plugin.{InvalidPluginParameterValueException, PluginContext}
import org.silkframework.runtime.resource.{ResourceLoader, ResourceManager}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.changes.ChangeJsonFormats.ChangeEntryJsonFormat
import org.silkframework.util.Identifier
import org.silkframework.workspace.changes.{Change, ChangeEntry, ChangeJournalStore}
import play.api.libs.json.{JsValue, Json}

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path, Paths, StandardCopyOption, StandardOpenOption}
import java.util.Comparator
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal

/**
  * Keeps the journal of each project in a folder of its own under `dir`: append-only segment files named after the
  * seq of their first entry, one entry per line, and `reviewed.json` with the watermark. The entries of a project are
  * parsed on first access and kept in memory; a segment holds about a tenth of the cap, and the oldest segments are
  * deleted while the project exceeds the cap. See the file store section of the feature spec for the layout and the
  * damage handling.
  */
@Plugin(
  id = "fileChangeJournal",
  label = "Changes on the file system",
  description = "Keeps the changes of each project in files under a directory, so they survive a restart and travel with project exports."
)
case class FileChangeJournalStore(@Param("The directory that holds a folder per project.")
                                  dir: String,
                                  @Param("Per project, the oldest changes are dropped beyond this size, unreviewed changes included.")
                                  maxSizeInMB: Int = 10) extends ChangeJournalStore {

  import FileChangeJournalStore._

  private val root: Path = Paths.get(dir).toAbsolutePath
  Files.createDirectories(root)
  if(!Files.isDirectory(root) || !Files.isWritable(root)) {
    throw new InvalidPluginParameterValueException(s"The change journal directory '$root' could not be created. " +
      "Make sure that the location is accessible and writable.")
  }
  if(maxSizeInMB < 1) {
    throw new InvalidPluginParameterValueException(s"The change journal size cap must be at least 1 MB, but is $maxSizeInMB.")
  }

  private val capBytes: Long = maxSizeInMB.toLong * 1024 * 1024

  // A new segment starts once the newest one has reached a tenth of the cap
  private val segmentBytes: Long = capBytes / 10

  // One lock object per project, never removed: a lock replaced while a thread holds it would guard nothing
  private val monitors = new ConcurrentHashMap[Identifier, AnyRef]()

  // The loaded projects; a project is loaded on the first access to its entries and dropped by remove and import
  private val journals = new ConcurrentHashMap[Identifier, Journal]()

  override def monitor(project: Identifier): AnyRef = monitors.computeIfAbsent(project, _ => new Object)

  override def append(project: Identifier, entry: ChangeEntry)(implicit context: PluginContext): Unit = monitor(project).synchronized {
    val journal = loaded(project)
    // The position of a line gives its seq, so the seqs must be contiguous
    require(entry.seq == journal.latestSeq + 1, s"Entry ${entry.seq} does not follow the latest entry ${journal.latestSeq} of project '$project'.")
    val line = (Json.stringify(ChangeEntryJsonFormat.write(entry)(WriteContext.fromPluginContext[JsValue]())) + "\n").getBytes(UTF_8)
    val (segment, olderSegments) = journal.segments.lastOption match {
      case Some(newest) if newest.size < segmentBytes => (newest, journal.segments.init)
      case _ =>
        Files.createDirectories(projectDir(project))
        (Segment(entry.seq, segmentFile(project, entry.seq), Vector.empty, 0, 0L), journal.segments)
    }
    Files.write(segment.file, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    val appended = segment.copy(entries = segment.entries :+ entry, lines = segment.lines + 1, size = segment.size + line.length)
    journals.put(project, capped(Journal(olderSegments :+ appended, journal.entries :+ entry)))
  }

  override def entries(project: Identifier)(implicit context: PluginContext): Seq[ChangeEntry] = monitor(project).synchronized {
    loaded(project).entries
  }

  /** Without a loaded journal, the name of the newest segment plus its number of lines; no payload is parsed. */
  override def latestSeq(project: Identifier): Int = monitor(project).synchronized {
    Option(journals.get(project)) match {
      case Some(journal) => journal.latestSeq
      case None => segmentFiles(project).lastOption.map(file => firstSeqOf(file) + countLines(file) - 1).getOrElse(0)
    }
  }

  override def reviewedUpTo(project: Identifier): Int = monitor(project).synchronized {
    val file = reviewedFile(project)
    if(Files.exists(file)) {
      try {
        (Json.parse(Files.readAllBytes(file)) \ REVIEWED_UP_TO).as[Int]
      } catch {
        case NonFatal(ex) =>
          log.warning(s"The reviewed watermark of project '$project' in '$file' cannot be read and counts as unset: ${ex.getMessage}")
          0
      }
    } else {
      0
    }
  }

  /** Written to a temporary file that is moved into place, so a crash leaves the old watermark or the new one. */
  override def setReviewedUpTo(project: Identifier, seq: Int): Unit = monitor(project).synchronized {
    val file = reviewedFile(project)
    Files.createDirectories(file.getParent)
    val tmp = file.resolveSibling(file.getFileName.toString + TMP_SUFFIX)
    Files.write(tmp, Json.stringify(Json.obj(REVIEWED_UP_TO -> seq)).getBytes(UTF_8))
    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
  }

  override def remove(project: Identifier): Unit = monitor(project).synchronized {
    journals.remove(project)
    deleteDirectory(projectDir(project))
  }

  /** A raw copy of the project's files, so entries this process cannot read travel unchanged. */
  override def exportJournal(project: Identifier, target: ResourceManager): Unit = monitor(project).synchronized {
    for(file <- journalFiles(project)) {
      target.get(file.getFileName.toString).writeBytes(Files.readAllBytes(file))
    }
  }

  /** Replaces the project's files with those of the source; the entries are parsed on the next access. */
  override def importJournal(project: Identifier, source: ResourceLoader): Unit = monitor(project).synchronized {
    journals.remove(project)
    val directory = projectDir(project)
    deleteDirectory(directory)
    val names = source.list.filter(name => isSegmentName(name) || name == REVIEWED_FILE)
    if(names.nonEmpty) {
      Files.createDirectories(directory)
      for(name <- names) {
        Files.write(directory.resolve(name), source.get(name).loadAsBytes)
      }
    }
  }

  private def loaded(project: Identifier)(implicit context: PluginContext): Journal = {
    Option(journals.get(project)).getOrElse {
      val journal = load(project)
      journals.put(project, journal)
      journal
    }
  }

  private def load(project: Identifier)(implicit context: PluginContext): Journal = {
    val directory = projectDir(project)
    if(!Files.isDirectory(directory)) {
      Journal(Vector.empty, Vector.empty)
    } else {
      implicit val readContext: ReadContext = ReadContext.fromPluginContext()(context)
      for(file <- listFiles(directory); name = file.getFileName.toString; if !isSegmentName(name) && name != REVIEWED_FILE) {
        if(name == REVIEWED_FILE + TMP_SUFFIX) {
          // Left by a crash while the watermark was written; the old watermark is still in place
          Files.delete(file)
        } else {
          log.warning(s"The file '$file' in the change journal of project '$project' is neither a segment nor the watermark and is ignored.")
        }
      }
      val files = segmentFiles(project)
      val segments = for((file, index) <- files.zipWithIndex) yield readSegment(project, file, newest = index == files.size - 1)
      val journal = Journal.of(segments.filter(_.lines > 0).toVector)
      log.fine(s"Loaded ${journal.entries.size} changes of project '$project' from ${files.size} segment files.")
      journal
    }
  }

  private def readSegment(project: Identifier, file: Path, newest: Boolean)(implicit readContext: ReadContext): Segment = {
    val firstSeq = firstSeqOf(file)
    val bytes = Files.readAllBytes(file)
    // Whatever follows the last newline is not a line: an append that was cut short
    val end = bytes.lastIndexOf('\n'.toByte) + 1
    if(end < bytes.length) {
      if(newest) {
        log.warning(s"The change journal segment '$file' of project '$project' ends with an unfinished line, which is removed.")
        if(end == 0) Files.delete(file) else Files.write(file, bytes.take(end))
      } else {
        log.warning(s"The change journal segment '$file' of project '$project' ends with an unfinished line, which is ignored.")
      }
    }
    // Every line counts, an empty one too, so the position of a line gives its seq
    val lines = if(end == 0) Array.empty[String] else new String(bytes, 0, end, UTF_8).split("\n", -1).dropRight(1)
    val entries = for((line, index) <- lines.zipWithIndex; entry <- readLine(project, firstSeq + index, line)) yield entry
    Segment(firstSeq, file, entries.toVector, lines.length, end.toLong)
  }

  /** None for a line whose envelope cannot be read or whose seq is not its position; it keeps its seq as a gap. */
  private def readLine(project: Identifier, seq: Int, line: String)(implicit readContext: ReadContext): Option[ChangeEntry] = {
    try {
      val entry = ChangeEntryJsonFormat.read(Json.parse(line))
      if(entry.seq == seq) {
        Some(entry)
      } else {
        log.warning(s"Change $seq of project '$project' is skipped: its line carries seq ${entry.seq}.")
        None
      }
    } catch {
      case NonFatal(ex) =>
        log.warning(s"Change $seq of project '$project' is skipped, its line cannot be read: ${Change.reason(ex)}")
        None
    }
  }

  // While over the cap and more than one segment exists, the oldest segment goes; the newest always stays
  private def capped(journal: Journal): Journal = {
    var segments = journal.segments
    while(segments.size > 1 && segments.map(_.size).sum > capBytes) {
      Files.deleteIfExists(segments.head.file)
      segments = segments.tail
    }
    if(segments.size == journal.segments.size) journal else Journal.of(segments)
  }

  private def projectDir(project: Identifier): Path = root.resolve(project.toString)

  private def reviewedFile(project: Identifier): Path = projectDir(project).resolve(REVIEWED_FILE)

  private def segmentFile(project: Identifier, firstSeq: Int): Path = projectDir(project).resolve(f"$firstSeq%09d$SEGMENT_SUFFIX")

  private def firstSeqOf(file: Path): Int = file.getFileName.toString.stripSuffix(SEGMENT_SUFFIX).toInt

  /** The segment files of a project, oldest first; empty if the project has no folder. */
  private def segmentFiles(project: Identifier): Seq[Path] = {
    listFiles(projectDir(project)).filter(file => isSegmentName(file.getFileName.toString)).sortBy(_.getFileName.toString)
  }

  /** The segments and the watermark file, the files an export carries. */
  private def journalFiles(project: Identifier): Seq[Path] = {
    segmentFiles(project) ++ Some(reviewedFile(project)).filter(Files.exists(_))
  }

  private def listFiles(directory: Path): Seq[Path] = {
    if(Files.isDirectory(directory)) {
      val stream = Files.list(directory)
      try {
        stream.iterator().asScala.filter(Files.isRegularFile(_)).toVector
      } finally {
        stream.close()
      }
    } else {
      Seq.empty
    }
  }

  private def countLines(file: Path): Int = Files.readAllBytes(file).count(_ == '\n'.toByte)

  private def deleteDirectory(directory: Path): Unit = {
    if(Files.exists(directory)) {
      val stream = Files.walk(directory)
      try {
        stream.sorted(Comparator.reverseOrder[Path]()).forEach(path => Files.delete(path))
      } finally {
        stream.close()
      }
    }
  }
}

object FileChangeJournalStore {

  private val log = Logger.getLogger(classOf[FileChangeJournalStore].getName)

  private val SEGMENT_SUFFIX = ".jsonl"

  private val REVIEWED_FILE = "reviewed.json"

  private val REVIEWED_UP_TO = "reviewedUpTo"

  private val TMP_SUFFIX = ".tmp"

  private val segmentName = """\d{9}\.jsonl""".r

  private def isSegmentName(name: String): Boolean = segmentName.matches(name)

  /** A segment file: its first seq, its parsed entries and, as every line counts, its number of lines and bytes. */
  private case class Segment(firstSeq: Int, file: Path, entries: Vector[ChangeEntry], lines: Int, size: Long) {
    def lastSeq: Int = firstSeq + lines - 1
  }

  /** A loaded project: its segments, oldest first, and their entries in one sequence. */
  private case class Journal(segments: Vector[Segment], entries: Vector[ChangeEntry]) {
    def latestSeq: Int = segments.lastOption.map(_.lastSeq).getOrElse(0)
  }

  private object Journal {
    def of(segments: Vector[Segment]): Journal = Journal(segments, segments.flatMap(_.entries))
  }
}
