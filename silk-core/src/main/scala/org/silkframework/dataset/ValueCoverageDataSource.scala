package org.silkframework.dataset

import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Uri

/**
  * A data source that can give specific information about how input paths cover a specific path of the data source.
  * For example if an input path has a property filter, it returns which values are not covered by this restricted input path.
  */
trait ValueCoverageDataSource {
  this: PathCoverageDataSource with DataSource =>
  /**
    * Returns the value coverage of the input paths regarding the data source path.
    * @param dataSourcePath The normalized path as returned by retrievePaths or e.g. by the mapping coverage endpoint.
    * @param inputPaths The actual input paths that when normalized match the dataSourcePath
    * @return
    */
  def valueCoverage(dataSourcePath: UntypedPath, inputPaths: Traversable[UntypedPath])
                   (implicit userContext: UserContext): ValueCoverageResult = {
    val completeValues: Set[(String, Option[String])] = valuesForDataSourcePath(dataSourcePath)
    val collectedValues: Set[(String, Option[String])] = valuesForInputPaths(inputPaths)
    val uniqueCompleteValues = completeValues.toSeq.distinct
    val uniqueCollectedValues = collectedValues.toSeq.distinct
    val overallNumber = uniqueCompleteValues.size
    val missingValues = uniqueCompleteValues.diff(uniqueCollectedValues) map { case (value, id) =>
      ValueCoverageMiss(id, value)
    }
    ValueCoverageResult(overallNumber, overallNumber - missingValues.size, missedValues = missingValues)
  }

  /** Converts a path into the same path, but getting the ID of the value that would be fetched. If unique IDs are not
    * supported for this data source it should return None. */
  def convertToIdPath(path: UntypedPath): Option[UntypedPath]

  def valuesForDataSourcePath(dataSourcePath: UntypedPath)
                             (implicit userContext: UserContext): Set[(String, Option[String])] = {
    val dataSourceValuePath = dataSourcePath
    val dataSourceIdPath = convertToIdPath(dataSourcePath).map(_.asStringTypedPath)
    val noneStream = Stream.continually(None)
    val entitySchemaForOverallValues = EntitySchema(Uri(""), typedPaths = IndexedSeq(dataSourceValuePath.asStringTypedPath) ++ dataSourceIdPath.toIndexedSeq)
    val completeValues = retrieve(entitySchemaForOverallValues).entities.flatMap { e =>
      val values = e.values
      val ids = if (values.size > 1) {
        e.values.last.map(Some(_))
      } else {
        noneStream
      }
      e.values.head zip ids
    }
    completeValues.toSet
  }

  private def noneStream = Stream.continually(None)

  def valuesForInputPaths(inputPaths: Traversable[UntypedPath])
                         (implicit userContext: UserContext): Set[(String, Option[String])] = {
    val idInputPaths = inputPaths flatMap convertToIdPath
    val entitySchemaForInputPaths = EntitySchema(Uri(""), typedPaths = (inputPaths ++ idInputPaths).toIndexedSeq.map(_.asStringTypedPath))
    val collectedValues = retrieve(entitySchemaForInputPaths).entities.flatMap { e =>
      val entityValues = e.values
      if (entityValues.size == inputPaths.size) {
        entityValues.flatMap(_ zip noneStream)
      } else if (entityValues.size == inputPaths.size * 2) {
        val valueSeqs = entityValues.slice(0, inputPaths.size)
        val idSeqs = entityValues.slice(inputPaths.size, inputPaths.size * 2)
        for ((values, ids) <- valueSeqs zip idSeqs;
             (value, id) <- values zip ids) yield {
          (value, Some(id))
        }
      } else {
        throw new RuntimeException("Could not get IDs for all paths.")
      }
    }
    collectedValues.toSet
  }
}

case class ValueCoverageResult(overallValues: Int, coveredValues: Int, missedValues: Seq[ValueCoverageMiss])

/** This represents a value that was NOT covered by any of the input paths. The node id is an optional unique value
  * for this particular value, as for example returned by the '#id' special path. */
case class ValueCoverageMiss(nodeId: Option[String], value: String)