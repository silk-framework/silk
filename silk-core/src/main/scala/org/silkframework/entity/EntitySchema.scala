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
  * @param subPath Specifies the path starting from the root that is used for enumerating the entities.
  */
//noinspection ScalaStyle
case class EntitySchema(
  typeUri: Uri,
  typedPaths: IndexedSeq[TypedPath],
  filter: Restriction = Restriction.empty,
  subPath: Path = Path.empty
 ) extends Serializable {

  // use this for any validation tests
    assert(
      typedPaths.distinct.size == typedPaths.size &&  //typed paths are unique
      typeUri.uri.trim.nonEmpty                       //none empty type uri
    , "EntitySchema is not valid. Make sure all properties are named uniquely.")

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
    * Simplifies the acquisition of path ranges. No indices needed.
    * @param fromPath - range starts with path
    * @param toPath - range ends with path
    * @return
    */
  def getPathRange(fromPath: TypedPath, toPath: Option[TypedPath]): Seq[TypedPath] = {
    val fromIndex = typedPaths.zipWithIndex.find(p => fromPath == p._1).map(_._2).getOrElse(return Seq())
    val toIndex = toPath.map(tp => typedPaths.zipWithIndex.find(p => tp == p._1).map(_._2).getOrElse(typedPaths.size - 1)).getOrElse(return Seq())

    typedPaths.slice(fromIndex, toIndex + 1) //until param is exclusive
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
    typedPaths.zipWithIndex.find(pi => (
      valueTypeOpt.isEmpty ||                               //if no ValueType is specified or ...
      valueTypeOpt.get == AutoDetectValueType ||            //ValueType is of no importance or...
      pi._1.valueType == AutoDetectValueType ||             //ValueType of the list element is of no importance or...
      pi._1.valueType == valueTypeOpt.get                   //both ValueTypes match then...
    ) && pi._1 == path) match{                              //if the paths equal
      case Some((_, ind)) => ind
      case None => throw new NoSuchElementException(s"Path $path not found on entity. Available paths: ${typedPaths.mkString(", ")}.")
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

  lazy val propertyNames: IndexedSeq[String] = this.typedPaths.map(p => p.normalizedSerialization)

  def child(path: Path): EntitySchema = copy(subPath = Path(subPath.operators ::: path.operators))

  /**
    * Will replace the property uris of selects paths of a given EntitySchema, using a Map[oldUri, newUri].
    * NOTE: valueType and isAttribute of the TypedPath will be copied!
    * @param oldName - the property to be renamed
    * @param newName - the new property name
    * @return - the new EntitySchema with replaced property uris
    */
  def renameProperty(oldName: TypedPath, newName: TypedPath): EntitySchema ={
    val sourceSchema = getSchemaOfProperty(oldName)
    val targetSchema = sourceSchema.map(sa => sa.copy(
      typedPaths = sa.typedPaths.map(tp => if(tp == oldName) TypedPath(newName, tp.valueType, tp.isAttribute) else tp)
    ))
    targetSchema.getOrElse(this)
  }

  /**
    * Will create a new EntitySchema only containing the given TypedPaths
    * @param tps - the TypedPaths to drop
    * @return
    */
  def selectTypedPaths(tps: TypedPath*): EntitySchema ={
    //we have to delete any duplicate path first the selected paths have to be unique
    EntitySchema.deleteDuplicatePaths(this) match{
      case mes: MultiEntitySchema =>
        new MultiEntitySchema(
          mes.pivotSchema.selectTypedPaths(tps:_*),
          mes.subSchemata.map(ss => ss.selectTypedPaths(tps:_*))
        )
      case es: EntitySchema =>
        EntitySchema(
          es.typeUri,
          tps.flatMap(tp => es.typedPaths.find(t => t == tp) match{
            case Some(_) => Some(tp)
            case None => None
          }).toIndexedSeq,
          es.filter,
          es.subPath
        )
    }
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
          es.typedPaths.zip(this.typedPaths).forall(ps => ps._1 == ps._2) &&
          es.subPath == this.subPath &&
          (es.filter.operator match{
              case Some(f) if this.filter.operator.nonEmpty => f.paths.forall(p => this.filter.operator.get.paths.contains(p))
              case None if this.filter.operator.isEmpty => true
              case _ => false
          })
        case _ => false
      }
    }
    else
      false
  }
}

object EntitySchema {

  def empty: EntitySchema = EntitySchema(Uri(""), IndexedSeq[TypedPath](), subPath = Path.empty, filter = Restriction.empty)

  /**
    * Will delete any typed path already occurring before in the indexed sequence.
    * Precedence is given to paths of the pivot schema and then in order of sub schemata.
    * @param es - the EntitySchema to change
    * @return
    */
  private def deleteDuplicatePaths(es: EntitySchema): EntitySchema = {
    es match{
      case mes: MultiEntitySchema =>
        val pivot = deleteDuplicatePaths(mes.pivotSchema)
        var seen = pivot.typedPaths
        val subs = mes.subSchemata.map{ sub =>
          val tps = sub.typedPaths.distinct.diff(seen)
          seen = seen ++ tps
          sub.copy(typedPaths = tps)
        }
        mes.copy(es.typeUri, pivot.typedPaths, es.filter, es.subPath, subs)
      case nes: EntitySchema => nes.copy(typedPaths = nes.typedPaths.distinct)
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