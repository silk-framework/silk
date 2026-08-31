package org.silkframework.workspace.resources

import org.silkframework.runtime.resource.{ResourceManager, WritableResource}
import org.silkframework.workspace.changes.{Change, ChangeJournal, FileState, ResourceCreated, ResourceDeleted, ResourceOverwritten}

import java.io.{File, InputStream, OutputStream}
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

/**
  * The resources of a project, recording every write and deletion made on behalf of a request in its change journal.
  * The writes of an activity, e.g. a workflow run, are its effect and represented by the activity's own entry.
  *
  * @param prefix The path of this manager relative to the project resources, ending in '/' unless it is the root.
  */
class JournalingResourceManager(private val underlying: ResourceManager, journal: ChangeJournal, prefix: String = "",
                                parentManager: Option[JournalingResourceManager] = None) extends ResourceManager {

  override def basePath: String = underlying.basePath

  override def list: List[String] = underlying.list

  override def listChildren: List[String] = underlying.listChildren

  override def child(name: String): ResourceManager = {
    new JournalingResourceManager(underlying.child(name), journal, prefix + name + "/", Some(this))
  }

  /** The project is the boundary of the journal, so the root manager exposes no parent outside it. */
  override def parent: Option[ResourceManager] = parentManager

  override def get(name: String, mustExist: Boolean): WritableResource = {
    new JournalingResource(underlying.get(name, mustExist), journal, prefix + name)
  }

  /** Deletes a resource or a child directory. Each file goes through its resource, so each deletion is recorded. */
  override def delete(name: String): Unit = {
    if(underlying.list.contains(name)) {
      get(name).delete()
    } else {
      if(underlying.listChildren.contains(name)) {
        val directory = child(name)
        directory.listRecursive.foreach(path => directory.getInPath(path).delete())
      }
      // Removes the directory itself, which holds no files anymore, or a name that is not listed.
      underlying.delete(name)
    }
  }

  override def close(): Unit = underlying.close()

  /** Compares by the wrapped manager, as wrapping must not turn value equality into identity. */
  override def equals(obj: Any): Boolean = obj match {
    case other: JournalingResourceManager => underlying == other.underlying
    case _ => false
  }

  override def hashCode(): Int = underlying.hashCode()
}

/** A resource whose writes and deletion are recorded. */
private class JournalingResource(private val underlying: WritableResource, journal: ChangeJournal,
                                 journalPath: String) extends WritableResource {

  override def name: String = underlying.name

  override def path: String = underlying.path

  override def entryPath: Option[String] = underlying.entryPath

  override def underlyingFile: Option[File] = underlying.underlyingFile

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

  /** Compares by the wrapped resource, as wrapping must not change how tasks that hold resources compare. */
  override def equals(obj: Any): Boolean = obj match {
    case other: JournalingResource => underlying == other.underlying
    case _ => false
  }

  override def hashCode(): Int = underlying.hashCode()

  private def record(change: Change): Unit = {
    ChangeJournal.requestUserContext.foreach(user => journal.record(change)(user))
  }

  /** Records the write once the stream is closed, i.e. the file is in its new state. */
  private class RecordingOutputStream(out: OutputStream, before: Option[FileState]) extends OutputStream {

    private val closed = new AtomicBoolean(false)

    override def write(b: Int): Unit = out.write(b)

    override def write(b: Array[Byte]): Unit = out.write(b)

    override def write(b: Array[Byte], off: Int, len: Int): Unit = out.write(b, off, len)

    override def flush(): Unit = out.flush()

    override def close(): Unit = {
      if(closed.compareAndSet(false, true)) {
        // Recorded even if closing fails, as the file has been written to nevertheless.
        try {
          out.close()
        } finally {
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
