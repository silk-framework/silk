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
//noinspection ScalaStyle
case class EntitySchema(
  typeUri: Uri,
  typedPaths: IndexedSeq[TypedPath],
  filter: Restriction = Restriction.empty,
  subPath: Path = Path.empty
 ) extends Serializable {

  /**
    * overriding the default case class copy(). to deal with Sub-Schemata
    * NOTE: providing subSchemata will automatically transform this schema in a MultiEntitySchema
    */
  def copy(
    typeUri: Uri = this.typeUri,
    typedPaths: IndexedSeq[TypedPath] = this.typedPaths,
    filter: Restriction = this.filter,
    subPath: Path = this.subPath,
    subSchemata: IndexedSeq[EntitySchema] = IndexedSeq.empty
  ): EntitySchema ={
    val pivotSchema = EntitySchema(typeUri, typedPaths, filter, subPath)
    subSchemata match{
      case subs if subs.isEmpty => pivotSchema
      case subs => new MultiEntitySchema(pivotSchema, subs)
    }
  }

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

  /**
    * Will return the (sub-) schema containing the TypedPath in question
    * @param property - property name of TypedPath to look for
    * @return
    */
  def getSchemaOfProperty(property: String): Option[EntitySchema] ={
    this.typedPaths.find(tp => tp.serializeSimplified == property) match{
      case Some(s) => getSchemaOfProperty(s)
      case None => None
    }
  }

  /**
    * this will return the EntitySchema containing the given typed path
    * NOTE: has to be overwritten in MultiEntitySchema
    * @param tp - the typed path
    * @return
    */
  def getSchemaOfProperty(tp: TypedPath): Option[EntitySchema] = this.typedPaths.find(tt => tt == tp) match{
    case Some(_) => Some(this)
    case None => None
  }

  def propertyNames: IndexedSeq[String] = this.typedPaths.map(p => p.serializeSimplified)

  def child(path: Path): EntitySchema = copy(subPath = Path(subPath.operators ::: path.operators))

  /**
    * Will replace the property uris of selects paths of a given EntitySchema, using a Map[oldUri, newUri].
    * NOTE: valueType and isAttribute of the TypedPath will be copied!
    * @param oldName - the property to be renamed
    * @param newName - the new property name
    * @return - the new EntitySchema with replaced property uris
    */
  def renameProperty(oldName: String, newName: String): EntitySchema ={
    val sourceSchema = getSchemaOfProperty(oldName)
    val targetSchema = sourceSchema.map(sa => sa.copy(
      typedPaths = sa.typedPaths.map(tp =>{
        if(tp.serializeSimplified == oldName) TypedPath(Path(newName), tp.valueType, tp.isAttribute) else tp
      })
    ))
    targetSchema.getOrElse(this)
  }

  override def hashCode(): Int = {
    val prime = 31
    var hashCode = typeUri.hashCode()
    hashCode = hashCode * prime + typedPaths.foldLeft(1)((hash,b) => hash * prime + b.hashCode())
    hashCode
  }

  override def equals(obj: scala.Any): Boolean = {
    if(obj != null) {
      obj match {
        case es: EntitySchema =>
          es.typeUri == this.typeUri &&
            es.typedPaths.size == this.typedPaths.size &&
            es.typedPaths.zip(this.typedPaths).forall(ps => ps._1 == ps._2)
        case _ => false
      }
    }
    else
      false
  }

  override def toString: String = "(" + typeUri + " : " + typedPaths.mkString(", ") + ")"
}

object EntitySchema {

  def empty: EntitySchema = EntitySchema(Uri(""), IndexedSeq[TypedPath](), subPath = Path.empty, filter = Restriction.empty)

  /**
    * Will create a nes EntitySchema minus all given TypedPaths
    * @param schema - the schema to be changed
    * @param tps - the TypedPaths to drop
    * @return
    */
  def dropTypedPaths(schema: EntitySchema, tps: TypedPath*): EntitySchema ={
    schema match{
      case mes: MultiEntitySchema =>
        new MultiEntitySchema(
          dropTypedPaths(mes.pivotSchema),
          mes.subSchemata.map(ss => dropTypedPaths(ss, tps:_*))
        )
      case es: EntitySchema =>
        EntitySchema(
          es.typeUri,
          es.typedPaths.map(tp => if(tps.contains(tp)) TypedPath.empty else tp),
          es.filter,
          es.subPath
        )
    }
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