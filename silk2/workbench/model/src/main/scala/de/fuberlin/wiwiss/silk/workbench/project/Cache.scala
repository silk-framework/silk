package de.fuberlin.wiwiss.silk.workbench.project

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.instance.{Path, Instance}

//TODO use options?
class Cache(var paths : SourceTargetPair[Traversable[(Path, Double)]] = null,
            var positiveInstances : Traversable[SourceTargetPair[Instance]] = null,
            var negativeInstances : Traversable[SourceTargetPair[Instance]] = null)
{
}
