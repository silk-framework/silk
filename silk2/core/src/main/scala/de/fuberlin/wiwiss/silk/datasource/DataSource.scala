package de.fuberlin.wiwiss.silk.datasource

import de.fuberlin.wiwiss.silk.util.{Factory, Strategy}
import de.fuberlin.wiwiss.silk.instance.{InstanceSpecification, Instance}

trait DataSource extends Strategy
{
    def retrieve(instanceSpec : InstanceSpecification, instances : Seq[String] = Seq.empty) : Traversable[Instance]
}

object DataSource extends Factory[DataSource]
