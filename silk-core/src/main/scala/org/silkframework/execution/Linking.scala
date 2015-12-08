package org.silkframework.execution

import org.silkframework.entity.Link
import org.silkframework.util.DPair

/**
  * Set of links.
  */
case class Linking(links: Seq[Link] = Seq.empty, statistics: LinkingStatistics = LinkingStatistics())

case class LinkingStatistics(entityCount: DPair[Int] = DPair.fill(0))
