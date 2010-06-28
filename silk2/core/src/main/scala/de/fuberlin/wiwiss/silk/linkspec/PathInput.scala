package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.Instance
import path.Path

case class PathInput(path : Path) extends Input
{
    override def evaluate(instances : Traversable[Instance]) =
    {
        instances.find(_.variable == path.variable) match
        {
            case Some(instance) => instance.evaluate(path)
            case None => Traversable.empty
        }
    }
}
