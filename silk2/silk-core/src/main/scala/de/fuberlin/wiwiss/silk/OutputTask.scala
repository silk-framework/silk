package de.fuberlin.wiwiss.silk

import collection.mutable.Buffer
import output.{Output, Link}
import util.{Uri, Task}

/**
* Writes the links to the output.
*/
class OutputTask(links : Buffer[Link], linkType : Uri, outputs : Traversable[Output]) extends Task[Unit]
{
  taskName = "Writing output"

  override def execute()
  {
    outputs.foreach(_.open)

    for(link <- links;
        output <- outputs)
    {
      output.write(link, linkType.toString)
    }

    outputs.foreach(_.close)
  }
}