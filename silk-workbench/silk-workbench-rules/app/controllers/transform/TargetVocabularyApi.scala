package controllers.transform

import controllers.core.util.ControllerUtilsTrait
import org.silkframework.rule.TransformSpec
import org.silkframework.workspace.{Project, User}
import org.silkframework.workspace.activity.transform.VocabularyCache
import play.api.mvc.{Action, AnyContent, Controller}
import controllers.util.SerializationUtils._
import org.silkframework.rule.vocab.{VocabularyClass, VocabularyProperty}
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.JsonSerializers
import org.silkframework.util.Uri
import play.api.libs.json.{JsArray, JsValue, Json, Writes}

/**
  * Provides access to the target vocabulary.
  */
class TargetVocabularyApi extends Controller with ControllerUtilsTrait {

  /** Returns meta data for a vocabulary class */
  def getTypeInfo(projectName: String, transformTaskName: String, typeUri: String): Action[AnyContent] = Action { implicit request =>
    implicit val (project, task) = projectAndTask[TransformSpec](projectName, transformTaskName)
    val vocabularies = task.activity[VocabularyCache].value
    val fullTypeUri = Uri.parse(typeUri, project.config.prefixes)

    vocabularies.findClass(fullTypeUri.uri) match {
      case Some(vocabType) =>
        serializeCompileTime(vocabType)
      case None =>
        NotFound(s"Type $typeUri could not be found in any of the target vocabularies.")
    }
  }

  /** Returns meta data for a vocabulary property */
  def getPropertyInfo(projectName: String, transformTaskName: String, propertyUri: String): Action[AnyContent] = Action { implicit request =>
    implicit val (project, task) = projectAndTask[TransformSpec](projectName, transformTaskName)
    val vocabularies = task.activity[VocabularyCache].value
    val fullPropertyUri = Uri.parse(propertyUri, project.config.prefixes)

    vocabularies.findProperty(fullPropertyUri.uri) match {
      case Some(vocabProperty) =>
        serializeCompileTime(vocabProperty)
      case None =>
        NotFound(s"Property $propertyUri could not be found in any of the target vocabularies.")
    }
  }

  /**
    * Returns all properties that are in the domain of the given class or one of its super classes.
    * @param projectName Name of project
    * @param taskName    Name of task
    * @param classUri    Class URI
    */
  def propertiesByType(projectName: String, taskName: String, classUri: String): Action[AnyContent] = Action { implicit request =>
    implicit val project = User().workspace.project(projectName)
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
                                         addBackwardRelations: Boolean): (Seq[VocabularyProperty], Seq[VocabularyProperty]) = {
    val task = project.task[TransformSpec](taskName)
    val vocabularies = task.activity[VocabularyCache].value
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
  implicit private val writeContext = WriteContext[JsValue]()
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
  implicit private val relationFormat = Json.writes[Relation]
  implicit private val classRelationsFormat = Json.writes[ClassRelations]

  // Depending on the forward switch either the range or the domain is taken for the classUri.
  private def vocabularyPropertyToRelation(vocabularyProperty: VocabularyProperty, forward: Boolean): Relation = {
    val targetClass = if(forward) { vocabularyProperty.range } else { vocabularyProperty.domain }
    assert(targetClass.isDefined, "No target class defined for relation property " + vocabularyProperty.info.uri)
    Relation(vocabularyProperty, targetClass.get)
  }

  def relationsOfType(projectName: String, taskName: String, classUri: String): Action[AnyContent] = Action { implicit request =>
    implicit val project = getProject(projectName)
    // Filter only object properties
    val (forwardProperties, backwardProperties) = vocabularyPropertiesByType(taskName, project, classUri, addBackwardRelations = true)
    val forwardObjectProperties = forwardProperties.filter(vp => vp.range.isDefined && vp.domain.isDefined)
    val f = forwardObjectProperties map (fp => vocabularyPropertyToRelation(fp, forward = true))
    val b = backwardProperties map (bp => vocabularyPropertyToRelation(bp, forward = false))
    val classRelations = ClassRelations(f, b)
    Ok(Json.toJson(classRelations))
  }

}
