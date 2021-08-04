package controllers.util

import io.swagger.v3.oas.annotations.media.Schema

/**
  * Used for OpenAPI specifications.
  * This models the body of a multi part request to upload one file.
  */
case class FileMultiPartRequest(@Schema(`type` = "string", format = "binary", description = "Resource contents", required = false)
                                file: Option[String])
