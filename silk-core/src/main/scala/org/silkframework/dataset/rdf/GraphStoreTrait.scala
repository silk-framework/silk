package org.silkframework.dataset.rdf

import java.io.OutputStream
import java.net.{HttpURLConnection, URL}

/**
 * Created by andreas on 1/28/16.
 */
trait GraphStoreTrait {
  def graphStoreEndpoint(graph: String): String

  /**
   * Allows to write triples directly into a graph. The [[OutputStream]] must be closed by the caller.
   * @param graph
   * @param contentType
   * @return
   */
  def postDataToGraph(graph: String, contentType: String = "application/n-triples"): OutputStream = {
    val updateUrl = graphStoreEndpoint(graph)
    val url = new URL(updateUrl)
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod( "POST" )
    connection.setDoInput( true )
    connection.setDoOutput( true )
    connection.setUseCaches( false )
    connection.setRequestProperty( "Content-Type", contentType )
    connection.getOutputStream()
  }
}
