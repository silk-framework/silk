package org.silkframework.dataset

import org.silkframework.config.Prefixes
import org.silkframework.entity.{ForwardOperator, Path}

/**
  * A data source that can give information about how given paths cover the sources input paths.
  */
trait CoverageDataSource {
  this: DataSource =>
  def pathCoverage(pathInputs: Seq[CoveragePathInput])(implicit prefixes: Prefixes): PathCoverageResult = {
    // This should get all paths defined for this source, depending on the implementation of the data source the depth might be limited to 1.
    val allPaths = retrievePaths("")
    val pathCoverages = for (sourcePath <- allPaths) yield {
      var covered = false
      var fullyCovered = false
      for (pathInput <- pathInputs;
           inputPath <- pathInput.paths) {
        if(matchPath(pathInput.typeUri, inputPath, sourcePath)) {
          covered = true
          if(fullCoveragePath(inputPath)) {
            fullyCovered = true
          }
        }
      }
      PathCoverage(sourcePath.serializeSimplified, covered, fullyCovered)
    }
    PathCoverageResult(pathCoverages)
  }

  /** Only paths that only have forward paths are considered to fully cover the input values. This assumption is true for
    * all nested types like XML and JSON, it may not be true for other data models. */
  def fullCoveragePath(path: Path): Boolean = {
    path.operators.forall {
      case _: ForwardOperator =>
        true
      case _ =>
        false // Operators like filters are expected to not fully cover a specific path.
    }
  }

  /** Returns true if the given input path matches the source path else false. */
  def matchPath(typeUri: String, inputPath: Path, sourcePath: Path): Boolean
}

case class PathCoverageResult(paths: Seq[PathCoverage])

case class PathCoverage(path: String, covered: Boolean, fully: Boolean)

case class CoveragePathInput(typeUri: String, paths: Seq[Path])