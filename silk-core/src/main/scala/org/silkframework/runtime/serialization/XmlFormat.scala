package org.silkframework.runtime.serialization

import scala.reflect.ClassTag
import scala.xml.Node

/**
 * XML serialization format.
 */
abstract class XmlFormat[T: ClassTag] extends SerializationFormat[T, Node] {

  def serializedType = {
    implicitly[ClassTag[T]].runtimeClass
  }

}