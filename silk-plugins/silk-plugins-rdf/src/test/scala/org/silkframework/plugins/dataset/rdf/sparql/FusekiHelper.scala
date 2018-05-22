package org.silkframework.plugins.dataset.rdf.sparql

import org.apache.jena.fuseki.FusekiException
import org.apache.jena.fuseki.embedded.FusekiServer
import org.apache.jena.fuseki.server.{DataService, Operation}
import org.apache.jena.query.Dataset

/**
  * Some Fuseki related helper functions.
  */
object FusekiHelper {
  /**
    * Starts a local Fuseki server based on the given dataset.
    * @param dataset    The dataset that should be accessed via the Fuseki server.
    * @param startPort  The port to start looking for open ports.
    * @return
    */
  def startFusekiServer(dataset: Dataset, startPort: Int): FusekiServerInfo = {
    var fusekiServer: Option[FusekiServer] = None
    var fusekiServerPort = startPort

    val ds = dataset.asDatasetGraph()
    val dataService = new DataService(ds)
    // All services on the same endpoint instead different ones for query and update
    dataService.addEndpoint(Operation.Quads_RW, "")
    dataService.addEndpoint(Operation.Query, "")
    dataService.addEndpoint(Operation.Update, "")
    while (fusekiServer.isEmpty) {
      try {
        val server = FusekiServer.create.add("/ds", ds)
            .setPort(fusekiServerPort)
            .add("/data", dataService)
            .build
        server.start()
        fusekiServer = Some(server)
      } catch {
        case _: FusekiException =>
          fusekiServerPort += 1
      }
    }
    FusekiServerInfo(s"http://localhost:$fusekiServerPort/ds", fusekiServerPort, fusekiServer.get)
  }
}

case class FusekiServerInfo(url: String, port: Int, server: FusekiServer)