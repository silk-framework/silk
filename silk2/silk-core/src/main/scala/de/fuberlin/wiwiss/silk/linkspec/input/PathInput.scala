package de.fuberlin.wiwiss.silk.linkspec.input

import de.fuberlin.wiwiss.silk.entity.{Entity, Path}
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.util.{Identifier, SourceTargetPair}
import de.fuberlin.wiwiss.silk.linkspec.Operator

/**
 * A PathInput retrieves values from a data item by a given RDF path and optionally applies a transformation to them.
 */
case class PathInput(id: Identifier = Operator.generateId, path: Path) extends Input {
  /**
   * Retrieves the values of this input for a given entity.
   *
   * @param entities The pair of entities.
   * @return The values.
   */
  override def apply(entities: SourceTargetPair[Entity]): Set[String] = {
    if (entities.source.desc.variable == path.variable)
      entities.source.evaluate(path)
    else if (entities.target.desc.variable == path.variable)
      entities.target.evaluate(path)
    else
      Set.empty
  }

  override def toXML(implicit prefixes: Prefixes) = <Input id={id} path={path.serialize}/>
}
