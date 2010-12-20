package de.fuberlin.wiwiss.silk.workbench.instancespec

import de.fuberlin.wiwiss.silk.instance.{ForwardOperator, Path, InstanceSpecification}
import util.Random
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.util.sparql.SparqlEndpoint
import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.workbench.Constants
import de.fuberlin.wiwiss.silk.util.{Uri, Task, SourceTargetPair}

class InstanceSpecBuilder(sourceEndpoint : SparqlEndpoint, targetEndpoint : SparqlEndpoint, alignment : Traversable[Link], prefixes : Map[String, String]) extends Task[SourceTargetPair[InstanceSpecification]]
{
    private val logger = Logger.getLogger(classOf[InstanceSpecBuilder].getName)

    override protected def execute() : SourceTargetPair[InstanceSpecification] =
    {
        val shuffledAlignment = Random.shuffle(alignment.toSeq)

        val referenceSourceInstances = shuffledAlignment.map(_.sourceUri)
        val referenceTargetInstances = shuffledAlignment.map(_.targetUri)

        val sourceInstanceSpec = createInstanceSpec(referenceSourceInstances, sourceEndpoint, Constants.SourceVariable)
        val targetInstanceSpec = createInstanceSpec(referenceTargetInstances, targetEndpoint, Constants.TargetVariable)

        SourceTargetPair(sourceInstanceSpec, targetInstanceSpec)
    }

    private def createInstanceSpec(referenceInstances : Traversable[String], endpoint : SparqlEndpoint, variable : String) : InstanceSpecification =
    {
        updateStatus("Searching for relevant properties in " + endpoint)
        //TODO consider paths longer than 1
        val relevantProperties = RelevantPropertiesCollector(referenceInstances, endpoint)

        updateStatus("Finding restrictions in " + endpoint)
        val restriction = RestrictionCollector(referenceInstances, relevantProperties, variable, endpoint)

        val relevantPaths = relevantProperties.map{case (prop, freq) => new Path(variable, ForwardOperator(Uri.fromURI(prop, prefixes)) :: Nil)}

        val instanceSpec = new InstanceSpecification(variable, restriction, relevantPaths, Map.empty)
        updateStatus("Instance specification created for " + endpoint)
        logger.info("Instance specification created for " + endpoint)

        instanceSpec
    }
}
