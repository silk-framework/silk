package org.silkframework.plugins.dataset.csv

import org.silkframework.dataset.DatasetCharacteristics.SpecialPaths
import org.silkframework.entity.paths.{ForwardOperator, PathNotFoundException, UntypedPath}
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.util.Uri

/**
 * Generates entities from table rows.
 *
 * @param entitySchema The schema of the requested entities.
 * @param headerIndexMap All available columns and their indices.
 * @param allowMissingPaths If true, missing paths will return empty values instead of throwing an exception.
 */
class TableEntityGenerator(entitySchema: EntitySchema, headerIndexMap: Map[Uri, Int], allowMissingPaths: Boolean = false) {

  private val headerIndices = collectHeaderIndices()

  def generate(uri: String, values: collection.Seq[Seq[String]], entityRowIdx: Int, rowNum: Int): Entity = {
    Entity(
      uri = uri,
      values = headerIndices.map(i => getValue(i, values, entityRowIdx, rowNum)),
      schema = entitySchema
    )
  }

  private def collectHeaderIndices(): IndexedSeq[PathIndex] = {
    for (path <- entitySchema.typedPaths) yield {
      path.operators match {
        case ForwardOperator(prop) :: Nil =>
          if (path.operators == SpecialPaths.IDX.path.operators) {
            PathIndex(UntypedPath.IDX_PATH_IDX)
          } else if (path.operators == SpecialPaths.LINE.path.operators) {
            PathIndex(UntypedPath.IDX_PATH_LINE)
          } else if (path.operators == SpecialPaths.COLUMN.path.operators) {
            PathIndex(UntypedPath.IDX_PATH_COLUMN)
          } else {
            getPathIndex(prop, getColumn = false)
          }
        case ForwardOperator(prop) :: ForwardOperator(Uri(SpecialPaths.IDX.value)) :: Nil if !prop.uri.startsWith("#") =>
          PathIndex(UntypedPath.IDX_PATH_IDX)
        case ForwardOperator(prop) :: ForwardOperator(Uri(SpecialPaths.LINE.value)) :: Nil if !prop.uri.startsWith("#") =>
          PathIndex(UntypedPath.IDX_PATH_LINE)
        case ForwardOperator(prop) :: ForwardOperator(Uri(SpecialPaths.COLUMN.value)) :: Nil if !prop.uri.startsWith("#") =>
          getPathIndex(prop, getColumn = true)
        case _ =>
          throw PathNotFoundException(path, entitySchema.typeUri, headerIndexMap.keys.map(UntypedPath(_)))
      }
    }
  }

  private def getPathIndex(prop: Uri, getColumn: Boolean): PathIndex = {
    headerIndexMap.get(prop) match {
      case Some(index) =>
        PathIndex(index, getColumn)
      case None if allowMissingPaths =>
        PathIndex(UntypedPath.IDX_PATH_MISSING)
      case None =>
        throw PathNotFoundException(UntypedPath(prop), entitySchema.typeUri, headerIndexMap.keys.map(UntypedPath(_)))
    }
  }

  private def getValue(pathIndex: PathIndex, values: collection.Seq[Seq[String]], entityRowIdx: Int, rowNum: Int): Seq[String] = {
    if (pathIndex.getColumn) {
      Seq(pathIndex.index.toString)
    } else {
      pathIndex.index match {
        case UntypedPath.IDX_PATH_MISSING =>
          Seq.empty
        case UntypedPath.IDX_PATH_IDX =>
          Seq(entityRowIdx.toString)
        case UntypedPath.IDX_PATH_LINE =>
          Seq(rowNum.toString)
        case UntypedPath.IDX_PATH_COLUMN =>
          // The column of the entity (i.e., the row itself) is requested, so we return 0
          Seq("0")
        case idx: Int if idx >= 0 =>
          if(idx >= values.length) {
            Seq.empty
          } else {
            values(idx)
          }
        case specialIdx: Int =>
          throw new RuntimeException("Unsupported special path with index number: " + specialIdx)
      }
    }
  }

  /**
   * The index of a path within a table.
   *
   * @param index     The column index of this path
   * @param getColumn True, if the column number itself is being requested, False, if the value is requested.
   */
  private case class PathIndex(index: Int, getColumn: Boolean = false)

}

object TableEntityGenerator {

  def apply(entitySchema: EntitySchema, header: Seq[Uri], allowMissingPaths: Boolean = false): TableEntityGenerator = {
    new TableEntityGenerator(entitySchema, header.zipWithIndex.toMap, allowMissingPaths)
  }

}
