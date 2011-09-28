package de.fuberlin.wiwiss.silk.linkagerule.input

import de.fuberlin.wiwiss.silk.entity.{Entity, Path}
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.linkagerule.Operator
import xml.Node
import de.fuberlin.wiwiss.silk.util.{ValidationException, Identifier, DPair}

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
  override def apply(entities: DPair[Entity]): Set[String] = {
    if (entities.source.desc.variable == path.variable)
      entities.source.evaluate(path)
    else if (entities.target.desc.variable == path.variable)
      entities.target.evaluate(path)
    else
      Set.empty
  }

  override def toXML(implicit prefixes: Prefixes) = <Input id={id} path={path.serialize}/>
}

object PathInput {
  def fromXML(node: Node)(implicit prefixes: Prefixes) = {
    val id = Operator.readId(node)

    try {
      val pathStr = (node \ "@path").text
      val path = Path.parse(pathStr)
      PathInput(id, path)
    } catch {
      case ex: Exception => throw new ValidationException(ex.getMessage, id, "Path")
    }
  }
}
