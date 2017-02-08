package controllers.transform

import controllers.transform.TargetPathAutcompletion.Completion
import org.silkframework.config.Prefixes
import org.silkframework.entity.Path
import org.silkframework.rule.TransformSpec
import org.silkframework.workspace.activity.transform.{MappingCandidates, VocabularyCache}
import org.silkframework.workspace.{Project, ProjectTask}
import play.api.libs.json.{JsValue, Json}

/**
  * Generates autocompletions for mapping target paths.
  */
object TargetPathAutcompletion {

  /**
    * Given a search term, returns possible completions for target paths.
    *
    * @param project The project
    * @param task The transformation
    * @param sourcePath The source path to be completed. If none, types will be suggested
    * @param term The search term
    * @return The completions as Json.
    */
  def retrieve(project: Project, task: ProjectTask[TransformSpec], sourcePath: Option[String], term: String): Seq[Completion] = {
    // The maximum number of completions that will be returned
    val maxCompletions = 30

    // Retrieve all available completions
    val completions = retrieveAllCompletions(project, task, sourcePath)

    if (term.isEmpty) {
      // If the term is empty, return some completions anyway
      completions.take(maxCompletions)
    } else {
      // Filter all completions that match the search term
      val normalizedTerm = normalizeTerm(term)
      completions.filter(_.matches(normalizedTerm))
    }
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
    val prefixCompletions = retrievePrefixCompletions(project.config.prefixes)

    mappingCompletions ++ prefixCompletions
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
      for (activity <- task.activities if classOf[MappingCandidates].isAssignableFrom(activity.valueType)) yield {
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
    mappingCandidates.flatten.sortBy(-_.confidence)
  }

  /**
    * Retrieves all available suggestions for type completions.
    * Reads from all caches that provide MappingCandidates.
    *
    * @return The suggested type completions, sorted by confidence.
    */
  private def retrieveTypeCompletions(task: ProjectTask[TransformSpec]): Seq[Completion] = {
    val prefixes = task.project.config.prefixes
    val vocabularyCache = task.activity[VocabularyCache].value
    val mappingCandidates = {
      for (activity <- task.activities if classOf[MappingCandidates].isAssignableFrom(activity.valueType)) yield {
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
  private def retrievePrefixCompletions(prefixes: Prefixes): Seq[Completion] = {
    for(prefix <- prefixes.prefixMap.keys.toSeq.sorted) yield {
      Completion(
        value = prefix + ":",
        label = Some(prefix + ":"),
        description = None,
        category = "Prefixes",
        isCompletion = true
      )
    }
  }

  /**
    * Normalizes a term.
    */
  def normalizeTerm(term: String): String = {
    term.toLowerCase.filterNot(_.isWhitespace)
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
  case class Completion(value: String, confidence: Double = Double.MinValue, label: Option[String], description: Option[String], category: String, isCompletion: Boolean) {

    /**
      * Returns the label if present or generates a label from the value if no label is set.
      */
    def labelOrGenerated: String = label match {
      case Some(existingLabel) =>
        existingLabel
      case None =>
        value.substring(value.lastIndexWhere(c => c == '#' || c == '/' || c == ':') + 1)
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


