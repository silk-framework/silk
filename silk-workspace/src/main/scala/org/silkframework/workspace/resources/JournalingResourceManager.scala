package org.silkframework.workspace.resources

import org.silkframework.runtime.resource.{ResourceManager, WritableResource}
import org.silkframework.workspace.changes.{Change, ChangeJournal, FileState, ResourceCreated, ResourceDeleted, ResourceOverwritten}

import java.io.{InputStream, OutputStream}
import java.time.Instant

/**
  * The resources of a project, recording every write and deletion in its change journal.
  *
  * @param prefix The path of this manager relative to the project resources, ending in '/' unless it is the root.
  */
class JournalingResourceManager(underlying: ResourceManager, journal: ChangeJournal, prefix: String = "",
                                parentManager: Option[JournalingResourceManager] = None) extends ResourceManager {

  override def basePath: String = underlying.basePath

  override def list: List[String] = underlying.list

  override def listChildren: List[String] = underlying.listChildren

  override def child(name: String): ResourceManager = {
    new JournalingResourceManager(underlying.child(name), journal, prefix + name + "/", Some(this))
  }

  override def parent: Option[ResourceManager] = parentManager.orElse(underlying.parent)

  override def get(name: String, mustExist: Boolean): WritableResource = {
    new JournalingResource(underlying.get(name, mustExist), prefix + name)
  }

  /** Deletes a resource or a child directory. Each file goes through its resource, so each deletion is recorded. */
  override def delete(name: String): Unit = {
    if(list.contains(name)) {
      get(name).delete()
    }
    if(listChildren.contains(name)) {
      val directory = child(name)
      directory.listRecursive.foreach(path => directory.getInPath(path).delete())
    }
    underlying.delete(name)
  }

  override def close(): Unit = underlying.close()

  /** A resource whose writes and deletion are recorded. */
  private class JournalingResource(underlying: WritableResource, journalPath: String) extends WritableResource {

    override def name: String = underlying.name

    override def path: String = underlying.path

    override def entryPath: Option[String] = underlying.entryPath

    override def exists: Boolean = underlying.exists

    override def size: Option[Long] = underlying.size

    override def modificationTime: Option[Instant] = underlying.modificationTime

    override def inputStream: InputStream = underlying.inputStream

    override def createOutputStream(append: Boolean): OutputStream = {
      // Taken before the stream opens, as opening creates or truncates the file.
      val before = FileState.of(underlying)
      new RecordingOutputStream(underlying.createOutputStream(append), before)
    }

    override def delete(): Unit = {
      val before = FileState.of(underlying)
      underlying.delete()
      before.foreach(state => record(ResourceDeleted(journalPath, state)))
    }

    private def record(change: Change): Unit = journal.record(change)(ChangeJournal.requestUserContext)

    /** Records the write once the stream is closed, i.e. the file is in its new state. */
    private class RecordingOutputStream(out: OutputStream, before: Option[FileState]) extends OutputStream {

      private var closed = false

      override def write(b: Int): Unit = out.write(b)

      override def write(b: Array[Byte]): Unit = out.write(b)

      override def write(b: Array[Byte], off: Int, len: Int): Unit = out.write(b, off, len)

      override def flush(): Unit = out.flush()

      override def close(): Unit = {
        if(!closed) {
          closed = true
          out.close()
          for(after <- FileState.of(underlying)) {
            record(before match {
              case Some(previous) => ResourceOverwritten(journalPath, previous, after)
              case None => ResourceCreated(journalPath, after)
            })
          }
        }
      }
    }
  }
}
