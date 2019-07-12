package org.silkframework.entity

import MultiEntitySchema._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}

class MultiEntitySchema(private val pivot: EntitySchema, private val subs: IndexedSeq[EntitySchema])
  extends EntitySchema(
    getPivotSchema(pivot).typeUri,    //NOTE: using [[getPivotSchema]] will ensure that the pivot schema is not a MultiEntitySchema
    getPivotSchema(pivot).typedPaths,
    getPivotSchema(pivot).filter,
    getPivotSchema(pivot).subPath
  ){

  //NOTE: make sure not to use parameters pivot and subs (use these accessors instead)
  lazy val pivotSchema: EntitySchema = getPivotSchema(this)

  lazy val subSchemata: IndexedSeq[EntitySchema] = getNonPivotSchemata(this)
  //NOTE: ----------------------------------------------------------------------------

  // TypedPaths of pivot and typedPaths of subSchemata (prepended with their sub-paths)
  override val typedPaths: IndexedSeq[TypedPath] = pivotSchema.typedPaths ++
    subSchemata.flatMap(ses => ses.typedPaths.map(tp => TypedPath(ses.subPath.operators ++ tp.operators, tp.valueType, tp.isAttribute)))

  /**
    * Will replace the property uris of selects paths of a given EntitySchema, using a Map[oldUri, newUri].
    * NOTE: valueType and isAttribute of the TypedPath will be copied!
    *
    * @param oldName - the property to be renamed
    * @param newName - the new property name
    * @return - the new EntitySchema with replaced property uris
    */
  override def renameProperty(oldName: TypedPath, newName: TypedPath): EntitySchema = {
    this.getSchemaOfProperty(oldName) match{
      case Some(_) => new MultiEntitySchema(
        this.pivotSchema.renameProperty(oldName, newName),
        this.subSchemata.map(es => es.renameProperty(TypedPath.removePathPrefix(oldName, es.subPath).asInstanceOf[TypedPath], newName))
      )
      case None => this
    }
  }

  /**
    * his will return the EntitySchema containing the given typed path
    * NOTE: has to be overwritten in MultiEntitySchema
    * @param tp - the typed path
    * @return
    */
  override def getSchemaOfProperty(tp: TypedPath): Option[EntitySchema] =
    this.pivotSchema.getSchemaOfProperty(tp) match{
      case Some(es) => Some(es)
      case None => this.subSchemata.find(es => {
        es.typedPaths.contains(TypedPath.removePathPrefix(tp, es.subPath))
      })
  }


  /**
    * this will return the EntitySchema containing the given untyped path
    * NOTE: has to be overwritten in MultiEntitySchema
    * NOTE: there might be a chance that a given path exists twice with different value types, use [[getSchemaOfPropertyIgnoreType]] instead
    * @param tp - the untyped path
    */
  override def getSchemaOfPropertyIgnoreType(tp: UntypedPath): Option[EntitySchema] =
    this.pivotSchema.getSchemaOfPropertyIgnoreType(tp) match{
      case Some(es) => Some(es)
      case None =>
        this.subSchemata.find(es => {
          es.typedPaths.map(_.toUntypedPath).contains(UntypedPath.removePathPrefix(tp, es.subPath))
        })
    }

  /**
    * Returns the index of a given path in the pivot schema.
    * Note that paths in any of the sub schemata are not searched for.
    *
    * @param path - the path to find
    * @return - the index of the path in question
    */
  override def indexOfTypedPath(path: TypedPath): Int = pivotSchema.indexOfTypedPath(path)

  override def equals(obj: Any): Boolean = obj match{
    case mes: MultiEntitySchema =>
      mes.pivotSchema.equals(this.pivotSchema) &&
      mes.subSchemata.size == this.subSchemata.size &&
      mes.subSchemata.zip(this.subSchemata).forall(x => x._1.equals(x._2))
    case _ => false
  }

  override def equalsUntyped(obj: Any): Boolean = obj match{
    case mes: MultiEntitySchema =>
      mes.pivotSchema.equalsUntyped(this.pivotSchema) &&
        mes.subSchemata.size == this.subSchemata.size &&
        mes.subSchemata.zip(this.subSchemata).forall(x => x._1.equalsUntyped(x._2))
    case _ => false
  }
}

object MultiEntitySchema{
  def unapply(arg: MultiEntitySchema): Option[IndexedSeq[EntitySchema]] = Some(IndexedSeq(arg.pivotSchema) ++ arg.subSchemata)

  val SubEntityPrefix = "se"

  /**
    * Function to ensure to get the first, most granular EntitySchema
    * @param es - the Schema to begin with
    * @return
    */
  def getPivotSchema(es: EntitySchema): EntitySchema = es match{
    case mes: MultiEntitySchema => getPivotSchema(mes.pivot)
    case es: EntitySchema => es
  }

  /**
    * Opposite of [[getPivotSchema]] collecting all non-pivot schemata
    * @param es - the Schema to begin with
    * @return
    */
  def getNonPivotSchemata(es: EntitySchema): IndexedSeq[EntitySchema] = es match{
    case mes: MultiEntitySchema => getNonPivotSchemata(mes.pivot) ++ mes.subs.flatMap(sub => Seq(sub) ++ getNonPivotSchemata(sub))
    case _: EntitySchema => IndexedSeq()
  }
}
