package org.silkframework.serialization.json.changes

import org.silkframework.workspace.changes.ChangeJournalTestTrait

import java.nio.file.Files
import java.util.Comparator

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
      val files = Files.walk(directory)
      try {
        files.sorted(Comparator.reverseOrder()).forEach(path => Files.delete(path))
      } finally {
        files.close()
      }
    }
  }
}
