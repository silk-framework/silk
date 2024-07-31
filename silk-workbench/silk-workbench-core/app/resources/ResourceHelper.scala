package resources

import org.silkframework.config.TaskSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{Resource, UrlResource, WritableResource}
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.util.Identifier
import org.silkframework.workbench.utils.{ErrorResult, UnsupportedMediaTypeException}
import org.silkframework.workspace.{Project, ProjectTask}
import play.api.libs.Files
import play.api.mvc.Results.NoContent
import play.api.mvc._

import java.net.URL
import java.util.logging.Logger
import scala.collection.mutable

object ResourceHelper {

  private val log: Logger = Logger.getLogger(this.getClass.getName)

  /** Refresh all caches that depend changes of a specific resource. */
  def refreshCachesOfDependingTasks(resourcePath: String,
                                    project: Project)
                                   (implicit userContext: UserContext): Unit = {
    // The tasks depending on the resource that were actually updated.
    val resource = project.resources.getInPath(resourcePath)
    val updatedResourceTasks = mutable.Set[Identifier]()
    tasksDependingOnResource(resource, project).foreach { task =>
      // Updated caches
      var cacheUpdated = false
      task.activities.foreach { activity =>
        if (activity.isDatasetRelatedCache) {
          updatedResourceTasks.add(task.id)
          activity.startDirty()
          cacheUpdated = true
        }
      }
      // Also update path caches of tasks that directly depend on any of the updated tasks
      if(cacheUpdated) {
        task.dataValueHolder.republish()
      }
      // Publish resource update to TaskSpec
      task.data.resourceUpdated(resource)
    }
  }

  /** Find all tasks that depend on a resource. */
  def tasksDependingOnResource(resource: Resource, project: Project)
                              (implicit userContext: UserContext): Seq[ProjectTask[_ <: TaskSpec]] = {
    project.allTasks
      .filter(
        _.referencedResources.exists(ref =>
          ref.path == resource.path))
  }

  /**
    * Uploads a resource from the request body.
    */
  def uploadResource(project: Project, resource: WritableResource)
                    (implicit request: Request[AnyContent], user: UserContext): Result = {
    val response = request.body match {
      case AnyContentAsMultipartFormData(formData) if formData.files.nonEmpty =>
        putResourceFromMultipartFormData(resource, formData)
      case AnyContentAsMultipartFormData(formData) if formData.dataParts.contains("resource-url") =>
        putResourceFromResourceUrl(resource, formData)
      case AnyContentAsMultipartFormData(formData) if formData.files.isEmpty =>
        // Put empty resource
        resource.writeBytes(Array[Byte]())
        NoContent
      case AnyContentAsRaw(buffer) =>
        resource.writeFile(buffer.asFile)
        NoContent
      case AnyContentAsText(txt) =>
        resource.writeString(txt)
        NoContent
      case AnyContentAsEmpty =>
        // Put empty resource
        resource.writeBytes(Array[Byte]())
        NoContent
      case _ =>
        ErrorResult(UnsupportedMediaTypeException.supportedFormats("multipart/form-data", "application/octet-stream", "text/plain"))
    }
    if (response == NoContent) { // Successfully updated
      log.info(s"Created/updated resource '$resource' in project '${project.id}'. " + user.logInfo)
      ResourceHelper.refreshCachesOfDependingTasks(resource.name, project)
    }
    response
  }

  private def putResourceFromMultipartFormData(resource: WritableResource, formData: MultipartFormData[Files.TemporaryFile]) = {
    try {
      val file = formData.files.head.ref.path.toFile
      resource.writeFile(file)
      NoContent
    } catch {
      case ex: Exception =>
        ErrorResult(BadUserInputException(ex))
    }
  }

  private def putResourceFromResourceUrl(resource: WritableResource, formData: MultipartFormData[Files.TemporaryFile]): Result = {
    try {
      val dataParts = formData.dataParts("resource-url")
      val url = dataParts.head
      val urlResource = UrlResource(new URL(url))
      resource.writeResource(urlResource)
      NoContent
    } catch {
      case ex: Exception =>
        ErrorResult(BadUserInputException(ex))
    }
  }
}
