package org.silkframework.entity.paths

import org.silkframework.config.Prefixes
import org.silkframework.util.Uri

/**
  * A Path defines a chain of properties (or [[PathOperator]]), which when followed from a given type of [[org.silkframework.entity.Entity]] will provide the specified type of object or attribute.
  * There are two major implementations of this trait: [[UntypedPath]] and [[TypedPath]] where either an expected type is provided or not.
  */
trait Path extends Serializable {
  /**
    * The sequence of [[PathOperator]] defining the path.
    */
  def operators: List[PathOperator]


  /**
    * The normalized serialization using the Silk RDF path language.
    * Guarantees that the following equivalence holds true: path1 == path2 <=> path1.normalizedSerialization == normalizedSerialization
    */
  lazy val normalizedSerialization: String = serializePath(Prefixes.empty, stripForwardSlash = true)

  /**
    * Serializes this path using the Silk RDF path language.
    *
    * @param stripForwardSlash If true and if the path beginns with a forward operator, the first forward slash is stripped.
    * @param prefixes The prefixes used to shorten the path. If no prefixes are provided the normalized serialization is returned.
    */
  def serialize(stripForwardSlash: Boolean = true)(implicit prefixes: Prefixes = Prefixes.empty): String = prefixes match {
    case Prefixes.empty if stripForwardSlash => normalizedSerialization
    case _ => serializePath(prefixes, stripForwardSlash)
  }

  /**
    * Returns the property URI, if this is a simple forward path of length 1.
    * Otherwise, returns none.
    */
  def propertyUri: Option[Uri] = operators match {
    case ForwardOperator(prop) :: Nil => Some(prop)
    case _ => None
  }

  /**
    * Returns the number of operators in this path.
    */
  def size: Int = operators.size

  /**
    * Tests if this path is empty, i.e, has not operators.
    */
  def isEmpty: Boolean = operators.isEmpty

  /**
    * Internal path serialization function.
    */
  private def serializePath(prefixes: Prefixes, stripForwardSlash: Boolean): String = {
    val pathStr = operators.map(_.serialize(prefixes)).mkString
    if(stripForwardSlash) {
      pathStr.stripPrefix("/")
    } else {
      pathStr
    }
  }

  override def toString: String = normalizedSerialization

  def asUntypedPath: UntypedPath = UntypedPath(this.operators)
}
