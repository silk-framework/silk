package controllers.shared.autoCompletion

import controllers.autoCompletion
import controllers.autoCompletion._
import controllers.transform.AutoCompletionApi.Categories
import controllers.transform.autoCompletion.{OpFilter, PathToReplace}
import org.silkframework.config.Prefixes
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths._
import org.silkframework.plugins.path.PathMetaData
import org.silkframework.rule.DatasetSelection
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Uri
import org.silkframework.workspace.Project
import org.silkframework.workspace.activity.transform.CachedEntitySchemata

import scala.annotation.tailrec
import scala.collection.mutable

object AutoCompletionApiUtils {
  /** Gets the paths cache value as auto-autocompletion objects. */
  def pathsCacheCompletions(typeUri: Uri,
                            cachedEntitySchema: Option[CachedEntitySchemata],
                            preferUntypedSchema: Boolean,
                            pathMetaData: Option[Iterable[TypedPath] => Iterable[PathMetaData]] = None,
                            alternativeInputSchema: Option[EntitySchema] = None)
                           (implicit project: Project): Completions = {
    implicit val prefixes: Prefixes = project.config.prefixes
    if (cachedEntitySchema.isDefined || alternativeInputSchema.isDefined) {
      val paths = alternativeInputSchema.map(_.typedPaths)
        .getOrElse(fetchCachedPaths(typeUri, cachedEntitySchema.get, preferUntypedSchema))
      val serializedPathSet = new mutable.HashSet[String]()
      val serializedPaths = paths
        // Sort primarily by path operator length then name
        .sortWith { (p1, p2) =>
          if (p1.operators.length == p2.operators.length) {
            p1.serialize() < p2.serialize()
          } else {
            p1.operators.length < p2.operators.length
          }
        }
        .map(p => (p.toUntypedPath.serialize()(prefixes), p))
        .filter(p => {
          val passes = !serializedPathSet.contains(p._1)
          serializedPathSet.add(p._1)
          passes
        })
      val pathMetaDataMap: Map[String, String] = pathMetaData match {
        case Some(fetchPathMetaData) =>
          val pathsMetaData = fetchPathMetaData(serializedPaths.map(_._2))
          pathsMetaData
            .filter(_.label.isDefined)
            .map(p => (p.value, p.label.get))
            .toMap
        case None =>
          Map.empty
      }
      val completions = for((pathStr, _) <- serializedPaths) yield {
        Completion(
          value = pathStr,
          label = pathMetaDataMap.get(pathStr),
          description = None,
          category = Categories.sourcePaths,
          isCompletion = true
        )
      }
      autoCompletion.Completions(completions)
    } else {
      Completions()
    }
  }

  private def fetchCachedPaths(typeUri: Uri,
                               cachedEntitySchema: CachedEntitySchemata,
                               preferUntypedSchema: Boolean): IndexedSeq[TypedPath] = {
    cachedEntitySchema.fetchCachedPaths(typeUri, preferUntypedSchema)
  }

  /** Return the simple path of the given path, i.e. without any filters. */
  def simplePath(sourcePath: List[PathOperator]): List[PathOperator] = {
    sourcePath.filter(op => op.isInstanceOf[ForwardOperator] || op.isInstanceOf[BackwardOperator])
  }

  /** Filter out paths that start with either the simple source or forward only source path, then
    * rewrite the auto-completion to a relative path from the full paths. */
  def extractRelativePaths(simpleSourcePath: List[PathOperator],
                           forwardOnlySourcePath: List[PathOperator],
                           pathCacheCompletions: Completions,
                           isRdfInput: Boolean,
                           oneHopOnly: Boolean = false,
                           serializeFull: Boolean = false,
                           pathOpFilter: OpFilter.Value = OpFilter.None,
                           supportsAsteriskOperator: Boolean = false)
                          (implicit prefixes: Prefixes): Seq[Completion] = {
    pathCacheCompletions.values.filter { p =>
      val path = UntypedPath.parse(p.value)
      val matchesPrefix = isRdfInput || // FIXME: Currently there are no paths longer 1 in cache, that why return full path
        UntypedPath.startsWithPrefix(path.operators, forwardOnlySourcePath, supportsAsteriskOperator) && path.operators.size > forwardOnlySourcePath.size ||
        UntypedPath.startsWithPrefix(path.operators, simpleSourcePath, supportsAsteriskOperator) && path.operators.size > simpleSourcePath.size
      val truncatedOps = truncatePath(path, simpleSourcePath, forwardOnlySourcePath, isRdfInput, supportsAsteriskOperator)
      val pathOpMatches = pathOpFilter match {
        case OpFilter.Forward => truncatedOps.headOption.exists(op => op.isInstanceOf[ForwardOperator])
        case OpFilter.Backward => truncatedOps.headOption.exists(op => op.isInstanceOf[BackwardOperator])
        case _ => true
      }
      matchesPrefix && pathOpMatches && (!oneHopOnly && truncatedOps.nonEmpty || truncatedOps.size == 1)
    } map { completion =>
      val path = UntypedPath.parse(completion.value)
      val truncatedOps = truncatePath(path, simpleSourcePath, forwardOnlySourcePath, isRdfInput, supportsAsteriskOperator)
      completion.copy(value = UntypedPath(truncatedOps).serialize(stripForwardSlash = !serializeFull))
    }
  }

  private def truncatePath(path: UntypedPath,
                           simpleSourcePath: List[PathOperator],
                           forwardOnlySourcePath: List[PathOperator],
                           isRdfInput: Boolean,
                           supportsAsteriskOperator: Boolean): List[PathOperator] = {
    if (isRdfInput) {
      path.operators
    } else if (UntypedPath.startsWithPrefix(path.operators, forwardOnlySourcePath, supportsAsteriskOperator)) {
      path.operators.drop(forwardOnlySourcePath.size)
    } else {
      path.operators.drop(simpleSourcePath.size)
    }
  }

  /** Normalize this path by eliminating backward operators. It does not eliminate backward operators at the start, since this would suggest incorrect paths then. */
  def forwardOnlyPath(simpleSourcePath: List[PathOperator]): List[PathOperator] = {
    // Remove BackwardOperators
    var pathStack = List.empty[PathOperator]
    for (op <- simpleSourcePath) {
      op match {
        case f: ForwardOperator =>
          pathStack ::= f
        case b: BackwardOperator =>
          if (pathStack.isEmpty || pathStack.head.isInstanceOf[BackwardOperator]) {
            pathStack ::= b
          } else {
            pathStack = pathStack.tail
          }
        case _ =>
          throw new IllegalArgumentException("Path cannot contain path operators other than forward and backward operators!")
      }
    }
    pathStack.reverse
  }

  /** Return the partial auto-completion results. */
  def partialAutoCompletionResult(autoCompletionRequest: AutoSuggestAutoCompletionRequest,
                                  pathToReplace: PathToReplace,
                                  operatorCompletions: Option[ReplacementResults],
                                  filteredResults: Completions): AutoSuggestAutoCompletionResponse = {
    val from = pathToReplace.from
    val length = pathToReplace.length
    AutoSuggestAutoCompletionResponse(
      autoCompletionRequest.inputString,
      autoCompletionRequest.cursorPosition,
      replacementResults = Seq(
        ReplacementResults(
          ReplacementInterval(from, length),
          pathToReplace.query.getOrElse(""),
          filteredResults.toCompletionsBase.completions
        )
      ) ++ operatorCompletions
    )
  }
}
