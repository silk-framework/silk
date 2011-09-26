package de.fuberlin.wiwiss.silk.workbench.util

import de.fuberlin.wiwiss.silk.impl.datasource.SparqlDataSource
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation
import de.fuberlin.wiwiss.silk.entity.{SparqlRestriction, Path}

@StrategyAnnotation(id = "LDEsparqlEndpoint", label = "LDE SPARQL Endpoint", description = "DataSource in the LDE context")
class LDEDataSource(endpointURI : String) extends SparqlDataSource(endpointURI){

  override def retrievePaths(restrictions : SparqlRestriction, depth : Int, limit : Option[Int]) : Traversable[(Path, Double)] =
  {
    LDEPathsCollector(createEndpoint(), restrictions, limit)
  }
}