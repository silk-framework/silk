package de.fuberlin.wiwiss.silk.linkspec.input

import de.fuberlin.wiwiss.silk.instance.{Instance, Path}
import de.fuberlin.wiwiss.silk.util.SourceTargetPair

case class PathInput(path : Path) extends Input
{
  private var pathIndex = -1

  override def apply(instances : SourceTargetPair[Instance]) =
  {
    if(pathIndex == -1)
    {
        if(instances.source.spec.variable == path.variable)
        {
          pathIndex = instances.source.spec.pathIndex(path)
        }
        else if(instances.target.spec.variable == path.variable)
        {
          pathIndex = instances.target.spec.pathIndex(path)
        }
    }

    if(instances.source.spec.variable == path.variable)
    {
      instances.source.evaluate(pathIndex)
    }
    else if(instances.target.spec.variable == path.variable)
    {
      instances.target.evaluate(pathIndex)
    }
    else
    {
      Traversable.empty
    }
  }
}
