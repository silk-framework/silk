package org.silkframework.dataset

import org.silkframework.config.Prefixes
import org.silkframework.entity.paths.{BackwardOperator, ForwardOperator, PathOperator, UntypedPath}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Uri
import org.silkframework.util.Uri._

/**
  * A data source that can give information about how given input paths cover the sources input paths. This is used for example
  * to find out which input paths are covered by transformations.
  */
trait PathCoverageDataSource {
  this: DataSource =>
  def pathCoverage(pathInputs: Seq[CoveragePathInput])
                  (implicit prefixes: Prefixes,
                   userContext: UserContext): PathCoverageResult = {
    // This should get all paths defined for this source, depending on the implementation of the data source the depth might be limited to 1.
    val allPaths = retrievePaths("", depth = Int.MaxValue)
    val pathCoverages = for (sourcePath <- allPaths) yield {
      var covered = false
      var fullyCovered = false
      for (pathInput <- pathInputs;
           inputPath <- pathInput.paths) {
        if(matchPath(pathInput.typeUri, inputPath, sourcePath.toUntypedPath)) {
          covered = true
          if(fullCoveragePath(inputPath)) {
            fullyCovered = true
          }
        }
      }
      PathCoverage(sourcePath.toUntypedPath.serialize(), covered, fullyCovered)
    }
    PathCoverageResult(pathCoverages)
  }

  /** Only paths that only have forward paths are considered to fully cover the input values. This assumption is true for
    * all nested types like XML and JSON, it may not be true for other data models. */
  def fullCoveragePath(path: UntypedPath): Boolean = {
    path.operators.forall {
      case _: ForwardOperator =>
        true
      case _ =>
        false // Operators like filters are expected to not fully cover a specific path.
    }
  }

  /** Normalized the input path, gets rid of filters, resolves backward paths. The backward path resolution only works for
    * nested data models. This won't work for example with graph data models like RDF where there is no unique parent.*/
  def normalizeInputPath(pathOperators: Seq[PathOperator]): Option[Seq[PathOperator]] = {
    // Should only include forward operators like the source path
    var cleanOperators = List.empty[PathOperator]
    for(op <- pathOperators) {
      op match {
        case f: ForwardOperator =>
          cleanOperators ::= f
        case b: BackwardOperator =>
          if(cleanOperators.isEmpty) {
            return None // Invalid path, short cir
          } else {
            cleanOperators = cleanOperators.tail
          }
        case _ =>
        // Throw away other operators
      }
    }
    Some(cleanOperators.reverse)
  }

  /**
    * returns the combined path. Depending on the data source the input path may or may not be modified based on the type URI.
    */
  def combinedPath(typeUri: String, inputPath: UntypedPath): UntypedPath

  /** Returns true if the given input path matches the source path else false. */
  def matchPath(typeUri: String, inputPath: UntypedPath, sourcePath: UntypedPath): Boolean = {
    assert(sourcePath.operators.forall(_.isInstanceOf[ForwardOperator]), "Error in matching paths in XML source: Not all operators were forward operators!")
    val operators = combinedPath(typeUri, inputPath).operators
    normalizeInputPath(operators) match {
      case Some(cleanOperators) =>
        matchCleanPath(cleanOperators.toList, sourcePath.operators)
      case None =>
        false // not possible to normalize path
    }
  }

  /** Matches the cleaned up input path. Recognizes '*' and '**' forward paths. */
  def matchCleanPath(inputOperators: List[PathOperator],
                     sourceOperators: List[PathOperator]): Boolean = {
    (inputOperators, sourceOperators) match {
      case (ForwardOperator(Uri("*")) :: tail, _ :: sTail) =>
        matchCleanPath(tail, sTail)
      case (ForwardOperator(Uri("**")) :: tail, _) =>
        recursiveTails(sourceOperators) exists { sTail =>
          matchCleanPath(tail, sTail)
        }
      case (iOp :: iTail, sOp :: sTail) =>
        iOp == sOp && matchCleanPath(iTail, sTail)
      case (Nil, Nil) =>
        true
      case _ =>
        false
    }
  }

  private def recursiveTails(operators: List[PathOperator]): List[List[PathOperator]] = {
    operators match {
      case Nil =>
        Nil
      case _ :: tail =>
        tail :: recursiveTails(tail)
    }
  }
}

case class PathCoverageResult(paths: Seq[PathCoverage])

case class PathCoverage(path: String, covered: Boolean, fully: Boolean)

case class CoveragePathInput(typeUri: String, paths: Seq[UntypedPath])