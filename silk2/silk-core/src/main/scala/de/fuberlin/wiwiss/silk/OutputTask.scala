package de.fuberlin.wiwiss.silk

import collection.mutable.Buffer
import linkspec.LinkSpecification
import output.Link
import config.Configuration
import de.fuberlin.wiwiss.silk.util.Task

/**
* Writes the links to the output.
*/
class OutputTask(config : Configuration, linkSpec : LinkSpecification, links : Buffer[Link]) extends Task[Unit]
{
  override def execute()
  {
    val outputs = config.outputs ++ linkSpec.outputs

    outputs.foreach(_.open)

    for(link <- links;
        output <- outputs)
    {
      output.write(link, linkSpec.linkType)
    }

    outputs.foreach(_.close)
  }
}