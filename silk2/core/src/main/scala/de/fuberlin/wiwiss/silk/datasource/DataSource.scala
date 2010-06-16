package de.fuberlin.wiwiss.silk.datasource

import de.fuberlin.wiwiss.silk.Instance
import de.fuberlin.wiwiss.silk.util.{Factory, Strategy}

trait DataSource extends Strategy
{
    def retrieve(instance : InstanceSpecification, prefixes : Map[String, String]) : Traversable[Instance]
}

object DataSource extends Factory[DataSource]
{
    register("sparqlEndpoint", classOf[SparqlDataSource])
}
