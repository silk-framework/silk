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
case class EntitySchema(
  typeUri: Uri,
  typedPaths: IndexedSeq[TypedPath],
  filter: Restriction = Restriction.empty,
  subPath: Path = Path.empty
 ) extends Serializable {

  /**
    * Retrieves the index of a given path.
    * NOTE: will work simple Paths as well, but there might be a chance that a given path exists twice with different value types
    *
    * @param path - the path to find
    * @return - the index of the path in question
    * @throws NoSuchElementException If the path could not be found in the schema.
    */
  def pathIndex(path: Path): Int = {
    val valueTypeOpt = path match{
      case tp: TypedPath => Option(tp.valueType)
      case _ => None
    }
    //find the given path and, if provided, match the value type as well
    typedPaths.zipWithIndex.find(pi => pi._1 == path && (   //if the paths equal and ...
      valueTypeOpt.isEmpty ||                               //if no ValueType is specified or ...
      valueTypeOpt.get == AutoDetectValueType ||            //ValueType is of no importance or...
      pi._1.valueType == AutoDetectValueType ||             //ValueType of the list element is of no importance or...
      pi._1.valueType == valueTypeOpt.get                   //both ValueTypes match then...
    )) match{
      case Some((_, ind)) => ind
      case None => throw new NoSuchElementException(s"Path $path not found on entity. Available paths: ${typedPaths.mkString(", ")}.")
    }
  }

  lazy val uriIndex: Int = this.typedPaths.zipWithIndex.
    find(p => p._1.serializeSimplified == EntitySchema.defaultUriColumn).map(_._2).getOrElse(0)                     //TODO is using "URI" by default as uri column wise?

  lazy val valueIndicies: IndexedSeq[Int] = this.typedPaths.zipWithIndex.
    filterNot(p => p._1.serializeSimplified == EntitySchema.defaultUriColumn).map(_._2 + 1)                               //FIXME: change with CMEM-1172!

  def propertyNames: Seq[String] = valueIndicies.map(i => typedPaths(i - 1)).flatMap(p => p.propertyUri).map(_.toString)  //FIXME: change with CMEM-1172!

  def child(path: Path): EntitySchema = copy(subPath = Path(subPath.operators ::: path.operators))
}

object EntitySchema {

  //TODO needs properties entry
  val defaultUriColumn: String = "URI"

  def empty: EntitySchema = EntitySchema(Uri(""), IndexedSeq[TypedPath](), subPath = Path.empty, filter = Restriction.empty)

  /**
    * Will replace the property uris of selects paths of a given EntitySchema, using a Map[oldUri, newUri].
    * NOTE: valueType and isAttribute of the TypedPath will be copied!
    * @param es - the EntitySchema
    * @param propMap - the property uri replacement map
    * @return - the new EntitySchema with replaced property uris
    */
  def renamePaths(es: EntitySchema, propMap: Map[String, String]): EntitySchema ={
    val newPaths = es.typedPaths.map{p =>
      p.propertyUri match {
        case Some(prop) => propMap.get(prop.toString) match{
          case Some(newProp) => TypedPath(Path(newProp), p.valueType, p.isAttribute)
          case None => TypedPath(Path(prop), p.valueType, p.isAttribute)
        }
        case None => p
      }
    }
    es.copy(typedPaths = newPaths)
  }

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