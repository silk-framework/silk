package de.fuberlin.wiwiss.silk.datasource

import de.fuberlin.wiwiss.silk.linkspec.Param

class SparqlDataSource(params : Map[String, String]) extends DataSource
{
    require(params.contains("endpointURI"), "Parameter 'endpointURI' is required")

    override def toString = "SparqlDataSource(" + params + ")"
}