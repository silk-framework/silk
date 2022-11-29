package org.silkframework.runtime.resource

import java.nio.file.{Files, Path}
import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConverters
import scala.util.Try

/** Resource manager with a fixed map of files. */
case class FileMapResourceManager(baseDir: Path,
                                  files: Map[String, Path],
                                  removeFilesOnGc: Boolean) extends ResourceManager with DoSomethingOnGC {
  private val fileMap: ConcurrentHashMap[String, FileResource] = {
    val m = new ConcurrentHashMap[String, FileResource]()
    files.foreach { case (fileName, path) =>
      val resource = FileResource(path.toFile)
      if(removeFilesOnGc) {
        // Make sure that files are removed when this resource manager has been garbage collected
        resource.setDeleteOnGC(true)
      }
      m.put(fileName, resource)
    }
    m
  }
  /** No nested resources! */
  override def child(name: String): ResourceManager = throw new UnsupportedOperationException("A FileMapResourceManager does not support child operator.")

  override def parent: Option[ResourceManager] = None

  override def get(name: String, mustExist: Boolean): WritableResource = synchronized {
    Option(fileMap.get(name)) match {
      case None if mustExist =>
        throw new ResourceNotFoundException(s"Resource $name not found in file map resource manager. Available files: ${list.mkString(", ")}")
      case None =>
        val newPath = Path.of(baseDir.toString, name).normalize()
        val resource = if(!newPath.startsWith(baseDir)) {
          throw new IllegalArgumentException(s"Tried to break out of resource manager with base dir: $baseDir, with requested file: $name")
        } else {
          FileResource(newPath.toFile)
        }
        if(removeFilesOnGc) {
          resource.setDeleteOnGC(true)
        }
        fileMap.put(name, resource)
        resource
      case Some(fileResource) =>
        fileResource
    }
  }

  override def delete(name: String): Unit = synchronized {
    Option(fileMap.get(name)).foreach(file => {
      file.delete()
    })
    fileMap.remove(name)
  }

  /**
    * The full path of this resource loader.
    */
  override def basePath: String = baseDir.toString

  override def list: List[String] = JavaConverters.asScalaIterator(fileMap.keys().asIterator()).toList.sorted

  override def listChildren: List[String] = List.empty

  override def finalAction(): Unit = {
    if(removeFilesOnGc) {
      JavaConverters.asScalaIterator(fileMap.values().iterator()).foreach { file =>
        Try(file.delete())
      }
      Try(Files.deleteIfExists(baseDir))
    }
  }
}
