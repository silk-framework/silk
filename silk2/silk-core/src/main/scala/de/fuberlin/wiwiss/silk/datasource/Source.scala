package de.fuberlin.wiwiss.silk.datasource

import de.fuberlin.wiwiss.silk.instance.{InstanceSpecification, Instance}

/**
 * A source of instances.
 */
case class Source(id : String, dataSource : DataSource)
{
    /**
     * Retrieves instances from this source which satisfy a specific instance specification.
     *
     * @param instanceSpec The instance specification
     * @param instances The URIs of the instances to be retrieved. If empty, all instances will be retrieved.
     *
     * @return A Traversable over the instances. The evaluation of the Traversable may be non-strict.
     */
    def retrieve(instanceSpec : InstanceSpecification, instances : Seq[String] = Seq.empty) = dataSource.retrieve(instanceSpec, instances)
}
