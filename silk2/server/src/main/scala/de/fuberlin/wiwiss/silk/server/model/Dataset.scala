package de.fuberlin.wiwiss.silk.server.model

import de.fuberlin.wiwiss.silk.config.Configuration
import de.fuberlin.wiwiss.silk.output.Output
import de.fuberlin.wiwiss.silk.{Matcher, Loader}
import de.fuberlin.wiwiss.silk.impl.writer.MemoryWriter
import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.instance.{InstanceSpecification, MemoryInstanceCache}
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification

/**
 * Holds the dataset of a link specification.
 */
class Dataset(val name : String, config : Configuration, linkSpec : LinkSpecification)
{
    private val sourceCache = new MemoryInstanceCache()
    private val targetCache = new MemoryInstanceCache()
    new Loader(config, linkSpec).writeCaches(sourceCache, targetCache)

    private val (sourceInstanceSpec, targetInstanceSpec) = InstanceSpecification.retrieve(linkSpec)

    /**
     * Matches a set of instances with all instances in this dataset.
     */
    def apply(instanceSource : DataSource) : MatchResult =
    {
        val instanceCache = new MemoryInstanceCache()
        val writer = new MemoryWriter()
        val matcher = new Matcher(config.copy(outputs = new Output(writer) :: Nil), linkSpec)

        instanceCache.write(instanceSource.retrieve(sourceInstanceSpec, config.prefixes))
        if(instanceCache.instanceCount > 0)
        {
            matcher.execute(instanceCache, targetCache)
        }

        instanceCache.clear()
        instanceCache.write(instanceSource.retrieve(targetInstanceSpec, config.prefixes))
        if(instanceCache.instanceCount > 0)
        {
            matcher.execute(sourceCache, instanceCache)
        }

        MatchResult(writer.links, linkSpec.linkType)
    }

    /**
     * Adds new instances to this dataset
     */
    def addInstances(instanceSource : DataSource)
    {
        val loader = new Loader(config, linkSpec)
        loader.writeSourceCache(sourceCache, instanceSource)
        loader.writeTargetCache(targetCache, instanceSource)
    }

    def sourceInstanceCount = sourceCache.instanceCount

    def targetInstanceCount = targetCache.instanceCount
}