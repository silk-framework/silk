package de.fuberlin.wiwiss.silk.datasource

import de.fuberlin.wiwiss.silk.util.strategy.{Factory, Strategy}
import de.fuberlin.wiwiss.silk.instance.{InstanceSpecification, Instance}

/**
 * The base trait of a concrete source of instances.
 */
trait DataSource extends Strategy
{
    /**
     * Retrieves instances from this source which satisfy a specific instance specification.
     *
     * @param instanceSpec The instance specification
     * @param instances The URIs of the instances to be retrieved. If empty, all instances will be retrieved.
     *
     * @return A Traversable over the instances. The evaluation of the Traversable may be non-strict.
     */
    def retrieve(instanceSpec : InstanceSpecification, instances : Seq[String] = Seq.empty) : Traversable[Instance]
}

object DataSource extends Factory[DataSource]
