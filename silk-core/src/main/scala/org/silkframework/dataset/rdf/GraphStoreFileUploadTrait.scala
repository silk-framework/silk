package org.silkframework.dataset.rdf

import java.io.File
import java.nio.file.{Files, StandardCopyOption}

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.Resource

/**
  * Adds RDF file upload capabilities to a graph store client.
  */
trait GraphStoreFileUploadTrait {
  this: GraphStoreTrait =>
  /**
    * Uploads an RDF file to a graph via a multipart/form-data HTTP request to the GraphStore endpoint.
    *
    * @param graph       the graph this data should be uploaded to
    * @param file        The file containing the RDF data
    * @param contentType The content type of the file, e.g. application/n-triples
    */
  def uploadFileToGraph(graph: String,
                        file: File,
                        contentType: String,
                        comment: Option[String])
                       (implicit userContext: UserContext): Unit

  /**
    * Uploads an RDF resource to a graph.
    * A resource that is not held in a local file, e.g. on S3, is copied to a temporary file for the upload.
    *
    * @param graph       the graph this data should be uploaded to
    * @param resource    The resource containing the RDF data
    * @param contentType The content type of the resource, e.g. application/n-triples
    */
  def uploadResourceToGraph(graph: String,
                            resource: Resource,
                            contentType: String,
                            comment: Option[String])
                           (implicit userContext: UserContext): Unit = {
    resource.underlyingFile match {
      case Some(file) =>
        uploadFileToGraph(graph, file, contentType, comment)
      case None =>
        val extension = resource.name.lastIndexOf('.') match {
          case -1 => ".tmp"
          case index => resource.name.substring(index)
        }
        val tempFile = Files.createTempFile("graphStoreUpload", extension)
        try {
          resource.read { is => Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING) }
          uploadFileToGraph(graph, tempFile.toFile, contentType, comment)
        } finally {
          Files.deleteIfExists(tempFile)
        }
    }
  }
}
