package org.silkframework.serialization.json.changes

import org.silkframework.util.FileUtils._
import org.silkframework.workspace.changes.ChangeJournalTestTrait

import java.nio.file.Files

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
}
