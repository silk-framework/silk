package de.fuberlin.wiwiss.silk.workbench.util

import de.fuberlin.wiwiss.silk.impl.datasource.SparqlDataSource
import de.fuberlin.wiwiss.silk.instance.Path
import de.fuberlin.wiwiss.silk.linkspec.Restrictions
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "LDEsparqlEndpoint", label = "LDE SPARQL Endpoint", description = "DataSource in the LDE context")
class LDEDataSource(endpointURI : String, login : String = null, password : String = null,
                       graph : String = null, pageSize : Int = 1000, instanceList : String = null,
                       pauseTime : Int = 0, retryCount : Int = 3, retryPause : Int = 1000) extends SparqlDataSource(endpointURI, login, password, graph, pageSize, instanceList, pauseTime, retryCount, retryPause){

  override def retrievePaths(restrictions : Restrictions, depth : Int, limit : Option[Int]) : Traversable[(Path, Double)] =
  {
    LDEPathsCollector(createEndpoint(), restrictions, limit)
  }
}