package de.fuberlin.wiwiss.silk.linkspec.input

import de.fuberlin.wiwiss.silk.instance.{Instance, Path}
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.util.{Identifier, SourceTargetPair}
import de.fuberlin.wiwiss.silk.linkspec.Operator

/**
 * A PathInput retrieves values from a data item by a given RDF path and optionally applies a transformation to them.
 */
case class PathInput(id: Identifier = Operator.generateId, path: Path) extends Input {
  /**
   * Retrieves the values of this input for a given instance.
   *
   * @param instances The pair of instances.
   * @return The values.
   */
  override def apply(instances: SourceTargetPair[Instance]) = {
    if (instances.source.spec.variable == path.variable) {
      instances.source.evaluate(path)
    }
    else if (instances.target.spec.variable == path.variable) {
      instances.target.evaluate(path)
    }
    else {
      Traversable.empty
    }
  }

  override def toXML(implicit prefixes: Prefixes) = <Input path={path.serialize}/>
}
