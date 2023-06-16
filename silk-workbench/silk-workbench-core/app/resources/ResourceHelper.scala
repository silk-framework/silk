package resources

import org.silkframework.config.TaskSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.WritableResource
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.util.Identifier
import org.silkframework.workbench.utils.{ErrorResult, UnsupportedMediaTypeException}
import org.silkframework.workspace.{Project, ProjectTask, WorkspaceFactory}
import play.api.libs.Files
import play.api.mvc.Results.NoContent
import org.silkframework.runtime.resource.UrlResource

import scala.collection.mutable
import play.api.mvc._

import java.net.URL
import java.util.logging.Logger

object ResourceHelper {

  private val log: Logger = Logger.getLogger(this.getClass.getName)

  /** Refresh all caches that depend changes of a specific resource. */
  def refreshCachesOfDependingTasks(resourceName: String,
                                    project: Project)
                                   (implicit userContext: UserContext): Unit = {
    // The tasks depending on the resource that were actually updated.
    val updatedResourceTasks = mutable.Set[Identifier]()
    tasksDependingOnResource(resourceName, project).foreach { task =>
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
    }
  }

  /** Find all tasks that depend on a resource. */
  def tasksDependingOnResource(resourcePath: String, project: Project)
                              (implicit userContext: UserContext): Seq[ProjectTask[_ <: TaskSpec]] = {
    val p = project.resources.getInPath(resourcePath)
    project.allTasks
      .filter(
        _.referencedResources.exists(ref =>
          ref.path == p.path))
  }

  /**
    * Uploads a resource from the request body.
    */
  def uploadResource(projectName: String, resourceName: String)
                    (implicit request: Request[AnyContent], user: UserContext): Result = {
    val project = WorkspaceFactory().workspace.project(projectName)
    val resource = project.resources.get(resourceName)

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
      log.info(s"Created/updated resource '$resourceName' in project '$projectName'. " + user.logInfo)
      ResourceHelper.refreshCachesOfDependingTasks(resourceName, project)
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
