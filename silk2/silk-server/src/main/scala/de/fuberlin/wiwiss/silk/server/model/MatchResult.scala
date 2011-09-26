package de.fuberlin.wiwiss.silk.server.model

import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.util.Uri

case class MatchResult(links : Traversable[Link], linkType : Uri, unmatchedEntities : Set[String])