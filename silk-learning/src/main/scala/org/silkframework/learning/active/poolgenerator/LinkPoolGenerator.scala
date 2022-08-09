package org.silkframework.learning.active.poolgenerator

import org.silkframework.config.Prefixes
import org.silkframework.dataset.DataSource
import org.silkframework.learning.active.comparisons.ComparisonPair
import org.silkframework.rule.LinkSpec
import org.silkframework.runtime.activity.Activity
import org.silkframework.util.DPair

trait LinkPoolGenerator {

  def generator(inputs: DPair[DataSource],
                linkSpec: LinkSpec,
                paths: Seq[ComparisonPair],
                randomSeed: Long)
               (implicit prefixes: Prefixes): Activity[UnlabeledLinkPool]

}
