package resources

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{UrlResource, WritableResource}
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.workbench.utils.{ErrorResult, UnsupportedMediaTypeException}
import org.silkframework.workspace.Project
import org.silkframework.workspace.resources.CacheUpdaterHelper
import play.api.libs.Files
import play.api.mvc.{AnyContent, AnyContentAsEmpty, AnyContentAsMultipartFormData, AnyContentAsRaw, AnyContentAsText, MultipartFormData, Request, Result}
import play.api.mvc.Results.NoContent

import java.net.URL
import java.util.logging.Logger

object ResourceHelper {

  private val log: Logger = Logger.getLogger(this.getClass.getName)

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
      CacheUpdaterHelper.refreshCachesOfDependingTasks(resource.name, project)
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
