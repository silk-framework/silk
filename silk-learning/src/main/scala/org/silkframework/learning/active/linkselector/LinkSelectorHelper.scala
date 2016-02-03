package org.silkframework.learning.active.linkselector

import org.silkframework.entity.{Link, Entity}
import org.silkframework.util.DPair

/**
 * Created by andreas on 2/3/16.
 */
object LinkSelectorHelper {
  def pairToLink(pair: DPair[Entity]) = new Link(pair.source.uri, pair.target.uri, None, entities = Some(pair))
}
