package controllers.transform

import java.util.logging.Logger

import controllers.transform.AutoCompletionApi.Categories
import org.silkframework.config.Prefixes
import org.silkframework.entity.{BackwardOperator, ForwardOperator, Path, PathOperator}
import org.silkframework.rule.TransformSpec
import org.silkframework.workspace.activity.TaskActivity
import org.silkframework.workspace.activity.transform.{MappingCandidates, TransformPathsCache, VocabularyCache}
import org.silkframework.workspace.{Project, ProjectTask, User}
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.mvc.{Action, AnyContent, Controller}

import scala.language.implicitConversions

/**
  * Generates auto completions for mapping paths and types.
  */
class AutoCompletionApi extends Controller {
  val log: Logger = Logger.getLogger(this.getClass.getName)

  /**
    * Given a search term, returns all possible completions for source property paths.
    */
  def sourcePaths(projectName: String, taskName: String, ruleName: String, term: String, maxResults: Int): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)
    implicit val prefixes = project.config.prefixes
    val task = project.task[TransformSpec](taskName)
    var completions = Completions()
    task.nestedRuleAndSourcePath(ruleName) match {
      case Some((_, sourcePath)) =>
        val simpleSourcePath = sourcePath.filter(op => op.isInstanceOf[ForwardOperator] || op.isInstanceOf[BackwardOperator])
        val forwardOnlySourcePath = forwardOnlyPath(simpleSourcePath)
        val allPaths = pathsCacheCompletions(task)
        // FIXME: No only generate relative "forward" paths, but also generate paths that would be accessible by following backward paths.
        val relativeForwardPaths = relativePaths(simpleSourcePath, forwardOnlySourcePath, allPaths)
        // Add known paths
        completions += relativeForwardPaths
      case None =>
        log.warning("Requesting auto-completion for non-existent rule " + ruleName + " in transformation task " + taskName + "!")
    }

    // Add known prefixes last
    completions += prefixCompletions(project.config.prefixes)

    // Return filtered result
    Ok(completions.filter(term, maxResults).toJson)
  }

  /** Filter out paths that start with either the simple source or forward only source path, then
    * rewrite the auto-completion to a relative path from the full paths. */
  private def relativePaths(simpleSourcePath: List[PathOperator],
                            forwardOnlySourcePath: List[PathOperator],
                            pathCacheCompletions: Completions)
                           (implicit prefixes: Prefixes) = {
    pathCacheCompletions.values.filter { p =>
      val path = Path.parse(p.value)
      path.operators.startsWith(forwardOnlySourcePath) && path.operators.size > forwardOnlySourcePath.size ||
          path.operators.startsWith(simpleSourcePath) && path.operators.size > simpleSourcePath.size
    } map { completion =>
      val path = Path.parse(completion.value)
      val truncatedOps = if (path.operators.startsWith(forwardOnlySourcePath)) {
        path.operators.drop(forwardOnlySourcePath.size)
      } else {
        path.operators.drop(simpleSourcePath.size)
      }
      completion.copy(value = Path(truncatedOps).serializeSimplified)
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
  def targetProperties(projectName: String, taskName: String, ruleName: String, term: String, maxResults: Int): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    val completions = vocabularyPropertyCompletions(task)

    Ok(completions.filter(term, maxResults).toJson)
  }

  /**
    * Given a search term, returns possible completions for target paths.
    *
    * @param projectName The name of the project
    * @param taskName    The name of the transformation
    * @param term        The search term
    * @return
    */
  def targetTypes(projectName: String, taskName: String, ruleName: String, term: String, maxResults: Int): Action[AnyContent] = Action {
    val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    val completions = vocabularyTypeCompletions(task)

    Ok(completions.filter(term, maxResults).toJson)
  }

  /**
    * Retrieves all available completions.
    */
  private def retrieveAllCompletions(project: Project, task: ProjectTask[TransformSpec], sourcePath: Option[String]): Seq[Completion] = {
    val mappingCompletions = sourcePath match {
      case _ if task.data.targetVocabularies.isEmpty =>
        Seq(
          Completion(
            value = "",
            label = Some("No matches, transformation does not specify target vocabulary."),
            description = None,
            category = "VocabularyCache",
            isCompletion = false
          )
        )
      case Some(path) =>
        retrievePropertyCompletions(task, path)
      case None =>
        retrieveTypeCompletions(task)
    }
    //val prefixCompletions = prefixCompletions(project.config.prefixes)

    // mappingCompletions ++ prefixCompletions

    ???
  }

  /**
    * Retrieves all available suggestions for property completions.
    * Reads from all caches that provide MappingCandidates.
    *
    * @return The suggested property completions, sorted by confidence.
    */
  private def retrievePropertyCompletions(task: ProjectTask[TransformSpec], sourcePath: String): Seq[Completion] = {
    val prefixes = task.project.config.prefixes
    val vocabularyCache = task.activity[VocabularyCache].value
    val mappingCandidates = {
      for (activity <- mappingCandidateActivities(task)) yield {
        if(activity.status.failed) {
          Seq(
            Completion(
              value = "",
              label = Some(s"${activity.name} failed to load."),
              description = None,
              category = activity.name,
              isCompletion = false
            )
          )
        } else {
          val candidates = activity.value.asInstanceOf[MappingCandidates]
          val propertyCandidates = candidates.suggestProperties(Path.parse(sourcePath))
          for (propertyCandidate <- propertyCandidates) yield {
            val property = vocabularyCache.findProperty(propertyCandidate.uri.toString)
            Completion(
              value = prefixes.shorten(propertyCandidate.uri.toString),
              confidence = propertyCandidate.confidence,
              label = property.flatMap(_.info.label),
              description = property.flatMap(_.info.description),
              category = activity.name,
              isCompletion = true
            )
          }
        }
      }
    }
    mappingCandidates.flatten.sortBy(_.value).sortBy(-_.confidence)
  }

  // Returns transform task activities that calculate MappingCandidate values
  private def mappingCandidateActivities(task: ProjectTask[TransformSpec]): Seq[TaskActivity[TransformSpec, _]] = {
    task.activities filter (activity => classOf[MappingCandidates].isAssignableFrom(activity.valueType))
  }

  /**
    * Retrieves all available suggestions for type completions.
    * Reads from all caches that provide MappingCandidates.
    *
    * @return The suggested type completions, sorted by confidence.
    */
  def retrieveTypeCompletions(task: ProjectTask[TransformSpec]): Seq[Completion] = {
    val prefixes = task.project.config.prefixes
    val vocabularyCache = task.activity[VocabularyCache].value
    val mappingCandidates = {
      for (activity <- mappingCandidateActivities(task)) yield {
        if(activity.status.failed) {
          Seq(
            Completion(
              value = "",
              label = Some(s"${activity.name} failed to load."),
              description = None,
              category = activity.name,
              isCompletion = false
            )
          )
        } else {
          val candidates = activity.value.asInstanceOf[MappingCandidates]
          val typeCandidates = candidates.suggestTypes
          for (typeCandidate <- typeCandidates) yield {
            val clazz = vocabularyCache.findClass(typeCandidate.uri.toString)
            Completion(
              value = prefixes.shorten(typeCandidate.uri.toString),
              confidence = typeCandidate.confidence,
              label = clazz.flatMap(_.info.label),
              description = clazz.flatMap(_.info.description),
              category = activity.name,
              isCompletion = true
            )
          }
        }
      }
    }
    mappingCandidates.flatten.sortBy(-_.confidence)
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

  private def pathsCacheCompletions(task: ProjectTask[TransformSpec]): Completions = {
    if (Option(task.activity[TransformPathsCache].value).isDefined) {
      val paths = task.activity[TransformPathsCache].value.typedPaths
      val serializedPaths = paths.map(_.path.serializeSimplified(task.project.config.prefixes)).sorted
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

  /**
    * Normalizes a term.
    */
  private def normalizeTerm(term: String): String = {
    term.toLowerCase.filterNot(_.isWhitespace)
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
      * Filters all completions using a search term.
      */
    def filter(term: String, maxResults: Int): Completions = {
      if (term.isEmpty) {
        // If the term is empty, return some completions anyway
        Completions(values.take(maxResults))
      } else {
        // Filter all completions that match the search term
        val normalizedTerm = normalizeTerm(term)
        Completions(values.filter(_.matches(normalizedTerm)).take(maxResults))
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
    def labelOrGenerated: String = label match {
      case Some(existingLabel) =>
        existingLabel
      case None =>
        value.substring(value.lastIndexWhere(c => c == '#' || c == '/' || c == ':') + 1).filterNot(_ == '>')
    }

    /**
      * Checks if a term matches this completion.
      *
      * @param normalizedTerm the term normalized using normalizeTerm(term)
      */
    def matches(normalizedTerm: String): Boolean = {
      normalizeTerm(value).contains(normalizedTerm) || label.exists(l => normalizeTerm(l).contains(normalizedTerm))
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


