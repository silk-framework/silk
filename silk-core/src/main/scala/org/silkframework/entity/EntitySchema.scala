package org.silkframework.entity

import org.silkframework.config.Prefixes
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.util.Uri

import scala.xml.Node

/**
 * An entity schema.
 *
 * @param typeUri The entity type
 * @param paths The list of paths
 * @param filter A filter for restricting the entity set
 */
case class EntitySchema(typeUri: Uri, paths: IndexedSeq[Path], filter: Restriction = Restriction.empty) {
  //require(filter.paths.forall(paths.contains), "All paths that are used in restriction must be contained in paths list.")

  /**
   * Retrieves the index of a given path.
   */
  def pathIndex(path: Path) = {
    var index = 0
    while (path != paths(index)) {
      index += 1
      if (index >= paths.size)
        throw new NoSuchElementException(s"Path $path not found on entity. Available paths: ${paths.mkString(", ")}.")
    }
    index
  }
}

object EntitySchema {

  def empty = EntitySchema(Uri(""), IndexedSeq[Path](), Restriction.empty)

  /**
    * XML serialization format.
    */
  implicit object EntitySchemaFormat extends XmlFormat[EntitySchema] {
    /**
      * Deserialize an EntitySchema from XML.
      */
    def read(node: Node)(implicit readContext: ReadContext) = {
      EntitySchema(
        typeUri = Uri((node \ "Type").text),
        paths = for (pathNode <- (node \ "Paths" \ "Path").toIndexedSeq) yield Path.parse(pathNode.text.trim),
        filter = Restriction.parse((node \ "Restriction").text)(readContext.prefixes)
      )
    }

    /**
      * Serialize an EntitySchema to XML.
      */
    def write(desc: EntitySchema)(implicit writeContext: WriteContext[Node]): Node =
      <EntityDescription>
        <Type>{desc.typeUri}</Type>
        <Restriction>{desc.filter.serialize}</Restriction>
        <Paths> {
          for (path <- desc.paths) yield {
            <Path>{path.serialize(Prefixes.empty)}</Path>
          }
          }
        </Paths>
      </EntityDescription>
  }

}