package de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking

import de.fuberlin.wiwiss.silk.instance.{InstanceSpecification, Instance}
import xml.{NodeBuffer, Node}
import de.fuberlin.wiwiss.silk.util.{Task, SourceTargetPair}
import de.fuberlin.wiwiss.silk.util.sparql.{InstanceRetriever, SparqlEndpoint}
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.evaluation.ReferenceInstances
import de.fuberlin.wiwiss.silk.output.Link

//TODO use options?
//TODO store path frequencies
class Cache(var instanceSpecs : SourceTargetPair[InstanceSpecification] = null,
            var instances : ReferenceInstances = ReferenceInstances.empty)
{
  def toXML(implicit prefixes : Prefixes) : Node =
  {
    val nodes = new NodeBuffer()

    if(instanceSpecs != null)
    {
      nodes.append(
        <InstanceSpecifications>
          <Source>
            { instanceSpecs.source.toXML }
          </Source>
          <Target>
            { instanceSpecs.target.toXML }
          </Target>
        </InstanceSpecifications>)
    }

    if(instances != null)
    {
      nodes.append(
          <PositiveInstances>{
          for(SourceTargetPair(sourceInstance, targetInstance) <- instances.positive.values) yield
          {
            <Pair>
              <Source>{sourceInstance.toXML}</Source>
              <Target>{targetInstance.toXML}</Target>
            </Pair>
          }
          }</PositiveInstances>)

      nodes.append(
        <NegativeInstances>{
          for(SourceTargetPair(sourceInstance, targetInstance) <- instances.negative.values) yield
          {
            <Pair>
              <Source>{sourceInstance.toXML}</Source>
              <Target>{targetInstance.toXML}</Target>
            </Pair>
          }
        }</NegativeInstances>)
    }

    <Cache>{nodes}</Cache>
  }
}

object Cache
{
  def fromXML(node : Node) : Cache =
  {
    val instanceSpecs =
    {
      if(node \ "InstanceSpecifications" isEmpty)
      {
        null
      }
      else
      {
        val sourceSpec = InstanceSpecification.fromXML(node \ "InstanceSpecifications" \ "Source" \ "_" head)
        val targetSpec = InstanceSpecification.fromXML(node \ "InstanceSpecifications" \ "Target" \ "_" head)
        new SourceTargetPair(sourceSpec, targetSpec)
      }
    }

    val positiveInstances : Traversable[SourceTargetPair[Instance]] =
    {
      if(node \ "PositiveInstances" isEmpty)
      {
        Traversable.empty
      }
      else
      {
        for(pairNode <- node \ "PositiveInstances" \ "Pair" toList) yield
        {
           SourceTargetPair(
             Instance.fromXML(pairNode \ "Source" \ "Instance" head, instanceSpecs.source),
             Instance.fromXML(pairNode \ "Target" \ "Instance" head, instanceSpecs.target))
        }
      }
    }

    val negativeInstances : Traversable[SourceTargetPair[Instance]] =
    {
      if(node \ "NegativeInstances" isEmpty)
      {
        Traversable.empty
      }
      else
      {
        for(pairNode <- node \ "NegativeInstances" \ "Pair" toList) yield
        {
           SourceTargetPair(
             Instance.fromXML(pairNode \ "Source" \ "Instance" head, instanceSpecs.source),
             Instance.fromXML(pairNode \ "Target" \ "Instance" head, instanceSpecs.target))
        }
      }
    }

    new Cache(instanceSpecs, ReferenceInstances.fromInstances(positiveInstances, negativeInstances))
  }
}
