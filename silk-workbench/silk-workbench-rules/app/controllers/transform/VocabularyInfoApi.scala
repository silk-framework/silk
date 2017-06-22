package controllers.transform

import org.silkframework.rule.TransformSpec
import org.silkframework.workspace.User
import org.silkframework.workspace.activity.transform.VocabularyCache
import play.api.mvc.{Action, Controller}
import controllers.util.SerializationUtils._
import org.silkframework.util.Uri

/**
  * Provides access to the target vocabulary.
  */
class VocabularyInfoApi extends Controller {

  def getTypeInfo(projectName: String, taskName: String, typeUri: String) = Action { implicit request =>
    implicit val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    val vocabularies = task.activity[VocabularyCache].value
    val fullTypeUri = Uri.parse(typeUri, project.config.prefixes)

    vocabularies.findClass(fullTypeUri.uri) match {
      case Some(vocabType) =>
        serializeCompileTime(vocabType)
      case None =>
        NotFound(s"Type $typeUri could not be found in any of the target vocabularies.")
    }
  }

  def getPropertyInfo(projectName: String, taskName: String, propertyUri: String) = Action { implicit request =>
    implicit val project = User().workspace.project(projectName)
    val task = project.task[TransformSpec](taskName)
    val vocabularies = task.activity[VocabularyCache].value
    val fullPropertyUri = Uri.parse(propertyUri, project.config.prefixes)

    vocabularies.findProperty(fullPropertyUri.uri) match {
      case Some(vocabProperty) =>
        serializeCompileTime(vocabProperty)
      case None =>
        NotFound(s"Property $propertyUri could not be found in any of the target vocabularies.")
    }
  }

}
