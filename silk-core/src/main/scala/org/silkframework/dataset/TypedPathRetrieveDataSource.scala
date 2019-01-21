package org.silkframework.dataset

import org.silkframework.entity.TypedPath
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Uri

/**
  * A data source with an extended 'retrieve paths' method that also adds the information if a path is a literal path
  * or an object path.
  */
trait TypedPathRetrieveDataSource {
  this: DataSource =>

  /**
    * Retrieves typed paths. The value type of the path denotes what type this path has in the corresponding data source.
    * The [[org.silkframework.entity.UriValueType]] has a special meaning for non-RDF data sources, in that it specifies
    * non-literal values, e.g. a XML element with nested elements, a JSON object or array of objects etc.
    *
    * @param typeUri The type URI. For non-RDF data types this is not a URI, e.g. XML or JSON this may express the path from the root.
    * @param depth   The maximum depths of the returned paths. This is only a limit, but not a guarantee that all paths
    *                of this length are actually returned.
    * @param limit   The maximum number of typed paths returned. None stands for unlimited.
    */
  def retrieveTypedPath(typeUri: Uri, depth: Int = Int.MaxValue, limit: Option[Int] = None)
                       (implicit userContext: UserContext): IndexedSeq[TypedPath]
}

sealed trait PathType

object ObjectPath extends PathType

object LiteralPath extends PathType