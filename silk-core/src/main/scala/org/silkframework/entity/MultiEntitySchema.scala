package org.silkframework.entity

import MultiEntitySchema._

class MultiEntitySchema(private val pivot: EntitySchema, private val subs: IndexedSeq[EntitySchema])
  extends EntitySchema(
    getPivotSchema(pivot).typeUri,    //NOTE: using [[getPivotSchema]] will ensure that the pivot schema is not a MultiEntitySchema
    getPivotSchema(pivot).typedPaths,
    getPivotSchema(pivot).filter,    //TODO how to combine multiple filters, sub paths
    getPivotSchema(pivot).subPath
  ){

  //NOTE: make sure not to use parameters pivot and subs outside of the following two accessors (use those instead)
  lazy val pivotSchema: EntitySchema = getPivotSchema(this)

  //TODO containing all sub-schemata of subs and pivot!!! - order is not sufficiently ensured, should we rename TypePaths if duplicates?
  lazy val subSchemata: IndexedSeq[EntitySchema] = getNonPivotSchemata(this)
  //NOTE: ---------------------------------------------------------------------------------------------------------

  override val typedPaths: IndexedSeq[TypedPath] = pivotSchema.typedPaths ++ subSchemata.flatMap(_.typedPaths)

  override def getSchemaOfProperty (tp: TypedPath): EntitySchema = {
    if(tp.isEmpty){
      EntitySchema.empty
    }
    else if(pivotSchema.typedPaths.contains(tp)){
      pivotSchema
    }
    else{
      subSchemata.find(se => se.typedPaths.contains(tp)) match{
        case Some(s) => s
        case None => EntitySchema.empty
      }
    }
  }
}

object MultiEntitySchema{
  def unapply(arg: MultiEntitySchema): Option[IndexedSeq[EntitySchema]] = Some(IndexedSeq(arg.pivotSchema) ++ arg.subSchemata)

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
    * @param es
    * @return
    */
  def getNonPivotSchemata(es: EntitySchema): IndexedSeq[EntitySchema] = es match{
    case mes: MultiEntitySchema => getNonPivotSchemata(mes.pivot) ++ mes.subs.flatMap(sub => Seq(sub) ++ getNonPivotSchemata(sub))
    case _: EntitySchema => IndexedSeq()
  }
}
