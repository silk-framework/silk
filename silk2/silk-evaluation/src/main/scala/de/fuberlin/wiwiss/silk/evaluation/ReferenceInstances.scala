package de.fuberlin.wiwiss.silk.evaluation

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.instance.Instance

case class ReferenceInstances(positive : Traversable[SourceTargetPair[Instance]],
                              negative : Traversable[SourceTargetPair[Instance]])