package de.fuberlin.wiwiss.silk.server.model

import de.fuberlin.wiwiss.silk.output.Link

case class MatchResult(links : Traversable[Link], linkType : String, unmatchedInstances : Set[String])