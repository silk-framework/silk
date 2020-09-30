package org.silkframework.dataset
import org.silkframework.config.{Prefixes, Task}
import org.silkframework.entity.{EntitySchema, Link}
import org.silkframework.entity.paths.TypedPath
import org.silkframework.execution.EntityHolder
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Uri

/**
  * DataSource object that is returned when the application is in safe-mode and operations on a data source must not
  * be performed.
  */
object SafeModeDataSource extends DataSource {
  override def underlyingTask: Task[DatasetSpec[Dataset]] = SafeModeException.throwSafeModeException()

  override def retrieveTypes(limit: Option[Int])(implicit userContext: UserContext): Traversable[(String, Double)] = SafeModeException.throwSafeModeException()

  override def retrievePaths(typeUri: Uri, depth: Int, limit: Option[Int])(implicit userContext: UserContext): IndexedSeq[TypedPath] = SafeModeException.throwSafeModeException()

  override def retrieve(entitySchema: EntitySchema, limit: Option[Int])(implicit userContext: UserContext): EntityHolder = SafeModeException.throwSafeModeException()

  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])(implicit userContext: UserContext): EntityHolder = SafeModeException.throwSafeModeException()
}

object SafeModeSink extends DataSink with LinkSink with EntitySink {
  override def clear()(implicit userContext: UserContext): Unit = SafeModeException.throwSafeModeException()

  override def init()(implicit userContext: UserContext): Unit = SafeModeException.throwSafeModeException()

  override def writeLink(link: Link, predicateUri: String)(implicit userContext: UserContext): Unit = SafeModeException.throwSafeModeException()

  override def openTable(typeUri: Uri, properties: Seq[TypedProperty])(implicit userContext: UserContext, prefixes: Prefixes): Unit = SafeModeException.throwSafeModeException()

  override def closeTable()(implicit userContext: UserContext): Unit = SafeModeException.throwSafeModeException()

  override def writeEntity(subject: String, values: Seq[Seq[String]])(implicit userContext: UserContext): Unit = SafeModeException.throwSafeModeException()

  override def close()(implicit userContext: UserContext): Unit = SafeModeException.throwSafeModeException()
}

case class SafeModeException(msg: String) extends RuntimeException(msg)

object SafeModeException {
  def throwSafeModeException(): Nothing = {
    throw SafeModeException("The application is in safe-mode. Access to the external resource has been prevented.")
  }
}