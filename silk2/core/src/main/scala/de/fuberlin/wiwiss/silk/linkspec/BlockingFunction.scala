package de.fuberlin.wiwiss.silk.linkspec

import blocking.{NumBlockingFunction, AlphaNumBlockingFunction}
import de.fuberlin.wiwiss.silk.util.{Factory, Strategy}

trait BlockingFunction extends Strategy with (String => Double)

object BlockingFunction extends Factory[BlockingFunction]
{
    register("numeric", classOf[NumBlockingFunction])
    register("alphanumeric", classOf[AlphaNumBlockingFunction])
}
