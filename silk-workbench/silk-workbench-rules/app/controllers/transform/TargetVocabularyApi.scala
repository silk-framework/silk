package controllers.transform

import controllers.core.UserContextActions
import controllers.core.util.ControllerUtilsTrait
import controllers.transform.doc.TargetVocabularyApiDoc
import controllers.transform.vocabulary.SourcePropertiesOfClassRequest
import controllers.util.SerializationUtils._
import controllers.workspace.workspaceRequests.{VocabularyInfo, VocabularyInfos}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.silkframework.config.Prefixes
import org.silkframework.rule.TransformSpec
import org.silkframework.rule.vocab.{Vocabularies, VocabularyClass, VocabularyProperty}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.runtime.validation.NotFoundException
import org.silkframework.serialization.json.JsonSerializers
import org.silkframework.util.Uri
import org.silkframework.workbench.utils.ErrorResult
import org.silkframework.workspace.activity.transform.{TransformPathsCache, VocabularyCache, VocabularyCacheValue}
import org.silkframework.workspace.{Project, WorkspaceFactory}
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Action, AnyContent, InjectedController}

import javax.inject.Inject
import scala.util.{Success, Try}

@Tag(name = "Transform target vocabulary", description = "Provides access to the target vocabulary.")
class TargetVocabularyApi  @Inject() () extends InjectedController with UserContextActions with ControllerUtilsTrait {

  @Operation(
    summary = "Target vocabulary type",
    description = "Retrieves information about a type from the target vocabularies.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Target vocabulary type",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(TargetVocabularyApiDoc.typeInfoExample))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project, task or type has not been found."
      )
  ))
  def getTypeInfo(@Parameter(
                    name = "project",
                    description = "The project identifier",
                    required = true,
                    in = ParameterIn.PATH,
                    schema = new Schema(implementation = classOf[String])
                  )
                  projectName: String,
                  @Parameter(
                    name = "task",
                    description = "The task identifier",
                    required = true,
                    in = ParameterIn.PATH,
                    schema = new Schema(implementation = classOf[String])
                  )
                  transformTaskName: String,
                  @Parameter(
                    name = "uri",
                    description = "The URI of the type. May be a prefixed name.",
                    required = true,
                    in = ParameterIn.QUERY,
                    schema = new Schema(implementation = classOf[String])
                  )
                  typeUri: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val (project, task) = projectAndTask[TransformSpec](projectName, transformTaskName)
    val vocabularies = VocabularyCacheValue.targetVocabularies(task)
    val fullTypeUri = Uri.parse(typeUri, project.config.prefixes)

    vocabularies.findClass(fullTypeUri.uri) match {
      case Some(vocabType) =>
        serializeCompileTime(vocabType, Some(project))
      case None =>
        ErrorResult(NotFoundException(s"Type $typeUri could not be found in any of the target vocabularies."))
    }
  }

  @Operation(
    summary = "Target vocabulary property",
    description = "Retrieves information about a property from the target vocabularies.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Target vocabulary property",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(TargetVocabularyApiDoc.propertyInfoExample))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project, task or property has not been found."
      )
  ))
  def getPropertyInfo(@Parameter(
                        name = "project",
                        description = "The project identifier",
                        required = true,
                        in = ParameterIn.PATH,
                        schema = new Schema(implementation = classOf[String])
                      )
                      projectName: String,
                      @Parameter(
                        name = "task",
                        description = "The task identifier",
                        required = true,
                        in = ParameterIn.PATH,
                        schema = new Schema(implementation = classOf[String])
                      )
                      transformTaskName: String,
                      @Parameter(
                        name = "uri",
                        description = "The URI of the property. May be a prefixed name.",
                        required = true,
                        in = ParameterIn.QUERY,
                        schema = new Schema(implementation = classOf[String])
                      )
                      propertyUri: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val (project, task) = projectAndTask[TransformSpec](projectName, transformTaskName)
    val vocabularies = VocabularyCacheValue.targetVocabularies(task)
    val fullPropertyUri = Uri.parse(propertyUri, project.config.prefixes)

    vocabularies.findProperty(fullPropertyUri.uri) match {
      case Some(vocabProperty) =>
        serializeCompileTime(vocabProperty, Some(project))
      case None =>
        ErrorResult(NotFoundException(s"Property $propertyUri could not be found in any of the target vocabularies."))
    }
  }

  @Operation(
    summary = "Target vocabulary type or property",
    description = "Retrieves information about a type or a property from the target vocabularies. This endpoint can be used if it is not known whether the given URI represents a type or a property. Otherwise, the /type and /property endpoints should be prefered.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Target vocabulary type or property",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(TargetVocabularyApiDoc.propertyInfoExample))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project, task has not been found or there is no type or property with the specified URI."
      )
  ))
  def getTypeOrPropertyInfo(@Parameter(
                              name = "project",
                              description = "The project identifier",
                              required = true,
                              in = ParameterIn.PATH,
                              schema = new Schema(implementation = classOf[String])
                            )
                            projectName: String,
                            @Parameter(
                              name = "task",
                              description = "The task identifier",
                              required = true,
                              in = ParameterIn.PATH,
                              schema = new Schema(implementation = classOf[String])
                            )
                            transformTaskName: String,
                            @Parameter(
                              name = "uri",
                              description = "The URI of the type or property. May be a prefixed name.",
                              required = true,
                              in = ParameterIn.QUERY,
                              schema = new Schema(implementation = classOf[String])
                            )
                            uri: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val (project, task) = projectAndTask[TransformSpec](projectName, transformTaskName)
    val vocabularies = VocabularyCacheValue.targetVocabularies(task)
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

  @Operation(
    summary = "Target vocabulary properties by class",
    description = "Get all properties that the given class or any of its parent classes is the domain of in the corresponding target vocabulary.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(TargetVocabularyApiDoc.propertiesByClassExample))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project, task or class has not been found."
      )
  ))
  def propertiesByType(@Parameter(
                         name = "project",
                         description = "The project identifier",
                         required = true,
                         in = ParameterIn.PATH,
                         schema = new Schema(implementation = classOf[String])
                       )
                       projectName: String,
                       @Parameter(
                         name = "task",
                         description = "The task identifier",
                         required = true,
                         in = ParameterIn.PATH,
                         schema = new Schema(implementation = classOf[String])
                       )
                       taskName: String,
                       @Parameter(
                         name = "uri",
                         description = "The URI of the class.",
                         required = true,
                         in = ParameterIn.QUERY,
                         schema = new Schema(implementation = classOf[String])
                       )
                       classUri: String): Action[AnyContent] = RequestUserContextAction { implicit request => implicit userContext =>
    implicit val project: Project = WorkspaceFactory().workspace.project(projectName)
    val (vocabularyProps, _) = vocabularyPropertiesByType(taskName, project, fullClassUri(classUri, project.config.prefixes), addBackwardRelations = false)
    serializeIterableCompileTime(vocabularyProps, containerName = Some("Properties"))
  }

  @Operation(
    summary = "Source vocabulary properties by class",
    description = "Get all properties that the given class or any of its parent classes are the domain of in the vocabulary.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(TargetVocabularyApiDoc.propertiesByClassExample))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project, task or class has not been found."
      )
    ))
  def sourcePropertiesByType(@Parameter(
                               name = "project",
                               description = "The project identifier",
                               required = true,
                               in = ParameterIn.PATH,
                               schema = new Schema(implementation = classOf[String])
                             )
                             projectName: String,
                             @Parameter(
                               name = "task",
                               description = "The task identifier",
                               required = true,
                               in = ParameterIn.PATH,
                               schema = new Schema(implementation = classOf[String])
                             )
                             taskName: String): Action[JsValue] = RequestUserContextAction(parse.json) { implicit request =>
    implicit userContext =>
      validateJson[SourcePropertiesOfClassRequest] { sourcePropertiesOfClassRequest =>
        implicit val (project, transformTask) = projectAndTask[TransformSpec](projectName, taskName)
        var (vocabularyProps, _) = vocabularyPropertiesByType(taskName, project, fullClassUri(sourcePropertiesOfClassRequest.classUri, project.config.prefixes),
          addBackwardRelations = false, fromGlobalVocabularyCache = true)
        if(sourcePropertiesOfClassRequest.fromPathCacheOnly) {
          val pathsCache = transformTask.activity[TransformPathsCache]
          pathsCache.value.get match {
            case Some(cacheValue) =>
              val rootProperties = cacheValue.configuredSchema.typedPaths.flatMap(_.property.map(_.propertyUri)).toSet
              val otherProperties = cacheValue.untypedSchema.toSeq.flatMap(_.typedPaths.flatMap(_.property.map(_.propertyUri))).toSet
              val allProperties = rootProperties ++ otherProperties
              vocabularyProps = vocabularyProps.filter(p => allProperties.contains(p.info.uri))
            case None =>
              // Do not filter
          }
        }
        serializeIterableCompileTime(vocabularyProps, containerName = Some("Properties"))
      }
  }

  private def fullClassUri(classUri: String, prefixes: Prefixes): String = {
    Try(prefixes.resolve(classUri)) match {
      case Success(resolvedUri) =>
        resolvedUri
      case _ =>
        classUri
    }
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
                                         addBackwardRelations: Boolean,
                                         fromGlobalVocabularyCache: Boolean = false)
                                        (implicit userContext: UserContext): (Seq[VocabularyProperty], Seq[VocabularyProperty]) = {
    val task = project.task[TransformSpec](taskName)
    val vocabularies: Vocabularies = if(fromGlobalVocabularyCache) {
      Vocabularies(VocabularyCacheValue.globalVocabularies)
    } else {
      VocabularyCacheValue.targetVocabularies(task)
    }
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
  implicit private val writeContext: WriteContext[JsValue] = WriteContext.empty[JsValue]
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

  @Operation(
    summary = "Target vocabulary object properties by class",
    description = "Get all direct relations of a class or one of its parent classes to other classes from the vocabulary.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(TargetVocabularyApiDoc.relationsOfClassExample))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project, task or class has not been found."
      )
  ))
  def relationsOfType(@Parameter(
                        name = "project",
                        description = "The project identifier",
                        required = true,
                        in = ParameterIn.PATH,
                        schema = new Schema(implementation = classOf[String])
                      )
                      projectName: String,
                      @Parameter(
                        name = "task",
                        description = "The task identifier",
                        required = true,
                        in = ParameterIn.PATH,
                        schema = new Schema(implementation = classOf[String])
                      )
                      taskName: String,
                      @Parameter(
                        name = "uri",
                        description = "The URI of the class.",
                        required = true,
                        in = ParameterIn.QUERY,
                        schema = new Schema(implementation = classOf[String])
                      )
                      classUri: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    implicit val project: Project = getProject(projectName)
    // Filter only object properties
    val (forwardProperties, backwardProperties) = vocabularyPropertiesByType(taskName, project, fullClassUri(classUri, project.config.prefixes), addBackwardRelations = true)
    val forwardObjectProperties = forwardProperties.filter(vp => vp.range.isDefined && vp.domain.isDefined)
    val f = forwardObjectProperties map (fp => vocabularyPropertyToRelation(fp, forward = true))
    val b = backwardProperties map (bp => vocabularyPropertyToRelation(bp, forward = false))
    val classRelations = ClassRelations(f, b)
    Ok(Json.toJson(classRelations))
  }

  @Operation(
    summary = "Target vocabulary information",
    description = "Returns high-level information, e.g. label, class/property statistics, about the accessible vocabularies for that transform task.",
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "Success",
        content = Array(
          new Content(
            mediaType = "application/json",
            examples = Array(new ExampleObject(TargetVocabularyApiDoc.targetVocabularyExample))
          )
        )
      ),
      new ApiResponse(
        responseCode = "404",
        description = "If the specified project or task has not been found."
      )
    ))
  def vocabularyInfos(@Parameter(
                        name = "project",
                        description = "The project identifier",
                        required = true,
                        in = ParameterIn.PATH,
                        schema = new Schema(implementation = classOf[String])
                      )
                      projectId: String,
                      @Parameter(
                        name = "task",
                        description = "The task identifier",
                        required = true,
                        in = ParameterIn.PATH,
                        schema = new Schema(implementation = classOf[String])
                      )
                      transformTaskId: String): Action[AnyContent] = UserContextAction { implicit userContext =>
    implicit val (project, transformTask) = projectAndTask[TransformSpec](projectId, transformTaskId)
    transformTask.activity[VocabularyCache].control.waitUntilFinished()
    val vocabularies = VocabularyCacheValue.targetVocabularies(transformTask)
    val vocabInfoSeq = vocabularies map { vocab =>
      val label = vocab.info.label.orElse(vocab.info.altLabels.headOption)
      VocabularyInfo(vocab.info.uri, label, nrClasses = vocab.classes.size, nrProperties = vocab.properties.size)
    }
    Ok(Json.toJson(VocabularyInfos(vocabInfoSeq)))
  }
}