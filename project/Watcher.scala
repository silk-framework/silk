import java.io.File

import scala.collection.mutable
import scala.collection.mutable.HashMap
import scala.util.matching.Regex

/** Watches files for being modified */
object Watcher {
  private val watches = new HashMap[WatchConfig, WatchData]()

  /** Checks if files have changed since last check and updates watch data
    * @return The files that are not registered yet or have been modified, since the last check. */
  def filesChanged(watchConfig: WatchConfig): Iterable[File] = {
    val watchData = fetchWatchData(watchConfig)
    val changedFiles = mutable.HashSet.empty[File]

    watches.get(watchConfig) match {
      case Some(oldWatchData) =>
        if(watchData != oldWatchData) {
          watches.put(watchConfig, watchData)
          watchData.files.keys filter { file =>
            oldWatchData.files.get(file) match {
              case Some(oldFileInfo) =>
                watchData.files(file).modifiedTimestamp != oldFileInfo.modifiedTimestamp
              case None =>
                true // not existing, yet, so add
            }
          }
        } else {
          Seq.empty
        }
      case None =>
        watches.put(watchConfig, watchData)
        watchData.files.keys
    }
  }

  private def fetchWatchData(watchConfig: WatchConfig): WatchData = {
    val fileInfos = fetchFileInfos(watchConfig)
    WatchData(fileInfos)
  }

  private def fetchFileInfos(watchConfig: WatchConfig): Map[File, FileInfo] = {
    val files = fetchFileInfosRecursive(watchConfig.regex, watchConfig.directory)
    files map (file => {
      (
        file,
        FileInfo(file.lastModified())
      )
    }) toMap
  }

  private def fetchFileInfosRecursive(fileRegex: Regex, baseFile: File): Seq[File] = {
    val files = for(file <- baseFile.listFiles().toSeq) yield {
      if(file.isDirectory) {
        fetchFileInfosRecursive(fileRegex, file)
      } else {
        if(fileRegex.findFirstIn(file.getName).isDefined) {
          Seq(file)
        } else {
          Seq.empty
        }
      }
    }
    files.flatten
  }
}

/** The config for the files to be watched
  *
  * @param directory The base directory, files are watched recursively under this directory
  * @param fileRegex Regex for the files to be watched
  * */
case class WatchConfig(directory: File, fileRegex: String) {
  lazy val regex: Regex = fileRegex.r
}

case class WatchData(files: Map[File, FileInfo])

case class FileInfo(modifiedTimestamp: Long)