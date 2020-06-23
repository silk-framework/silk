package controllers.transform

import controllers.core.{RequestUserContextAction, UserContextAction}
import controllers.core.util.ControllerUtilsTrait
import controllers.util.SerializationUtils._
import javax.inject.Inject
import org.silkframework.rule.TransformSpec
import org.silkframework.rule.vocab.{VocabularyClass, VocabularyProperty}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.runtime.validation.NotFoundException
import org.silkframework.serialization.json.JsonSerializers
import org.silkframework.util.Uri
import org.silkframework.workbench.utils.ErrorResult
import org.silkframework.workspace.activity.transform.VocabularyCache
import org.silkframework.workspace.{Project, WorkspaceFactory}
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Action, AnyContent, InjectedController}

/**
  * Provides access to the target vocabulary.
  */
class TargetVocabularyApi  @Inject() () extends InjectedController with ControllerUtilsTrait {

  /** Returns meta data for a vocabulary class */
  def getTypeInfo(projectName: String, transformTaskName: String, typeUri: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val (project, task) = projectAndTask[TransformSpec](projectName, transformTaskName)
    val vocabularies = task.activity[VocabularyCache].value()
    val fullTypeUri = Uri.parse(typeUri, project.config.prefixes)

    vocabularies.findClass(fullTypeUri.uri) match {
      case Some(vocabType) =>
        serializeCompileTime(vocabType, Some(project))
      case None =>
        ErrorResult(NotFoundException(s"Type $typeUri could not be found in any of the target vocabularies."))
    }
  }

  /** Returns meta data for a vocabulary property */
  def getPropertyInfo(projectName: String,
                      transformTaskName: String,
                      propertyUri: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val (project, task) = projectAndTask[TransformSpec](projectName, transformTaskName)
    val vocabularies = task.activity[VocabularyCache].value()
    val fullPropertyUri = Uri.parse(propertyUri, project.config.prefixes)

    vocabularies.findProperty(fullPropertyUri.uri) match {
      case Some(vocabProperty) =>
        serializeCompileTime(vocabProperty, Some(project))
      case None =>
        ErrorResult(NotFoundException(s"Property $propertyUri could not be found in any of the target vocabularies."))
    }
  }

  /**
    * Returns metadata for a vocabulary class or property.
    */
  def getTypeOrPropertyInfo(projectName: String,
                            transformTaskName: String,
                            uri: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val (project, task) = projectAndTask[TransformSpec](projectName, transformTaskName)
    val vocabularies = task.activity[VocabularyCache].value()
    val fullUri = Uri.parse(uri, project.config.prefixes)

    (vocabularies.findClass(fullUri.uri), vocabularies.findProperty(fullUri.uri)) match {
      case (Some(vocabType), None) =>
        serializeCompileTime(vocabType, Some(project))
      case (None, Some(vocabProperty)) =>
        serializeCompileTime(vocabProperty, Some(project))
      case (None, None) =>
        ErrorResult(NotFoundException(s"Property or Class $uri could not be found in any of the target vocabularies."))
      case (Some(_), Some(_)) =>
        ErrorResult(INTERNAL_SERVER_ERROR, "Vocabulary Issue", s"For $uri both a class and a property can be found in the target vocabularies.")
    }
  }

  /**
    * Returns all properties that are in the domain of the given class or one of its super classes.
    * @param projectName Name of project
    * @param taskName    Name of task
    * @param classUri    Class URI
    */
  def propertiesByType(projectName: String, taskName: String, classUri: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val project: Project = WorkspaceFactory().workspace.project(projectName)
    val (vocabularyProps, _) = vocabularyPropertiesByType(taskName, project, classUri, addBackwardRelations = false)
    serializeIterableCompileTime(vocabularyProps, containerName = Some("Properties"))
  }

  /**
    * Returns all properties that are directly defined on a class or one of its parent classes.
    * @param classUri The class we want to have the relations for.
    * @param addBackwardRelations Specifies if backward relations should be added
    * @return Tuple of (forward properties, backward properties)
    */
  private def vocabularyPropertiesByType(taskName: String,
                                         project: Project,
                                         classUri: String,
                                         addBackwardRelations: Boolean)
                                        (implicit userContext: UserContext): (Seq[VocabularyProperty], Seq[VocabularyProperty]) = {
    val task = project.task[TransformSpec](taskName)
    val vocabularies = task.activity[VocabularyCache].value()
    val vocabularyClasses = vocabularies.flatMap(v => v.getClass(classUri).map(c => (v, c)))

    def filterProperties(propFilter: (VocabularyProperty, List[String]) => Boolean): Seq[VocabularyProperty] = {
      val props = for ((vocabulary, vocabularyClass) <- vocabularyClasses) yield {
        val classes = (vocabularyClass.info.uri :: vocabularyClass.parentClasses.toList).distinct
        val propsByAnyClass = vocabulary.properties.filter(propFilter(_, classes))
        propsByAnyClass
      }
      props.flatten
    }

    val forwardProperties = filterProperties((prop, classes) => prop.domain.exists(vc => classes.contains(vc.info.uri)))
    val backwardProperties = filterProperties((prop, classes) => addBackwardRelations && prop.range.exists(vc => classes.contains(vc.info.uri)))
    (forwardProperties, backwardProperties)
  }

  case class ClassRelations(forwardRelations: Seq[Relation], backwardRelations: Seq[Relation])

  case class Relation(property: VocabularyProperty, targetClass: VocabularyClass)

  /** Json serializers */
  implicit private val writeContext: WriteContext[JsValue] = WriteContext[JsValue](projectId = None)
  implicit private object vocabularyClassFormat extends Writes[VocabularyClass] {
    override def writes(vocabularyClass: VocabularyClass): JsValue = {
      JsonSerializers.VocabularyClassJsonFormat.write(vocabularyClass)
    }
  }
  implicit private object vocabularyPropertyFormat extends Writes[VocabularyProperty] {
    override def writes(vocabularyProperty: VocabularyProperty): JsValue = {
      JsonSerializers.VocabularyPropertyJsonFormat.write(vocabularyProperty)
    }
  }
  implicit private val relationFormat: Writes[Relation] = Json.writes[Relation]
  implicit private val classRelationsFormat: Writes[ClassRelations] = Json.writes[ClassRelations]

  // Depending on the forward switch either the range or the domain is taken for the classUri.
  private def vocabularyPropertyToRelation(vocabularyProperty: VocabularyProperty, forward: Boolean): Relation = {
    val targetClass = if(forward) { vocabularyProperty.range } else { vocabularyProperty.domain }
    assert(targetClass.isDefined, "No target class defined for relation property " + vocabularyProperty.info.uri)
    Relation(vocabularyProperty, targetClass.get)
  }

  def relationsOfType(projectName: String, taskName: String, classUri: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    implicit val project: Project = getProject(projectName)
    // Filter only object properties
    val (forwardProperties, backwardProperties) = vocabularyPropertiesByType(taskName, project, classUri, addBackwardRelations = true)
    val forwardObjectProperties = forwardProperties.filter(vp => vp.range.isDefined && vp.domain.isDefined)
    val f = forwardObjectProperties map (fp => vocabularyPropertyToRelation(fp, forward = true))
    val b = backwardProperties map (bp => vocabularyPropertyToRelation(bp, forward = false))
    val classRelations = ClassRelations(f, b)
    Ok(Json.toJson(classRelations))
  }

}
