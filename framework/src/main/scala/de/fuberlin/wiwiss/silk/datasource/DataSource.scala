package de.fuberlin.wiwiss.silk.datasource

import de.fuberlin.wiwiss.silk.Instance
import de.fuberlin.wiwiss.silk.linkspec.Configuration

trait DataSource
{
    val params : Map[String, String]

    def retrieve(config : Configuration, instance : InstanceSpecification) : Traversable[Instance]
}

object DataSource
{
    def apply(dataSourceType : String, params : Map[String, String]) : DataSource =
    {
        if (dataSourceType == "sparqlEndpoint") new SparqlDataSource(params)
        else throw new IllegalArgumentException("DataSource type unknown: " + dataSourceType)
    }
}
