package org.silkframework.entity

import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat, XmlSerialization}
import org.silkframework.util.Uri

import scala.xml.Node

/**
  * An entity schema.
  *
  * @param typeUri The entity type
  * @param typedPaths The list of paths
  * @param filter A filter for restricting the entity set
  * @param subPath
  */
case class EntitySchema(typeUri: Uri,
                        typedPaths: IndexedSeq[TypedPath],
                        filter: Restriction = Restriction.empty,
                        subPath: Path = Path.empty) extends Serializable {
  //require(filter.paths.forall(paths.contains), "All paths that are used in restriction must be contained in paths list.")

  /**
   * Retrieves the index of a given path.
   *
   * @throws NoSuchElementException If the path could not be found in the schema.
   */
  def pathIndex(path: Path): Int = {
    var index = 0
    while (path != typedPaths(index).path) {
      index += 1
      if (index >= typedPaths.size) {
        throw new NoSuchElementException(s"Path $path not found on entity. Available paths: ${typedPaths.map(_.path).mkString(", ")}.")
      }
    }
    index
  }

  def child(path: Path): EntitySchema = copy(subPath = Path(subPath.operators ::: path.operators))
}

object EntitySchema {

  def empty: EntitySchema = EntitySchema(Uri(""), IndexedSeq[TypedPath](), subPath = Path.empty, filter = Restriction.empty)

  /**
    * XML serialization format.
    */
  implicit object EntitySchemaFormat extends XmlFormat[EntitySchema] {
    /**
      * Deserialize an EntitySchema from XML.
      */
    def read(node: Node)(implicit readContext: ReadContext): EntitySchema = {
      // Try TypedPath first, then Path for forward compatibility
      val typedPaths = for (pathNode <- (node \ "Paths" \ "TypedPath").toIndexedSeq) yield {
        XmlSerialization.fromXml[TypedPath](pathNode)
      }
      val paths = if(typedPaths.isEmpty) {
        for (pathNode <- (node \ "Paths" \ "Path").toIndexedSeq) yield {
          TypedPath(Path.parse(pathNode.text.trim), StringValueType, isAttribute = false)
        }
      } else {
        typedPaths
      }

      EntitySchema(
        typeUri = Uri((node \ "Type").text),
        typedPaths =  paths,
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
          for (path <- desc.typedPaths) yield {
            XmlSerialization.toXml(path)
          }
          }
        </Paths>
      </EntityDescription>
  }

}