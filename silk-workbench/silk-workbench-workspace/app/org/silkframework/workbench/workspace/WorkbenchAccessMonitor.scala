package org.silkframework.workbench.workspace

import java.util
import java.util.logging.Logger

import javax.inject.Singleton
import org.silkframework.runtime.activity.UserContext

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * Stores project and task accesses by users, e.g. used for "recently viewed items".
  */
@Singleton
class WorkbenchAccessMonitor {
  private val log: Logger = Logger.getLogger(this.getClass.getName)
  private val userAccessMap = new mutable.HashMap[String, MostRecentlyOrderedList[WorkspaceItem]]()
  final val RECENTLY_VIEWED_ITEMS_MAX = 50

  /** Saves a direct project access */
  def saveProjectAccess(projectId: String)
                       (implicit userContext: UserContext): Unit = synchronized {
    addItem(WorkspaceProject(projectId))
  }

  /** Saves a direct task access. */
  def saveProjectTaskAccess(projectId: String,
                            taskId: String)
                           (implicit userContext: UserContext): Unit = synchronized {
    addItem(WorkspaceTask(projectId, taskId))
  }

  /** Fetch the most recently viewed items of a user.
    * Most recent item comes last, least recent comes first. */
  def getAccessItems(implicit userContext: UserContext): Seq[WorkspaceItem] = {
    userAccessMap.get(userId).map(_.items()).getOrElse(Seq.empty)
  }

  private def addItem(workspaceItem: WorkspaceItem)
                     (implicit userContext: UserContext): Unit = {
    val accessList = userAccessMap.getOrElseUpdate(userId, MostRecentlyOrderedList[WorkspaceItem](RECENTLY_VIEWED_ITEMS_MAX))
    accessList.add(workspaceItem)
    log.fine(s"Saved access user '$userId' to $workspaceItem.")
  }

  private def userId(implicit userContext: UserContext): String = {
    val userId = userContext.user.map(_.uri).getOrElse("  ANONYMOUS  ")
    userId
  }
}

/** Possible items that are stored in the most recently visited list. */
sealed trait WorkspaceItem {
  def projectId: String

  def taskIdOpt: Option[String]
}

case class WorkspaceProject(projectId: String) extends WorkspaceItem {
  override def toString: String = s"project '$projectId'"

  override def taskIdOpt: Option[String] = None
}

case class WorkspaceTask(projectId: String, taskId: String) extends WorkspaceItem {
  override def toString: String = {s"task '$taskId' of project '$projectId'"}

  override def taskIdOpt: Option[String] = Some(taskId)
}

/** A list that keeps its element sorted by how recent they have been added.
  * If the list exceeds its capacity it will remove it's least recently added item.
  * Re-adding the same item will put it into the most-recently-added position. */
case class MostRecentlyOrderedList[T](capacity: Int) {
  // stores the items in an ordered way
  private val linkedHashMap = new util.LinkedHashMap[T, T] {
    override def removeEldestEntry(eldest: util.Map.Entry[T, T]): Boolean = {
      this.size() > capacity
    }
  }

  def add(item: T): Unit = synchronized {
    if(linkedHashMap.containsKey(item)) {
      linkedHashMap.remove(item)
    }
    linkedHashMap.put(item, item)
  }

  def remove(item: T): Unit = synchronized {
    linkedHashMap.remove(item)
  }

  /** Most recent item comes last, least recent comes first. */
  def items(): Seq[T] = synchronized {
    linkedHashMap.values().asScala.toSeq
  }

  def size(): Int = synchronized {
    linkedHashMap.size()
  }
}