package de.fuberlin.wiwiss.silk.workbench.project

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.instance.{InstanceSpecification, Instance}

//TODO use options?
//TODO store path frequencies
class Cache(var instanceSpecs : SourceTargetPair[InstanceSpecification] = null,
            var positiveInstances : Traversable[SourceTargetPair[Instance]] = null,
            var negativeInstances : Traversable[SourceTargetPair[Instance]] = null)
{
}
