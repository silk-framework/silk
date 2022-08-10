package org.silkframework.learning.active.linkselector

import org.silkframework.entity.{Entity, LinkWithEntities}
import org.silkframework.util.DPair

object LinkSelectorHelper {
  def pairToLink(pair: DPair[Entity]): LinkWithEntities = new LinkWithEntities(pair.source.uri, pair.target.uri, linkEntities = pair)
}
