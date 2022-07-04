package org.silkframework.entity

import org.silkframework.entity.paths.{Path, TypedPath, UntypedPath}
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
  * @param singleEntity If true, at most a single entity is expected.
  */
case class EntitySchema(
  typeUri: Uri,
  typedPaths: IndexedSeq[TypedPath],
  filter: Restriction = Restriction.empty,
  subPath: UntypedPath = UntypedPath.empty,
  singleEntity: Boolean = false
 ) extends Serializable {

  /**
    * overriding the default case class copy(). to deal with Sub-Schemata
    * NOTE: providing subSchemata will automatically transform this schema in a MultiEntitySchema
    */
  def copy(
            typeUri: Uri = this.typeUri,
            typedPaths: IndexedSeq[TypedPath] = this.typedPaths,
            filter: Restriction = this.filter,
            subPath: UntypedPath = this.subPath,
            subSchemata: IndexedSeq[EntitySchema] = IndexedSeq.empty
  ): EntitySchema ={
    val pivotSchema = EntitySchema(typeUri, typedPaths, filter, subPath)
    subSchemata match{
      case subs if subs.isEmpty => pivotSchema
      case subs => new MultiEntitySchema(pivotSchema, subs)
    }
  }

  /**
    * Retrieves all paths including the paths of sub schemata.
    */
  def allPaths: IndexedSeq[TypedPath] = typedPaths

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
    *
    * @param path - the path to find
    * @return - the index of the path in question
    * @throws NoSuchPathException If the path could not be found in the schema.
    */
  def indexOfTypedPath(path: TypedPath): Int = {
    //find the given path and, if provided, match the value type as well
    typedPaths.zipWithIndex.find(pi => path.equals(pi._1)) match{
      case Some((_, ind)) => ind
      case None => throw NoSuchPathException(s"Path $path not found on entity. Available paths: ${typedPaths.mkString(", ")}.", path)
    }
  }

  /**
    * Retrieves the index of a given path.
    * NOTE: there might be a chance that a given path exists twice with different value types, use [[indexOfTypedPath]] instead
 *
    * @return - the index of the path in question
    * @throws NoSuchElementException If the path could not be found in the schema.
    */
  def indexOfPath(path: UntypedPath): Int = {
    typedPaths.zipWithIndex.find(pi => path.operators == pi._1.operators) match{
      case Some((_, ind)) => ind
      case None =>
        throw NoSuchPathException(s"Path $path not found on entity. Available paths: ${typedPaths.mkString(", ")}.", path)
    }
  }

  /**
    * This function will return an available TypedPath depending on the type of the path parameter.
    * By providing only a Path the TypedPath is selected by its path-operators only (ignoring ValueType).
    * Providing a TypedPath the full TypedPath equals function is used for comparison.
    * @param path - a TypedPath
    * @return - an element of the typedPath collection (if any)
    */
  def findTypedPath(path: TypedPath): Option[TypedPath] = {
    this.typedPaths.find(tp => path == tp)
  }

  /**
    * Same as [[findTypedPath]] but ignoring the ValueType of the path, returning the first TypedPath matching the Path operators
    * NOTE: there might be a chance that a given path exists twice with different value types, use [[findTypedPath]] instead
    * @param path - ether a Path or a TypedPath
    * @return - an element of the typedPath collection (if any)
    */
  def findPath(path: UntypedPath): Option[TypedPath] = {
    this.typedPaths.find(tp => tp.toUntypedPath.equals(path)) //we make sure to use a Path using its equals
  }


  /**
    * this will return the EntitySchema containing the given typed path
    * NOTE: has to be overwritten in MultiEntitySchema
    * @param tp - the typed path
    * @return
    */
  def getSchemaOfProperty(tp: TypedPath): Option[EntitySchema] = findTypedPath(tp) match{
    case Some(_) => Some(this)
    case None => None
  }

  /**
    * this will return the EntitySchema containing the given untyped path
    * NOTE: there might be a chance that a given path exists twice with different value types, use [[getSchemaOfPropertyIgnoreType]] instead
    * NOTE: has to be overwritten in MultiEntitySchema
    * @param tp - the untyped path
    */
  def getSchemaOfPropertyIgnoreType(tp: UntypedPath): Option[EntitySchema] = this.typedPaths.find(tt => tt.toUntypedPath.equals(tp)) match{
    case Some(_) => Some(this)
    case None => None
  }

  lazy val propertyNames: IndexedSeq[String] = this.typedPaths.map(p => p.toUntypedPath.normalizedSerialization)

  def child(path: UntypedPath): EntitySchema = copy(subPath = UntypedPath(subPath.operators ::: path.operators))

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
      typedPaths = sa.typedPaths.map(tp => if(tp.equals(oldName)) TypedPath(newName.operators, tp.valueType, tp.isAttribute) else tp)
    ))
    targetSchema.getOrElse(this)
  }

  /**
    * Will create a new EntitySchema only containing the given TypedPaths
    * @param tps - the TypedPaths
    * @return
    */
  def selectTypedPaths(tps: TypedPath*): EntitySchema ={
    //we have to delete any duplicate path first the selected paths have to be unique
    EntitySchema.deleteDuplicatePaths(this) match{
      case mes: MultiEntitySchema =>
        val newPivot = mes.pivotSchema.selectTypedPaths(tps:_*)
        new MultiEntitySchema(
          newPivot,
          mes.subSchemata.map(ss => ss.selectTypedPaths(tps.diff(newPivot.typedPaths):_*))
        )
      case es: EntitySchema =>
        EntitySchema(
          es.typeUri,
          tps.flatMap(tp => if(tp.valueType == ValueType.UNTYPED) es.findPath(tp.toUntypedPath) else es.findTypedPath(tp) match{
            case Some(_) => Some(tp)
            case None =>
              throw new IllegalArgumentException(tp + " was not found in EntitySchema: " + this.typedPaths.mkString(", "))
          }).toIndexedSeq,
          es.filter,
          es.subPath
        )
    }
  }

  override def hashCode(): Int = {
    val prime = 31
    var hashCode = typeUri.hashCode()
    hashCode = hashCode * prime + subPath.hashCode
    hashCode = hashCode * prime + typedPaths.foldLeft(1)((hash,b) => hash * prime + b.hashCode())
    hashCode
  }

  override def equals(obj: Any): Boolean = {
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

  def equalsUntyped(obj: Any): Boolean = {
    if(obj != null) {
      obj match {
        case es: EntitySchema =>
          es.typeUri == this.typeUri &&
            es.typedPaths.size == this.typedPaths.size &&
            es.typedPaths.zip(this.typedPaths).forall(ps => ps._1.equalsUntyped(ps._2)) &&
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

  def empty: EntitySchema = EntitySchema(Uri(""), IndexedSeq[TypedPath](), subPath = UntypedPath.empty, filter = Restriction.empty)

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
        TypedPath(UntypedPath.parse(pathNode.text.trim), ValueType.STRING, isAttribute = false)
      }
    } else {
      typedPaths
    }

    EntitySchema(
      typeUri = Uri((node \ "Type").text),
      typedPaths =  paths,
      filter = Restriction.parse((node \ "Restriction").text)(readContext.prefixes),
      singleEntity = (node \ "@singleEntity").headOption.exists(_.text.toBoolean)
    )
  }

  /**
    * Serialize an EntitySchema to XML.
    */
  def write(desc: EntitySchema)(implicit writeContext: WriteContext[Node]): Node =
    <EntityDescription singleEntity={desc.singleEntity.toString}>
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

case class NoSuchPathException(message: String, path: Path) extends NoSuchElementException(message)