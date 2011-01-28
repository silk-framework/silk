package de.fuberlin.wiwiss.silk.workbench.project

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.instance.{InstanceSpecification, Instance}
import xml.{NodeBuffer, Node}

//TODO use options?
//TODO store path frequencies
class Cache(var instanceSpecs : SourceTargetPair[InstanceSpecification] = null,
            var positiveInstances : Traversable[SourceTargetPair[Instance]] = null,
            var negativeInstances : Traversable[SourceTargetPair[Instance]] = null)
{

  def toXML : Node =
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

    if(positiveInstances != null)
    {
      nodes.append(
          <PositiveInstances>{
          for(SourceTargetPair(sourceInstance, targetInstance) <- positiveInstances) yield
          {
            <Pair>
              <Source>{sourceInstance.toXML}</Source>
              <Target>{targetInstance.toXML}</Target>
            </Pair>
          }
          }</PositiveInstances>)
    }

    if(negativeInstances != null)
    {
      nodes.append(
        <NegativeInstances>{
          for(SourceTargetPair(sourceInstance, targetInstance) <- negativeInstances) yield
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

    val positiveInstances =
    {
      if(node \ "PositiveInstances" isEmpty)
      {
        null
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

    val negativeInstances =
    {
      if(node \ "NegativeInstances" isEmpty)
      {
        null
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

    new Cache(instanceSpecs, positiveInstances, negativeInstances)
  }
}
