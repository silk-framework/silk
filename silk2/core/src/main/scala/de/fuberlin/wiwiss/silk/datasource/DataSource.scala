package de.fuberlin.wiwiss.silk.datasource

import de.fuberlin.wiwiss.silk.util.{Factory, Strategy}
import de.fuberlin.wiwiss.silk.instance.{InstanceSpecification, Instance}

trait DataSource extends Strategy
{
    def retrieve(instanceSpec : InstanceSpecification, prefixes : Map[String, String]) : Traversable[Instance]
}

object DataSource extends Factory[DataSource]
