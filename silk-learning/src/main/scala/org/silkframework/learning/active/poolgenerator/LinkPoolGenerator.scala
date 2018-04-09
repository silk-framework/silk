package org.silkframework.learning.active.poolgenerator

import org.silkframework.dataset.DataSource
import org.silkframework.entity.Path
import org.silkframework.learning.active.UnlabeledLinkPool
import org.silkframework.rule.LinkSpec
import org.silkframework.runtime.activity.Activity
import org.silkframework.util.DPair

trait LinkPoolGenerator {

  def generator(inputs: DPair[DataSource],
                linkSpec: LinkSpec,
                paths: Seq[DPair[Path]]): Activity[UnlabeledLinkPool]

}
