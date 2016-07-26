package org.silkframework.learning.active.poolgenerator

import org.silkframework.config.LinkSpec
import org.silkframework.dataset.DataSource
import org.silkframework.entity.Path
import org.silkframework.learning.active.UnlabeledLinkPool
import org.silkframework.runtime.activity.Activity
import org.silkframework.util.DPair

trait LinkPoolGenerator {

  def generator(inputs: DPair[DataSource],
                linkSpec: LinkSpec,
                paths: DPair[Seq[Path]]): Activity[UnlabeledLinkPool]

}
