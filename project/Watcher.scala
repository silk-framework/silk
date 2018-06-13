import java.io.File

import scala.collection.mutable.HashMap

object Watcher {
  private val watches = new HashMap[WatchConfig, WatchData]()

  /** Checks if files have changed since last check and updates watch data */
  def filesChanged(watchConfig: WatchConfig): Boolean = {
    val changed = !watches.contains(watchConfig) // TODO: Only returns true on first run with the same watch config, do real file change check
    watches.put(watchConfig, WatchData(Map()))
    changed
  }
}

/** The config for the files to be watched
  *
  * @param directory   The base directory, files are watched recursively under this directory
  * @param fileRegexes Regexes for the files to be watched
  **/
case class WatchConfig(directory: File, fileRegexes: Seq[String])

case class WatchData(files: Map[File, FileInfo])

case class FileInfo(modifiedTimestamp: Long)