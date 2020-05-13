package org.silkframework.learning.active.poolgenerator

import org.silkframework.dataset.DataSource
import org.silkframework.entity.paths.TypedPath
import org.silkframework.learning.active.UnlabeledLinkPool
import org.silkframework.learning.active.poolgenerator.LinkPoolGeneratorUtils._
import org.silkframework.rule.execution.GenerateLinks
import org.silkframework.rule.{LinkSpec, RuntimeLinkingConfig}
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.util.DPair

/**
  * Link Pool Generator that generates link candidates based on an existing linkage rule.
  */
class LinkSpecLinkPoolGenerator(maxLinks: Int = LinkSpecLinkPoolGenerator.defaultMaxLinks,
                                timeout: Int = LinkSpecLinkPoolGenerator.defaultTimeout) extends LinkPoolGenerator {

  override def generator(inputs: DPair[DataSource], linkSpec: LinkSpec, paths: Seq[DPair[TypedPath]], randomSeed: Long): Activity[UnlabeledLinkPool] = {
    new LinkPoolGeneratorActivity(inputs, linkSpec, paths)
  }

  private class LinkPoolGeneratorActivity(inputs: DPair[DataSource],
                                          linkSpec: LinkSpec,
                                          paths: Seq[DPair[TypedPath]]) extends Activity[UnlabeledLinkPool] {

    private val runtimeConfig = RuntimeLinkingConfig(generateLinksWithEntities = true, linkLimit = Some(maxLinks), executionTimeout = Some(timeout))

    override def run(context: ActivityContext[UnlabeledLinkPool])(implicit userContext: UserContext): Unit = {
      val entitySchemata = entitySchema(linkSpec, paths)
      val generateLinks = new GenerateLinks("PoolGenerator", "Pool Generator", inputs, linkSpec, Seq.empty, runtimeConfig) {
        override def entityDescs = entitySchemata
      }

      val links = context.child(generateLinks, 1.0).startBlockingAndGetValue().links

      context.value() = UnlabeledLinkPool(linkSpec.entityDescriptions, shuffleLinks(links))
    }
  }
}

object LinkSpecLinkPoolGenerator {

  val defaultMaxLinks = 1000

  val defaultTimeout = 60

}
