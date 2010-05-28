package de.fuberlin.wiwiss.silk.datasource

import de.fuberlin.wiwiss.silk.linkspec.Param

trait DataSource

object DataSource
{
    def apply(dataSourceType : String, params : Map[String, String]) : DataSource =
    {
        if (dataSourceType == "sparqlEndpoint") new SparqlDataSource(params)
        else throw new IllegalArgumentException("DataSource type unknown: " + dataSourceType)
    }
}