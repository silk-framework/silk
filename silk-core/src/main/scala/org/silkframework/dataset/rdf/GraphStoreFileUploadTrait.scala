package org.silkframework.dataset.rdf

import java.io.File

import org.silkframework.runtime.activity.UserContext

/**
  * Adds RDF file upload capabilities to a graph store client.
  */
trait GraphStoreFileUploadTrait {
  this: GraphStoreTrait =>
  /**
    * Uploads an RDF file to a graph via a multipart HTTP request to the GraphStore endpoint.
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
}
