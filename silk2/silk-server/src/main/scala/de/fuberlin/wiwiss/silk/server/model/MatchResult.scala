package de.fuberlin.wiwiss.silk.server.model

import de.fuberlin.wiwiss.silk.util.Uri
import de.fuberlin.wiwiss.silk.entity.Link

case class MatchResult(links : Traversable[Link], linkType : Uri, unmatchedEntities : Set[String])