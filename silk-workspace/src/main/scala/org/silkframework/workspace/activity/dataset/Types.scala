package org.silkframework.workspace.activity.dataset

import org.silkframework.config.Prefixes
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.serialization.XmlFormat

import scala.xml.Node

/**
 * Information about the most frequent types in a data set.
 */
case class Types(typesByFrequency: Seq[(String, Double)]) {

  // Lists all known types.
  def types = typesByFrequency.map(_._1)
}

object Types {

  def empty = Types(Seq.empty)

  /**
   * XML serialization.
   */
  implicit object TypesFormat extends XmlFormat[Types] {

    def read(node: Node)(implicit prefixes: Prefixes, resources: ResourceManager) = Types(
      for (typeNode <- node \ "Type";
           frequencyNode <- typeNode \ "@frequency")
        yield (typeNode.text, frequencyNode.text.toDouble)
    )

    def write(value: Types)(implicit prefixes: Prefixes): Node =
      <Types>
      { for((uri, frequency) <- value.typesByFrequency) yield <Type frequency={frequency.toString}>{uri}</Type> }
      </Types>
  }

}
