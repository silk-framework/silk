package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.util.{Factory, Strategy}

trait BlockingFunction extends Strategy with (String => Double)

object BlockingFunction extends Factory[BlockingFunction]
