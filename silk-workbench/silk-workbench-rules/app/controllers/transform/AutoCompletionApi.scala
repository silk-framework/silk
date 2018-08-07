package controllers.transform

import java.net.URLDecoder
import java.util.logging.Logger

import controllers.transform.AutoCompletionApi.Categories
import org.silkframework.config.Prefixes
import org.silkframework.entity._
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.users.WebUserManager
import org.silkframework.runtime.validation.NotFoundException
import org.silkframework.workspace.activity.transform.{TransformPathsCache, VocabularyCache}
import org.silkframework.workspace.{ProjectTask, User}
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.mvc.{Action, AnyContent, Controller}

import scala.language.implicitConversions
import scala.util.Try

/**
  * Generates auto completions for mapping paths and types.
  */
class AutoCompletionApi extends Controller {
  val log: Logger = Logger.getLogger(this.getClass.getName)

  /**
    * Given a search term, returns all possible completions for source property paths.
    */
  def sourcePaths(projectName: String, taskName: String, ruleName: String, term: String, maxResults: Int): Action[AnyContent] = Action { request =>
    implicit val userContext: UserContext = WebUserManager().userContext(request)
    val project = User().workspace.project(projectName)
    implicit val prefixes: Prefixes = project.config.prefixes
    val task = project.task[TransformSpec](taskName)
    var completions = Completions()
    task.nestedRuleAndSourcePath(ruleName) match {
      case Some((_, sourcePath)) =>
        val simpleSourcePath = sourcePath.filter(op => op.isInstanceOf[ForwardOperator] || op.isInstanceOf[BackwardOperator])
        val forwardOnlySourcePath = forwardOnlyPath(simpleSourcePath)
        val allPaths = pathsCacheCompletions(task, simpleSourcePath)
        val isRdfInput = task.activity[TransformPathsCache].value.isRdfInput(task)
        // FIXME: No only generate relative "forward" paths, but also generate paths that would be accessible by following backward paths.
        val relativeForwardPaths = relativePaths(simpleSourcePath, forwardOnlySourcePath, allPaths, isRdfInput)
        // Add known paths
        completions += relativeForwardPaths
        // Return filtered result
        Ok(completions.filterAndSort(term, maxResults, sortEmptyTermResult = false).toJson)
      case None =>
        throw new NotFoundException("Requesting auto-completion for non-existent rule " + ruleName + " in transformation task " + taskName + "!")
    }
  }

  /** Filter out paths that start with either the simple source or forward only source path, then
    * rewrite the auto-completion to a relative path from the full paths. */
  private def relativePaths(simpleSourcePath: List[PathOperator],
                            forwardOnlySourcePath: List[PathOperator],
                            pathCacheCompletions: Completions,
                            isRdfInput: Boolean)
                           (implicit prefixes: Prefixes): Seq[Completion] = {
    pathCacheCompletions.values.filter { p =>
      val path = Path.parse(p.value)
      isRdfInput || // FIXME: Currently there are no paths longer 1 in cache, that why return full path
      path.operators.startsWith(forwardOnlySourcePath) && path.operators.size > forwardOnlySourcePath.size ||
      path.operators.startsWith(simpleSourcePath) && path.operators.size > simpleSourcePath.size
    } map { completion =>
      val path = Path.parse(completion.value)
      val truncatedOps = if (path.operators.startsWith(forwardOnlySourcePath)) {
        path.operators.drop(forwardOnlySourcePath.size)
      } else if(isRdfInput) {
        path.operators
      } else {
        path.operators.drop(simpleSourcePath.size)
      }
      completion.copy(value = Path(truncatedOps).serialize())
    }
  }

  // Normalize this path by eliminating backward operators
  private def forwardOnlyPath(simpleSourcePath: List[PathOperator]) = {
    // Remove BackwardOperators
    var pathStack = List.empty[PathOperator]
    for (op <- simpleSourcePath) {
      op match {
        case f: ForwardOperator =>
          pathStack ::= f
        case BackwardOperator(prop) =>
          if (pathStack.isEmpty) {
            // TODO: What to do?
          } else {
            pathStack = pathStack.tail
          }
        case _ =>
          throw new IllegalArgumentException("Path cannot contain path operators other than forward and backward operators!")
      }
    }
    pathStack.reverse

  }

  /**
    * Given a search term, returns possible completions for target paths.
    *
    * @param projectName The name of the project
    * @param taskName    The name of the transformation
    * @param term        The search term
    * @return
    */
  def targetProperties(projectName: String, taskName: String, ruleName: String, term: String, maxResults: Int): Action[AnyContent] = Action { request =>
    implicit val userContext: UserContext = WebUserManager().userContext(request)
    val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    var completions = vocabularyPropertyCompletions(task)
    // Removed as they currently cannot be edited in the UI: completions += prefixCompletions(project.config.prefixes)

    Ok(completions.filterAndSort(term, maxResults).toJson)
  }

  /**
    * Given a search term, returns possible completions for target paths.
    *
    * @param projectName The name of the project
    * @param taskName    The name of the transformation
    * @param term        The search term
    * @return
    */
  def targetTypes(projectName: String, taskName: String, ruleName: String, term: String, maxResults: Int): Action[AnyContent] = Action { request =>
    implicit val userContext: UserContext = WebUserManager().userContext(request)
    val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    var completions = vocabularyTypeCompletions(task)
    // Removed as they currently cannot be edited in the UI: completions += prefixCompletions(project.config.prefixes)

    Ok(completions.filterAndSort(term, maxResults).toJson)
  }

  /**
    * Retrieves completions for prefixes.
    *
    * @return The completions, sorted alphabetically
    */
  private def prefixCompletions(prefixes: Prefixes): Completions = {
    Completions(
      for(prefix <- prefixes.prefixMap.keys.toSeq.sorted) yield {
        Completion(
          value = prefix + ":",
          label = Some(prefix + ":"),
          description = None,
          category = Categories.prefixes,
          isCompletion = true
        )
      }
    )
  }

  private def pathsCacheCompletions(task: ProjectTask[TransformSpec], sourcePath: List[PathOperator])
                                   (implicit userContext: UserContext): Completions = {
    if (Option(task.activity[TransformPathsCache].value).isDefined) {
      val paths = fetchCachedPaths(task, sourcePath)
      val serializedPaths = paths.map(_.serialize()(task.project.config.prefixes)).sorted.distinct
      for(pathStr <- serializedPaths) yield {
        Completion(
          value = pathStr,
          label = None,
          description = None,
          category = Categories.sourcePaths,
          isCompletion = true
        )
      }
    } else {
      Completions()
    }
  }

  private def fetchCachedPaths(task: ProjectTask[TransformSpec], sourcePath: List[PathOperator])
                              (implicit userContext: UserContext): IndexedSeq[TypedPath] = {
    val cachedSchemata = task.activity[TransformPathsCache].value
    cachedSchemata.fetchCachedPaths(task, sourcePath)
  }

  private def vocabularyTypeCompletions(task: ProjectTask[TransformSpec]): Completions = {
    val prefixes = task.project.config.prefixes
    val vocabularyCache = task.activity[VocabularyCache].value

    val typeCompletions =
      for(vocab <- vocabularyCache.vocabularies; vocabClass <- vocab.classes) yield {
        Completion(
          value = prefixes.shorten(vocabClass.info.uri.toString),
          label = vocabClass.info.label,
          description = vocabClass.info.description,
          category = Categories.vocabularyTypes,
          isCompletion = true
        )
      }

    typeCompletions.distinct
  }

  private def vocabularyPropertyCompletions(task: ProjectTask[TransformSpec]): Completions = {
    val prefixes = task.project.config.prefixes
    val vocabularyCache = task.activity[VocabularyCache].value

    val propertyCompletions =
      for(vocab <- vocabularyCache.vocabularies; prop <- vocab.properties) yield {
        Completion(
          value = prefixes.shorten(prop.info.uri.toString),
          label = prop.info.label,
          description = prop.info.description,
          category = Categories.vocabularyProperties,
          isCompletion = true
        )
      }

    propertyCompletions.distinct
  }

  // Characters that are removed before comparing (in addition to whitespaces)
  private val ignoredCharacters = Set('/', '\\')

  /**
    * Normalizes a term.
    */
  private def normalizeTerm(term: String): String = {
    term.toLowerCase.filterNot(c => c.isWhitespace || ignoredCharacters.contains(c))
  }

  private implicit def createCompletion(completions: Seq[Completion]): Completions = Completions(completions)

  /**
    * A list of auto completions.
    */
  case class Completions(values: Seq[Completion] = Seq.empty) {

    /**
      * Adds another list of completions to this one and returns the result.
      */
    def +(completions: Completions): Completions = {
      Completions(values ++ completions.values)
    }

    /**
      * Filters and ranks all completions using a search term.
      */
    def filterAndSort(term: String, maxResults: Int, sortEmptyTermResult: Boolean = true): Completions = {
      if (term.isEmpty) {
        // If the term is empty, return some completions anyway
        val sortedValues = if(sortEmptyTermResult) values.sortBy(_.labelOrGenerated.length) else values
        Completions(sortedValues.take(maxResults))
      } else {
        // Filter all completions that match the search term and sort them by score
        val normalizedTerm = normalizeTerm(term)
        val scoredValues = for(value <- values; score <- value.matches(normalizedTerm)) yield (value, score)
        val sortedValues = scoredValues.sortBy(-_._2).map(_._1)
        Completions(sortedValues.take(maxResults))
      }
    }

    def toJson: JsValue = {
      JsArray(values.map(_.toJson))
    }

  }

  /**
    * A single completion.
    *
    * @param value The value to be filled if the user selects this completion.
    * @param confidence The confidence of this completion.
    * @param label A user readable label if available
    * @param description A user readable description if available
    * @param category The category to be shown in the autocompletion
    * @param isCompletion True, if this is a valid completion. False, if this is a (error) message.
    */
  case class Completion(value: String,
                        confidence: Double = Double.MinValue,
                        label: Option[String],
                        description: Option[String],
                        category: String,
                        isCompletion: Boolean) {

    /**
      * Returns the label if present or generates a label from the value if no label is set.
      */
    lazy val labelOrGenerated: String = label match {
      case Some(existingLabel) =>
        existingLabel
      case None =>
        val lastPart = value.substring(value.lastIndexWhere(c => c == '#' || c == '/' || c == ':') + 1).filterNot(_ == '>')
        Try(URLDecoder.decode(lastPart, "UTF8")).getOrElse(lastPart)
    }

    /**
      * Checks if a term matches this completion.
      *
      * @param normalizedTerm the term normalized using normalizeTerm(term)
      * @return None, if the term does not match at all.
      *         Some(matchScore), if the terms match.
      */
    def matches(normalizedTerm: String): Option[Double] = {
      val values = Set(value, labelOrGenerated) ++ description
      val scores = values.flatMap(rank(normalizedTerm))
      if(scores.isEmpty)
        None
      else
        Some(scores.max)
    }

    /**
      * Ranks a term, the higher the result the higher the ranking.
      */
    private def rank(normalizedTerm: String)(value: String): Option[Double] = {
      val normalizedValue = normalizeTerm(value)
      if(normalizedValue.contains(normalizedTerm)) {
        Some(normalizedTerm.length.toDouble / normalizedValue.length)
      } else {
        None
      }
    }

    def toJson: JsValue = {
      Json.obj(
        "value" -> value,
        "label" -> labelOrGenerated,
        "description" -> description,
        "category" -> category,
        "isCompletion" -> isCompletion
      )
    }

  }


}

object AutoCompletionApi {

  /**
    * The names of the auto completion categories.
    */
  object Categories {

    val prefixes = "Prefixes"

    val sourcePaths = "Source Paths"

    val vocabularyTypes = "Vocabulary Types"

    val vocabularyProperties = "Vocabulary Properties"

  }

}


