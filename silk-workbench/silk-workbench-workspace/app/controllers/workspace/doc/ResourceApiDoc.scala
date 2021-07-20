package controllers.workspace.doc

import io.swagger.v3.oas.annotations.media.Schema

object ResourceApiDoc {

  final val resourceListExample =
    """
      [{
         "name": "source.nt",
         "lastModified": "2020-01-09T12:17:12Z",
         "size": 3836164
       }, {
         "name": "target.nt",
         "lastModified": "2020-01-09T12:17:12Z",
         "size": 1288984
       }, {
         "name": "subsource.nt",
         "lastModified": "2020-01-09T12:17:12Z",
         "size": 3836164
       }]
    """

  final val resourceMetadataExample =
    """
      {
        "name": "source.nt",
        "relativePath": "source.nt",
        "absolutePath": "/var/dataintegration/workspace/movies/resources/source.nt",
        "size": 3836164,
        "modified":"2020-01-13T14:34:03Z"
      }
    """

  final val resourceUploadDescription =
"""Adds a file from the local file system to the project. There are three options to upload files:

1. Providing a local resource using the `file` form parameter.
2. Providing a remote resource using the `resource-url` form parameter. The provided resource will be downloaded and added to the project.
3. Providing the file as body payload. Supplying no body will create an empty resource.

The options are exclusive, i.e., only one option can be used per request."""

  /**
    * Models a multipart request for uploading resources.
    */
  case class ResourceMultiPartRequest(@Schema(`type` = "string", format = "binary", description = "Resource contents", required = false)
                                      file: Option[String],
                                      @Schema(description = "Resource URL", required = false)
                                      `resource-url`: Option[String])

}
